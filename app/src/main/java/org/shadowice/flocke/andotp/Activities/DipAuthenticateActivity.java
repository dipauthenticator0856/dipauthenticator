package org.shadowice.flocke.andotp.Activities;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import org.shadowice.flocke.andotp.Common.Network.Rest.RestCallback;
import org.shadowice.flocke.andotp.Database.DipEntry;
import org.shadowice.flocke.andotp.R;
import org.shadowice.flocke.andotp.Services.API.Managers.DeviceManager;
import org.shadowice.flocke.andotp.Services.API.Models.DipDeviceModel;
import org.shadowice.flocke.andotp.Utilities.Constants;
import org.shadowice.flocke.andotp.Utilities.EncryptionHelper;
import org.shadowice.flocke.andotp.Utilities.KeyStoreHelper;
import org.shadowice.flocke.andotp.Utilities.NotificationHelper;
import org.shadowice.flocke.andotp.Utilities.SaveSharedPref;
import org.shadowice.flocke.andotp.Utilities.TokenCalculator;
import org.shadowice.flocke.andotp.View.DipEntriesCardAdapter;
import org.shadowice.flocke.andotp.View.DipManualEntryDialog;
import org.shadowice.flocke.andotp.View.ItemTouchHelper.SimpleItemTouchHelperCallback;
import org.shadowice.flocke.andotp.View.TagsAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.crypto.SecretKey;

import static org.shadowice.flocke.andotp.Utilities.Constants.AuthMethod;
import static org.shadowice.flocke.andotp.Utilities.Constants.EncryptionType;
import static org.shadowice.flocke.andotp.Utilities.Constants.SortMode;

public class DipAuthenticateActivity extends DipBaseActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String INTENT_SCAN_QR = "org.shadowice.flocke.andotp.intent.SCAN_QR";
    private static final String INTENT_ENTER_DETAILS = "org.shadowice.flocke.andotp.intent.ENTER_DETAILS";
    public static long animatorDuration = 1000;
    private DipEntriesCardAdapter adapter;
    private SpeedDialView speedDial;
    private MenuItem sortMenu;
    private SimpleItemTouchHelperCallback touchHelperCallback;
    private EncryptionType encryptionType = EncryptionType.KEYSTORE;
    private boolean requireAuthentication = false;
    private Handler handler;
    private Runnable handlerTask;
    private ListView tagsDrawerListView;
    private TagsAdapter tagsDrawerAdapter;
    private ActionBarDrawerToggle tagsToggle;
    private SharedPreferences sp;
    private  TextView txtSeedKey;

    private static String getRandomNumberString() {
        // It will generate 6 digit random Number.
        // from 0 to 999999
        Random rnd = new Random();
        int number = rnd.nextInt(9999999);

        // this will convert any number sequence into 6 character.
        return String.format("%07d", number);

    }

    // QR code scanning
    private void scanQRCode() {
        new IntentIntegrator(DipAuthenticateActivity.this)
                .setOrientationLocked(false)
                .setBeepEnabled(false)
                .initiateScan();
    }

    private void showFirstTimeWarning() {
        Intent introIntent = new Intent(this, IntroScreenActivity.class);
        startActivityForResult(introIntent, Constants.INTENT_MAIN_INTRO);
    }

    public void authenticate(int messageId) {
        AuthMethod authMethod = settings.getAuthMethod();

       /* if (authMethod == AuthMethod.DEVICE) {
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && km.isKeyguardSecure()) {
                Intent authIntent = km.createConfirmDeviceCredentialIntent(getString(R.string.dialog_title_auth), getString(R.string.dialog_msg_auth));
                startActivityForResult(authIntent, Constants.INTENT_MAIN_AUTHENTICATE);
            }
        } else if (authMethod == AuthMethod.PASSWORD || authMethod == AuthMethod.PIN) {
            Intent authIntent = new Intent(this, AuthenticateActivity.class);
            authIntent.putExtra(Constants.EXTRA_AUTH_MESSAGE, messageId);
            startActivityForResult(authIntent, Constants.INTENT_MAIN_AUTHENTICATE);
        }*/

        if (authMethod == AuthMethod.PIN) {
            Intent authIntent = new Intent(this, AuthenticateActivity.class);
            authIntent.putExtra(Constants.EXTRA_AUTH_MESSAGE, messageId);
            startActivityForResult(authIntent, Constants.INTENT_MAIN_AUTHENTICATE);
        }
    }

    private void restoreSortMode() {
        if (settings != null && adapter != null && touchHelperCallback != null) {
            SortMode mode = settings.getSortMode();
            adapter.setSortMode(mode);

            if (mode == SortMode.UNSORTED)
                touchHelperCallback.setDragEnabled(true);
            else
                touchHelperCallback.setDragEnabled(false);
        }
    }

    private void saveSortMode(SortMode mode) {
        if (settings != null)
            settings.setSortMode(mode);
    }

    private HashMap<String, Boolean> createTagsMap(ArrayList<DipEntry> entries) {
        HashMap<String, Boolean> tagsHashMap = new HashMap<>();

        for (DipEntry entry : entries) {
            for (String tag : entry.getTags())
                tagsHashMap.put(tag, settings.getTagToggle(tag));
        }

        return tagsHashMap;
    }

    private void populateAdapter() {
        adapter.loadEntries();
        tagsDrawerAdapter.setTags(createTagsMap(adapter.getEntries()));
        adapter.filterByTags(tagsDrawerAdapter.getActiveTags());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.app_name);

        if (!settings.getScreenshotsEnabled())
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_dip_authenticate);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sp = getSharedPreferences(SaveSharedPref.PREF, Context.MODE_PRIVATE);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        settings.registerPreferenceChangeListener(this);

        encryptionType = settings.getEncryption();

        if (settings.getAuthMethod() == AuthMethod.PIN && savedInstanceState == null)
            requireAuthentication = true;

        setBroadcastCallback(new BroadcastReceivedCallback() {
            @Override
            public void onReceivedScreenOff() {
                if (settings.getRelockOnScreenOff() && settings.getAuthMethod() == AuthMethod.PIN)
                    requireAuthentication = true;
            }
        });

        if (!settings.getFirstTimeWarningShown()) {
            showFirstTimeWarning();
        }

        speedDial = findViewById(R.id.speedDial);
        speedDial.inflate(R.menu.menu_fab);

        speedDial.setOnActionSelectedListener(new SpeedDialView.OnActionSelectedListener() {
            @Override
            public boolean onActionSelected(SpeedDialActionItem speedDialActionItem) {
                switch (speedDialActionItem.getId()) {
                    case R.id.fabScanQR:
                        scanQRCode();
                        return false;
                    case R.id.fabEnterDetails:
                        DipManualEntryDialog.show(DipAuthenticateActivity.this, settings, adapter);
                        return false;
                    default:
                        return false;
                }
            }
        });

        final ProgressBar progressBar = findViewById(R.id.progressBar);

        RecyclerView recList = findViewById(R.id.cardList);
        recList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recList.setLayoutManager(llm);

        tagsDrawerAdapter = new TagsAdapter(this, new HashMap<String, Boolean>());
        adapter = new DipEntriesCardAdapter(this, tagsDrawerAdapter);
        recList.setAdapter(adapter);

        touchHelperCallback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(touchHelperCallback);
        touchHelper.attachToRecyclerView(recList);

        NotificationHelper.initializeNotificationChannels(this);
        restoreSortMode();

        float durationScale = android.provider.Settings.Global.getFloat(this.getContentResolver(), android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0);
        if (durationScale == 0)
            durationScale = 1;

        animatorDuration = (long) (1000 / durationScale);

        adapter.setCallback(new DipEntriesCardAdapter.Callback() {
            @Override
            public void onMoveEventStart() {
                stopUpdater();
            }

            @Override
            public void onMoveEventStop() {
                startUpdater();
            }
        });


        handler = new Handler();
        handlerTask = new Runnable() {
            @Override
            public void run() {

                final int progress = (int) (TokenCalculator.DIP_DEFAULT_PERIOD - (System.currentTimeMillis() / 1000) % TokenCalculator.DIP_DEFAULT_PERIOD);
                progressBar.setProgress(progress * 100);

                ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", (progress - 1) * 100);
                animation.setDuration(animatorDuration);
                animation.setInterpolator(new LinearInterpolator());
                animation.start();
                adapter.updateTimeBasedTokens();

                handler.postDelayed(this, 1000);
            }
        };
        setupDrawer();

        Intent callingIntent = getIntent();
        if (callingIntent != null && callingIntent.getAction() != null) {
            if (callingIntent.getAction().equals(INTENT_SCAN_QR)) {
                scanQRCode();
            } else if (callingIntent.getAction().equals(INTENT_ENTER_DETAILS)) {
                DipManualEntryDialog.show(DipAuthenticateActivity.this, settings, adapter);
            }
        }

        txtSeedKey = findViewById(R.id.txtSeedKey);
        String seedkey = "SeedKey: " + sp.getString(SaveSharedPref.dipseedkey, "");
        txtSeedKey.setText(seedkey);

    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        tagsToggle.syncState();
    }

    public void stopUpdater() {
        handler.removeCallbacks(handlerTask);
    }

    public void startUpdater() {
        handler.post(handlerTask);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (requireAuthentication) {
            if (settings.getAuthMethod() == AuthMethod.PIN) {
                requireAuthentication = false;
                authenticate(R.string.auth_msg_authenticate);
            }
        } else {
            if (settings.getFirstTimeWarningShown()) {
                if (adapter.getEncryptionKey() == null) {
                    updateEncryption(null);
                } else {
                    populateAdapter();
                }
            }
        }

        startUpdater();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopUpdater();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.settings_key_label_size)) ||
                key.equals(getString(R.string.settings_key_label_scroll)) ||
                key.equals(getString(R.string.settings_key_split_group_size)) ||
                key.equals(getString(R.string.settings_key_thumbnail_size))) {
            adapter.notifyDataSetChanged();
        } else if (key.equals(getString(R.string.settings_key_tap_to_reveal)) ||
                key.equals(getString(R.string.settings_key_theme)) ||
                key.equals(getString(R.string.settings_key_locale)) ||
                key.equals(getString(R.string.settings_key_enable_screenshot)) ||
                key.equals(getString(R.string.settings_key_tag_functionality))) {
            recreate();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        tagsToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result != null) {
            if (result.getContents() != null) {

                Log.e("result", " secrettest " + result.getContents());

                final String desecret = result.getContents().substring(0, Math.min(result.getContents().length(), 32));
                String digits = getRandomNumberString();

                final String label = result.getContents().substring(33, result.getContents().lastIndexOf("_"));

                final SharedPreferences sp = getSharedPreferences(SaveSharedPref.PREF, Context.MODE_PRIVATE);

                JsonObject jsonobject = new JsonObject();
                jsonobject.addProperty("desecret", desecret);
                jsonobject.addProperty("digits", digits);
                jsonobject.addProperty("seedkey", sp.getString(SaveSharedPref.dipseedkey, ""));

                try {

                    DeviceManager.Instance().sendDipDevice(jsonobject, new RestCallback.CommonInfoDelegate<DipDeviceModel>() {
                        @Override
                        public void CallDidSuccess(DipDeviceModel info) {

                            if (info != null) {
                                if (info.getSuccess().equals("1")) {
                                    List<String> tags = new ArrayList<>();

                                    String dipseedkey = sp.getString(SaveSharedPref.dipseedkey, "");

                                    DipEntry e = new DipEntry(DipEntry.OTPType.TOTP, desecret, TokenCalculator.DIP_DEFAULT_PERIOD, TokenCalculator.DIP_DEFAULT_DIGITS, label, TokenCalculator.HashAlgorithm.SHA1, tags, dipseedkey);
                                    e.updateOTP();
                                    adapter.addEntry(e);
                                    adapter.saveEntries();
                                    refreshTags();
                                }
                            }

                        }

                        @Override
                        public void CallFailedWithError(String error) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(DipAuthenticateActivity.this);
                            builder.setMessage(error)
                                    .setCancelable(false)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.dismiss();
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    });


                } catch (Exception ex) {
                    Toast.makeText(this, "Your Scanned/Manual QR Code is Invalid. Try Again !", Toast.LENGTH_LONG).show();
                }

            }
        } else if (requestCode == Constants.INTENT_MAIN_BACKUP && resultCode == RESULT_OK) {
            if (intent.getBooleanExtra("reload", false)) {
                adapter.loadEntries();
                sp.edit().putString(SaveSharedPref.dipseedkey, adapter.getDipSeedkey()).apply();
                String seedkey = "SeedKey: " + sp.getString(SaveSharedPref.dipseedkey, "");
                txtSeedKey.setText(seedkey);
                refreshTags();
            }
        } else if (requestCode == Constants.INTENT_MAIN_SETTINGS && resultCode == RESULT_OK) {
            boolean encryptionChanged = intent.getBooleanExtra(Constants.EXTRA_SETTINGS_ENCRYPTION_CHANGED, false);
            byte[] newKey = intent.getByteArrayExtra(Constants.EXTRA_SETTINGS_ENCRYPTION_KEY);

            if (encryptionChanged)
                updateEncryption(newKey);
        } else if (requestCode == Constants.INTENT_MAIN_AUTHENTICATE) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(getBaseContext(), R.string.toast_auth_failed_fatal, Toast.LENGTH_LONG).show();

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    finishAndRemoveTask();
                } else {
                    finish();
                }
            } else {
                requireAuthentication = false;

                byte[] authKey = null;

                if (intent != null)
                    authKey = intent.getByteArrayExtra(Constants.EXTRA_AUTH_PASSWORD_KEY);

                updateEncryption(authKey);
            }
        }
    }

    private void updateEncryption(byte[] newKey) {
        SecretKey encryptionKey = null;

        encryptionType = settings.getEncryption();

        if (encryptionType == EncryptionType.KEYSTORE) {
            encryptionKey = KeyStoreHelper.loadEncryptionKeyFromKeyStore(this, false);
        } else if (encryptionType == EncryptionType.PASSWORD) {
            if (newKey != null && newKey.length > 0) {
                encryptionKey = EncryptionHelper.generateSymmetricKey(newKey);
            } else {
                authenticate(R.string.auth_msg_confirm_encryption);
            }
        }

        if (encryptionKey != null)
            adapter.setEncryptionKey(encryptionKey);

        populateAdapter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        sortMenu = menu.findItem(R.id.menu_sort);

        if (adapter != null) {
            SortMode mode = adapter.getSortMode();

            if (mode == SortMode.UNSORTED) {
                sortMenu.setIcon(R.drawable.ic_sort_inverted_white);
                menu.findItem(R.id.menu_sort_none).setChecked(true);
            } else if (mode == SortMode.LABEL) {
                sortMenu.setIcon(R.drawable.ic_sort_inverted_label_white);
                menu.findItem(R.id.menu_sort_label).setChecked(true);
            } else if (mode == SortMode.LAST_USED) {
                sortMenu.setIcon(R.drawable.ic_sort_inverted_time_white);
                menu.findItem(R.id.menu_sort_last_used).setChecked(true);
            }
        }

        MenuItem searchItem = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty())
                    adapter.filterByTags(tagsDrawerAdapter.getActiveTags());
                else
                    adapter.getFilter().filter(newText);

                return false;
            }
        });

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                speedDial.setVisibility(View.GONE);
                touchHelperCallback.setDragEnabled(false);
                if (sortMenu != null)
                    sortMenu.setVisible(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                speedDial.setVisibility(View.VISIBLE);

                if (adapter == null || adapter.getSortMode() == SortMode.UNSORTED)
                    touchHelperCallback.setDragEnabled(true);

                if (sortMenu != null)
                    sortMenu.setVisible(true);

                return true;
            }
        });

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_backup) {
            Intent backupIntent = new Intent(this, DipBackupActivity.class);
            backupIntent.putExtra(Constants.EXTRA_BACKUP_ENCRYPTION_KEY, adapter.getEncryptionKey().getEncoded());
            startActivityForResult(backupIntent, Constants.INTENT_MAIN_BACKUP);
        } else if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, DipSettingsActivity.class);
            if (adapter.getEncryptionKey() != null)
                settingsIntent.putExtra(Constants.EXTRA_SETTINGS_ENCRYPTION_KEY, adapter.getEncryptionKey().getEncoded());
            startActivityForResult(settingsIntent, Constants.INTENT_MAIN_SETTINGS);
        } else if (id == R.id.action_about) {
            Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
            return true;
        } else if (id == R.id.menu_sort_none) {
            item.setChecked(true);
            sortMenu.setIcon(R.drawable.ic_sort_inverted_white);
            saveSortMode(SortMode.UNSORTED);
            if (adapter != null) {
                adapter.setSortMode(SortMode.UNSORTED);
                touchHelperCallback.setDragEnabled(true);
            }
        } else if (id == R.id.menu_sort_label) {
            item.setChecked(true);
            sortMenu.setIcon(R.drawable.ic_sort_inverted_label_white);
            saveSortMode(SortMode.LABEL);
            if (adapter != null) {
                adapter.setSortMode(SortMode.LABEL);
                touchHelperCallback.setDragEnabled(false);
            }
        } else if (id == R.id.menu_sort_last_used) {
            item.setChecked(true);
            sortMenu.setIcon(R.drawable.ic_sort_inverted_time_white);
            saveSortMode(SortMode.LAST_USED);
            if (adapter != null) {
                adapter.setSortMode(SortMode.LAST_USED);
                touchHelperCallback.setDragEnabled(false);
            }
            if (!settings.getLastUsedDialogShown())
                showLastUsedDialog();
        } else if (tagsToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showLastUsedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title_manual_entry)
                .setTitle(R.string.dialog_title_last_used)
                .setMessage(R.string.dialog_msg_last_used)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        settings.setLastUsedDialogShown(true);
                    }
                })
                .create()
                .show();
    }

    private void setupDrawer() {
        tagsDrawerListView = findViewById(R.id.tags_list_in_drawer);

        final DrawerLayout tagsDrawerLayout = findViewById(R.id.drawer_layout);

        tagsToggle = new ActionBarDrawerToggle(this, tagsDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(R.string.label_tags);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(R.string.app_name);
                invalidateOptionsMenu();
            }
        };

        tagsToggle.setDrawerIndicatorEnabled(true);
        tagsDrawerLayout.addDrawerListener(tagsToggle);

        final CheckedTextView noTagsButton = findViewById(R.id.no_tags_entries);
        final CheckedTextView allTagsButton = findViewById(R.id.all_tags_in_drawer);

        allTagsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckedTextView checkedTextView = ((CheckedTextView) view);
                checkedTextView.setChecked(!checkedTextView.isChecked());

                settings.setAllTagsToggle(checkedTextView.isChecked());

                for (int i = 0; i < tagsDrawerListView.getChildCount(); i++) {
                    CheckedTextView childCheckBox = (CheckedTextView) tagsDrawerListView.getChildAt(i);
                    childCheckBox.setChecked(checkedTextView.isChecked());
                    tagsDrawerAdapter.setTagState(childCheckBox.getText().toString(), childCheckBox.isChecked());
                    settings.setTagToggle(childCheckBox.getText().toString(), childCheckBox.isChecked());
                }

                if (checkedTextView.isChecked()) {
                    adapter.filterByTags(tagsDrawerAdapter.getActiveTags());
                } else {
                    adapter.filterByTags(new ArrayList<String>());
                }
            }
        });
        allTagsButton.setChecked(settings.getAllTagsToggle());

        noTagsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckedTextView checkedTextView = ((CheckedTextView) view);
                checkedTextView.setChecked(!checkedTextView.isChecked());

                if (settings.getTagFunctionality() == Constants.TagFunctionality.SINGLE) {
                    checkedTextView.setChecked(true);
                    allTagsButton.setChecked(false);
                    settings.setAllTagsToggle(false);

                    for (String tag : tagsDrawerAdapter.getTags()) {
                        settings.setTagToggle(tag, false);
                        tagsDrawerAdapter.setTagState(tag, false);
                    }
                }

                settings.setNoTagsToggle(checkedTextView.isChecked());
                adapter.filterByTags(tagsDrawerAdapter.getActiveTags());
            }
        });
        noTagsButton.setChecked(settings.getNoTagsToggle());

        tagsDrawerListView.setAdapter(tagsDrawerAdapter);
        tagsDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckedTextView checkedTextView = ((CheckedTextView) view);

                if (settings.getTagFunctionality() == Constants.TagFunctionality.SINGLE) {
                    allTagsButton.setChecked(false);
                    settings.setAllTagsToggle(false);
                    noTagsButton.setChecked(false);
                    settings.setNoTagsToggle(false);

                    for (String tag : tagsDrawerAdapter.getTags()) {
                        settings.setTagToggle(tag, false);
                        tagsDrawerAdapter.setTagState(tag, false);
                    }
                    checkedTextView.setChecked(true);
                } else {
                    checkedTextView.setChecked(!checkedTextView.isChecked());
                }

                settings.setTagToggle(checkedTextView.getText().toString(), checkedTextView.isChecked());
                tagsDrawerAdapter.setTagState(checkedTextView.getText().toString(), checkedTextView.isChecked());

                if (!checkedTextView.isChecked()) {
                    allTagsButton.setChecked(false);
                    settings.setAllTagsToggle(false);
                }

                if (tagsDrawerAdapter.allTagsActive()) {
                    allTagsButton.setChecked(true);
                    settings.setAllTagsToggle(true);
                }

                adapter.filterByTags(tagsDrawerAdapter.getActiveTags());
            }
        });

        adapter.filterByTags(tagsDrawerAdapter.getActiveTags());
    }

    public void refreshTags() {
        HashMap<String, Boolean> tagsHashMap = new HashMap<>();
        for (String tag : tagsDrawerAdapter.getTags()) {
            tagsHashMap.put(tag, false);
        }
        for (String tag : tagsDrawerAdapter.getActiveTags()) {
            tagsHashMap.put(tag, true);
        }
        for (String tag : adapter.getTags()) {
            if (!tagsHashMap.containsKey(tag))
                tagsHashMap.put(tag, true);
        }
        tagsDrawerAdapter.setTags(tagsHashMap);
        adapter.filterByTags(tagsDrawerAdapter.getActiveTags());
    }

}
