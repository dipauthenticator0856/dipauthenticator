/*
 * Copyright (C) 2017-2018 Jakob Nixdorf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.shadowice.flocke.andotp.View;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.shadowice.flocke.andotp.Common.Network.Rest.RestCallback;
import org.shadowice.flocke.andotp.Database.DipEntry;
import org.shadowice.flocke.andotp.R;
import org.shadowice.flocke.andotp.Services.API.Managers.DeleteDigitManager;
import org.shadowice.flocke.andotp.Services.API.Managers.DipDigitManager;
import org.shadowice.flocke.andotp.Utilities.Constants;
import org.shadowice.flocke.andotp.Utilities.DipDatabaseHelper;
import org.shadowice.flocke.andotp.Utilities.EntryThumbnail;
import org.shadowice.flocke.andotp.Utilities.SaveSharedPref;
import org.shadowice.flocke.andotp.Utilities.Settings;
import org.shadowice.flocke.andotp.Utilities.Tools;
import org.shadowice.flocke.andotp.View.ItemTouchHelper.ItemTouchHelperAdapter;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import javax.crypto.SecretKey;

import okhttp3.ResponseBody;

public class DipEntriesCardAdapter extends RecyclerView.Adapter<DipEntryViewHolder>
        implements ItemTouchHelperAdapter, Filterable {
    private Context context;
    private Handler taskHandler;
    private EntryFilter filter;
    private ArrayList<DipEntry> entries;

    private ArrayList<DipEntry> displayedEntries;
    private Callback callback;
    private List<String> tagsFilter = new ArrayList<>();

    private SecretKey encryptionKey = null;

    private Constants.SortMode sortMode = Constants.SortMode.UNSORTED;
    private TagsAdapter tagsFilterAdapter;
    private Settings settings;

    private SharedPreferences sp;

    public DipEntriesCardAdapter(Context context, TagsAdapter tagsFilterAdapter) {
        this.context = context;
        this.tagsFilterAdapter = tagsFilterAdapter;
        this.settings = new Settings(context);
        this.taskHandler = new Handler();
        this.entries = new ArrayList<>();
    }

    public SecretKey getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(SecretKey key) {
        encryptionKey = key;
    }

    @Override
    public int getItemCount() {
        return displayedEntries.size();
    }

    public ArrayList<DipEntry> getEntries() {
        return entries;
    }

    public void addEntry(DipEntry e) {
        if (!entries.contains(e)) {
            entries.add(e);
            entriesChanged();
        } else {
            Toast.makeText(context, R.string.toast_entry_exists, Toast.LENGTH_LONG).show();
        }
    }

    private int getRealIndex(int displayPosition) {
        return entries.indexOf(displayedEntries.get(displayPosition));
    }

    private void entriesChanged() {
        displayedEntries = sortEntries(entries);
        filterByTags(tagsFilter);
        notifyDataSetChanged();
    }

    public String getDipSeedkey() {
        if (!entries.isEmpty())
            return entries.get(0).getDipseedkey();
        else
            return null;
    }

    public void saveEntries() {
        DipDatabaseHelper.saveDatabase(context, entries, encryptionKey);
    }

    public void loadEntries() {
        if (encryptionKey != null) {
            entries = DipDatabaseHelper.loadDatabase(context, encryptionKey);
            entriesChanged();
        }
    }

    public void filterByTags(List<String> tags) {
        tagsFilter = tags;
        List<DipEntry> matchingEntries = new ArrayList<>();

        for (DipEntry e : entries) {
            //Entries with no tags will always be shown
            Boolean foundMatchingTag = e.getTags().isEmpty() && settings.getNoTagsToggle();

            if (settings.getTagFunctionality() == Constants.TagFunctionality.AND) {
                if (e.getTags().containsAll(tags)) {
                    foundMatchingTag = true;
                }
            } else {
                for (String tag : tags) {
                    if (e.getTags().contains(tag)) {
                        foundMatchingTag = true;
                    }
                }
            }

            if (foundMatchingTag) {
                matchingEntries.add(e);
            }
        }

        displayedEntries = sortEntries(matchingEntries);
        notifyDataSetChanged();
    }

    public void updateTimeBasedTokens() {
        boolean change = false;

        for (DipEntry e : entries) {
            if (e.isTimeBased()) {
                boolean item_changed = e.updateOTP();
                change = change || item_changed || e.hasNonDefaultPeriod();

            }

        }

        if (change) {
            sendDipDigit();
            notifyDataSetChanged();
        }

    }

    @Override
    public void onBindViewHolder(@NonNull DipEntryViewHolder entryViewHolder, int i) {
        DipEntry entry = displayedEntries.get(i);

        if (!entry.isTimeBased())
            entry.updateOTP();
        entryViewHolder.updateValues(entry);

        entryViewHolder.setLabelSize(settings.getLabelSize());
        entryViewHolder.setLabelScroll(settings.getScrollLabel());

        if (settings.getThumbnailVisible())
            entryViewHolder.setThumbnailSize(settings.getThumbnailSize());
    }

    @Override
    @NonNull
    public DipEntryViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.component_card, viewGroup, false);
        DipEntryViewHolder viewHolder = new DipEntryViewHolder(context, itemView, settings.getTapToReveal());

        sp = context.getSharedPreferences(SaveSharedPref.PREF, Context.MODE_PRIVATE);

        viewHolder.setCallback(new DipEntryViewHolder.Callback() {
            @Override
            public void onMoveEventStart() {
                if (callback != null)
                    callback.onMoveEventStart();

            }

            @Override
            public void onMoveEventStop() {
                if (callback != null)
                    callback.onMoveEventStop();
            }

            @Override
            public void onMenuButtonClicked(View parentView, int position) {
                showPopupMenu(parentView, position);
            }

            @Override
            public void onCopyButtonClicked(String text, int position) {
                copyToClipboard(text);
                updateLastUsed(position, getRealIndex(position));
            }

            @Override
            public void onCardClicked(final int position) {
                if (settings.getTapToReveal()) {
                    final DipEntry entry = displayedEntries.get(position);
                    final int realIndex = entries.indexOf(entry);

                    if (entry.isVisible()) {
                        hideEntry(entry);
                    } else {
                        entries.get(realIndex).setHideTask(new Runnable() {
                            @Override
                            public void run() {
                                hideEntry(entry);
                            }
                        });
                        taskHandler.postDelayed(entries.get(realIndex).getHideTask(), settings.getTapToRevealTimeout() * 1000);

                        entry.setVisible(true);
                        notifyItemChanged(position);
                    }
                }
            }

            @Override
            public void onCounterClicked(int position) {
                DipEntry entry = displayedEntries.get(position);
                DipEntry realEntry = entries.get(getRealIndex(position));

                long counter = entry.getCounter() + 1;

                entry.setCounter(counter);
                entry.updateOTP();
                notifyItemChanged(position);

                realEntry.setCounter(counter);

                Log.e("countr ", "is " + counter);

                realEntry.updateOTP();
                DipDatabaseHelper.saveDatabase(context, entries, encryptionKey);
            }

            @Override
            public void onCounterLongPressed(int position) {
                setCounter(position);
            }
        });

        return viewHolder;
    }

    private void hideEntry(DipEntry entry) {
        int pos = displayedEntries.indexOf(entry);
        int realIndex = entries.indexOf(entry);

        if (realIndex >= 0) {
            entries.get(realIndex).setVisible(false);
            taskHandler.removeCallbacks(entries.get(realIndex).getHideTask());
            entries.get(realIndex).setHideTask(null);
        }

        boolean updateNeeded = updateLastUsed(pos, realIndex);

        if (pos >= 0) {
            displayedEntries.get(pos).setVisible(false);

            if (updateNeeded)
                notifyItemChanged(pos);
        }
    }

    private void setCounter(final int pos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        int marginSmall = context.getResources().getDimensionPixelSize(R.dimen.activity_margin_small);
        int marginMedium = context.getResources().getDimensionPixelSize(R.dimen.activity_margin_medium);

        final EditText input = new EditText(context);
        input.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        input.setText(String.format(Locale.ENGLISH, "%d", displayedEntries.get(pos).getCounter()));
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine();

        FrameLayout container = new FrameLayout(context);
        container.setPaddingRelative(marginMedium, marginSmall, marginMedium, 0);
        container.addView(input);

        builder.setTitle(R.string.dialog_title_counter)
                .setView(container)
                .setPositiveButton(R.string.button_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int realIndex = getRealIndex(pos);
                        long newCounter = Long.parseLong(input.getEditableText().toString());

                        displayedEntries.get(pos).setCounter(newCounter);
                        notifyItemChanged(pos);

                        DipEntry e = entries.get(realIndex);
                        e.setCounter(newCounter);

                        DipDatabaseHelper.saveDatabase(context, entries, encryptionKey);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .create()
                .show();
    }

    private boolean updateLastUsed(int position, int realIndex) {
        long timeStamp = System.currentTimeMillis();

        if (position >= 0)
            displayedEntries.get(position).setLastUsed(timeStamp);

        entries.get(realIndex).setLastUsed(timeStamp);
        DipDatabaseHelper.saveDatabase(context, entries, encryptionKey);

        if (sortMode == Constants.SortMode.LAST_USED) {
            displayedEntries = sortEntries(displayedEntries);
            notifyDataSetChanged();
            return false;
        }

        return true;
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (sortMode == Constants.SortMode.UNSORTED && displayedEntries.equals(entries)) {
            Collections.swap(entries, fromPosition, toPosition);

            displayedEntries = new ArrayList<>(entries);
            notifyItemMoved(fromPosition, toPosition);

            DipDatabaseHelper.saveDatabase(context, entries, encryptionKey);
        }

        return true;
    }

    public void editEntryLabel(final int pos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        int marginSmall = context.getResources().getDimensionPixelSize(R.dimen.activity_margin_small);
        int marginMedium = context.getResources().getDimensionPixelSize(R.dimen.activity_margin_medium);

        final EditText input = new EditText(context);
        input.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        input.setText(displayedEntries.get(pos).getLabel());
        input.setSingleLine();

        FrameLayout container = new FrameLayout(context);
        container.setPaddingRelative(marginMedium, marginSmall, marginMedium, 0);
        container.addView(input);

        builder.setTitle(R.string.dialog_title_rename)
                .setView(container)
                .setPositiveButton(R.string.button_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int realIndex = getRealIndex(pos);
                        String newLabel = input.getEditableText().toString();

                        displayedEntries.get(pos).setLabel(newLabel);
                        if (sortMode == Constants.SortMode.LABEL) {
                            displayedEntries = sortEntries(displayedEntries);
                            notifyDataSetChanged();
                        } else {
                            notifyItemChanged(pos);
                        }

                        DipEntry e = entries.get(realIndex);
                        e.setLabel(newLabel);

                        DipDatabaseHelper.saveDatabase(context, entries, encryptionKey);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .create()
                .show();
    }

    public void changeThumbnail(final int pos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        int marginSmall = context.getResources().getDimensionPixelSize(R.dimen.activity_margin_small);
        int marginMedium = context.getResources().getDimensionPixelSize(R.dimen.activity_margin_medium);

        int realIndex = getRealIndex(pos);
        final ThumbnailSelectionAdapter thumbnailAdapter = new ThumbnailSelectionAdapter(context, entries.get(realIndex).getLabel());

        final EditText input = new EditText(context);
        input.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        input.setSingleLine();

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                thumbnailAdapter.filter(editable.toString());
            }
        });

        int gridPadding = context.getResources().getDimensionPixelSize(R.dimen.activity_margin_small);
        int gridBackground = Tools.getThemeColor(context, R.attr.thumbnailBackground);

        GridView grid = new GridView(context);
        grid.setAdapter(thumbnailAdapter);
        grid.setBackgroundColor(gridBackground);
        grid.setPadding(gridPadding, gridPadding, gridPadding, gridPadding);
        grid.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int thumbnailSize = settings.getThumbnailSize();
        grid.setColumnWidth(thumbnailSize);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setVerticalSpacing(context.getResources().getDimensionPixelSize(R.dimen.activity_margin_medium));
        grid.setHorizontalSpacing(context.getResources().getDimensionPixelSize(R.dimen.activity_margin_medium));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.addView(input);
        layout.addView(grid);

        FrameLayout container = new FrameLayout(context);
        container.setPaddingRelative(marginMedium, marginSmall, marginMedium, 0);
        container.addView(layout);

        final AlertDialog alert = builder.setTitle(R.string.menu_popup_change_image)
                .setView(container)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .create();

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int realIndex = getRealIndex(pos);
                EntryThumbnail.EntryThumbnails thumbnail = EntryThumbnail.EntryThumbnails.Default;
                try {
                    int realPos = thumbnailAdapter.getRealIndex(position);
                    thumbnail = EntryThumbnail.EntryThumbnails.values()[realPos];
                } catch (Exception e) {
                    e.printStackTrace();
                }

                DipEntry e = entries.get(realIndex);
                e.setThumbnail(thumbnail);

                DipDatabaseHelper.saveDatabase(context, entries, encryptionKey);
                notifyItemChanged(pos);
                alert.cancel();
            }
        });

        alert.show();
    }

    public void editEntryTags(final int pos) {
        final int realPos = getRealIndex(pos);
        final DipEntry entry = entries.get(realPos);

        HashMap<String, Boolean> tagsHashMap = new HashMap<>();
        for (String tag : entry.getTags()) {
            tagsHashMap.put(tag, true);
        }
        for (String tag : getTags()) {
            if (!tagsHashMap.containsKey(tag))
                tagsHashMap.put(tag, false);
        }
        final TagsAdapter tagsAdapter = new TagsAdapter(context, tagsHashMap);

        final Callable tagsCallable = new Callable() {
            @Override
            public Object call() throws Exception {
                entries.get(realPos).setTags(tagsAdapter.getActiveTags());
                DipDatabaseHelper.saveDatabase(context, entries, encryptionKey);

                List<String> inUseTags = getTags();

                HashMap<String, Boolean> tagsHashMap = new HashMap<>();
                for (String tag : tagsFilterAdapter.getTags()) {
                    if (inUseTags.contains(tag))
                        tagsHashMap.put(tag, false);
                }
                for (String tag : tagsFilterAdapter.getActiveTags()) {
                    if (inUseTags.contains(tag))
                        tagsHashMap.put(tag, true);
                }
                for (String tag : getTags()) {
                    if (inUseTags.contains(tag))
                        if (!tagsHashMap.containsKey(tag))
                            tagsHashMap.put(tag, true);
                }

                tagsFilterAdapter.setTags(tagsHashMap);
                filterByTags(tagsFilterAdapter.getActiveTags());
                return null;
            }
        };

        TagsDialog.show(context, tagsAdapter, tagsCallable, tagsCallable);
    }

    public void removeItem(final int pos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        String label = displayedEntries.get(pos).getLabel();
        String message = context.getString(R.string.dialog_msg_confirm_delete, label);
        final String desecret = displayedEntries.get(pos).getSecretKeyString();

        builder.setTitle(R.string.dialog_title_remove)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int realIndex = getRealIndex(pos);

                        displayedEntries.remove(pos);
                        notifyItemRemoved(pos);

                        entries.remove(realIndex);
                        DipDatabaseHelper.saveDatabase(context, entries, encryptionKey);

                        SharedPreferences sp = context.getSharedPreferences(SaveSharedPref.PREF, Context.MODE_PRIVATE);

                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("desecret", desecret);
                        jsonObject.addProperty("seedkey", sp.getString(SaveSharedPref.dipseedkey, ""));

                        deleteDevice(jsonObject);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .show();
    }

    private void deleteDevice(JsonObject jsonObject) {

        DeleteDigitManager.Instance().deleteDipDigit(jsonObject, new RestCallback.CommonInfoDelegate<ResponseBody>() {
            @Override
            public void CallDidSuccess(ResponseBody info) {
                try {
                    Log.e("deleteentry", " CallDidSuccess " + info.string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void CallFailedWithError(String error) {
            }
        });

    }

    private void showPopupMenu(View view, final int pos) {
        View menuItemView = view.findViewById(R.id.menuButton);
        PopupMenu popup = new PopupMenu(view.getContext(), menuItemView);
        MenuInflater inflate = popup.getMenuInflater();
        inflate.inflate(R.menu.menu_popup, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.menu_popup_editLabel) {
                    editEntryLabel(pos);
                    return true;
                } else if (id == R.id.menu_popup_changeImage) {
                    changeThumbnail(pos);
                    return true;
                } else if (id == R.id.menu_popup_editTags) {
                    editEntryTags(pos);
                    return true;
                } else if (id == R.id.menu_popup_remove) {
                    removeItem(pos);
                    return true;
                } else {
                    return false;
                }
            }
        });
        popup.show();
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(context.getString(R.string.label_clipboard_content), text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(context, R.string.toast_copied_to_clipboard, Toast.LENGTH_LONG).show();
    }

    public Constants.SortMode getSortMode() {
        return this.sortMode;
    }

    public void setSortMode(Constants.SortMode mode) {
        this.sortMode = mode;
        entriesChanged();
    }

    private ArrayList<DipEntry> sortEntries(List<DipEntry> unsorted) {
        ArrayList<DipEntry> sorted = new ArrayList<>(unsorted);

        if (sortMode == Constants.SortMode.LABEL) {
            Collections.sort(sorted, new LabelComparator());
        } else if (sortMode == Constants.SortMode.LAST_USED) {
            Collections.sort(sorted, new LastUsedComparator());
        }

        return sorted;
    }

    public void setCallback(Callback cb) {
        this.callback = cb;
    }

    public EntryFilter getFilter() {
        if (filter == null)
            filter = new EntryFilter();

        return filter;
    }

    public List<String> getTags() {
        HashSet<String> tags = new HashSet<String>();

        for (DipEntry entry : entries) {
            tags.addAll(entry.getTags());
        }

        return new ArrayList<String>(tags);
    }

    public void sendDipDigit() {

        SharedPreferences sp = context.getSharedPreferences(SaveSharedPref.PREF, Context.MODE_PRIVATE);

        JsonArray jsonArray1 = new JsonArray();
        for (DipEntry e : entries) {

            if (e.getCurrentOTP() != null) {
                JsonObject jsonobjects = new JsonObject();
                jsonobjects.addProperty("desecret", e.getSecretKeyString());
                jsonobjects.addProperty("digits", e.getCurrentOTP());
                jsonobjects.addProperty("seedkey", sp.getString(SaveSharedPref.dipseedkey, ""));
                jsonArray1.add(jsonobjects);

            }

        }

        if (jsonArray1.size() > 0) {
            sendDipDigit(jsonArray1);
        }

    }

    private void sendDipDigit(JsonArray jsonArray) {

        DipDigitManager.Instance().sendDipDigit(jsonArray, new RestCallback.CommonInfoDelegate<ResponseBody>() {
            @Override
            public void CallDidSuccess(ResponseBody responseBody) {
            }

            @Override
            public void CallFailedWithError(String error) {
            }
        });

    }

    public interface Callback {
        void onMoveEventStart();

        void onMoveEventStop();
    }

    public class EntryFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            final FilterResults filterResults = new FilterResults();

            ArrayList<DipEntry> filtered = new ArrayList<>();
            if (constraint != null && constraint.length() != 0) {
                for (int i = 0; i < entries.size(); i++) {
                    if (entries.get(i).getLabel().toLowerCase().contains(constraint.toString().toLowerCase())) {
                        filtered.add(entries.get(i));
                    }
                }
            } else {
                filtered = entries;
            }

            filterResults.count = filtered.size();
            filterResults.values = filtered;

            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            displayedEntries = sortEntries((ArrayList<DipEntry>) results.values);
            notifyDataSetChanged();
        }
    }

    public class LabelComparator implements Comparator<DipEntry> {
        Collator collator;

        LabelComparator() {
            collator = Collator.getInstance();
            collator.setStrength(Collator.PRIMARY);
        }

        @Override
        public int compare(DipEntry o1, DipEntry o2) {
            return collator.compare(o1.getLabel(), o2.getLabel());
        }
    }

    public class LastUsedComparator implements Comparator<DipEntry> {
        @Override
        public int compare(DipEntry o1, DipEntry o2) {
            return Long.compare(o2.getLastUsed(), o1.getLastUsed());
        }
    }
}
