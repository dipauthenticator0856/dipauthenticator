/*
 * Copyright (C) 2017-2018 Jakob Nixdorf
 * Copyright (C) 2015 Bruno Bierbaumer
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

package org.shadowice.flocke.andotp.Activities;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.v2.files.FileMetadata;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.gson.JsonObject;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.shadowice.flocke.andotp.Common.Network.Rest.RestClient;
import org.shadowice.flocke.andotp.Common.Utilities.NetworkConnectivity;
import org.shadowice.flocke.andotp.Database.Entry;
import org.shadowice.flocke.andotp.Dialogs.PasswordEntryDialog;
import org.shadowice.flocke.andotp.Googlepay.PaymentsUtil;
import org.shadowice.flocke.andotp.Helpers.DriveServiceHelper;
import org.shadowice.flocke.andotp.Helpers.DropboxServices.DropboxClient;
import org.shadowice.flocke.andotp.Helpers.DropboxServices.DropboxServiceHelper;
import org.shadowice.flocke.andotp.Helpers.DropboxServices.UploadTask;
import org.shadowice.flocke.andotp.R;
import org.shadowice.flocke.andotp.Services.API.Models.NotificationModel;
import org.shadowice.flocke.andotp.Utilities.Constants;
import org.shadowice.flocke.andotp.Utilities.DatabaseHelper;
import org.shadowice.flocke.andotp.Utilities.EncryptionHelper;
import org.shadowice.flocke.andotp.Utilities.FileHelper;
import org.shadowice.flocke.andotp.Utilities.KeyStoreHelper;
import org.shadowice.flocke.andotp.Utilities.NotificationHelper;
import org.shadowice.flocke.andotp.Utilities.SaveSharedPref;
import org.shadowice.flocke.andotp.Utilities.Settings;
import org.shadowice.flocke.andotp.Utilities.TokenCalculator;
import org.shadowice.flocke.andotp.Utilities.Tools;
import org.shadowice.flocke.andotp.View.EntriesCardAdapter;
import org.shadowice.flocke.andotp.View.ItemTouchHelper.AdapterItemClickListener;
import org.shadowice.flocke.andotp.View.ItemTouchHelper.SimpleItemTouchHelperCallback;
import org.shadowice.flocke.andotp.View.ManualEntryDialog;
import org.shadowice.flocke.andotp.View.NotificationAdapter;
import org.shadowice.flocke.andotp.View.TagsAdapter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.crypto.SecretKey;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.shadowice.flocke.andotp.Utilities.Constants.AuthMethod;
import static org.shadowice.flocke.andotp.Utilities.Constants.EncryptionType;
import static org.shadowice.flocke.andotp.Utilities.Constants.SortMode;

public class MainActivity extends BaseActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 991;
    private static final int REQUEST_CODE_SIGN_IN = 992;

    private static final String INTENT_SCAN_QR = "org.shadowice.flocke.andotp.intent.SCAN_QR";
    private static final String INTENT_ENTER_DETAILS = "org.shadowice.flocke.andotp.intent.ENTER_DETAILS";
    public static long animatorDuration = 1000;
    private static MainActivity instance;
    ProgressBar progressBar;
    ViewStub stub;
    SecretKey encryptionKey = null;
    private EntriesCardAdapter adapter;
    private SpeedDialView speedDial;
    private MenuItem sortMenu;
    private SimpleItemTouchHelperCallback touchHelperCallback;
    private EncryptionType encryptionType = EncryptionType.KEYSTORE;
    private boolean requireAuthentication = false;
    private Handler handler;
    private Runnable handlerTask;
    private TagsAdapter tagsDrawerAdapter;
    private ActionBarDrawerToggle tagsToggle;
    private SharedPreferences sp;
    private Button btnGetPremium;

    private RecyclerView recList;
    //    private RecyclerView recyclerViewNotification;
    private RelativeLayout actionbarLayout;

    private NotificationAdapter notificationAdapter;
    private List<NotificationModel> notificationList = new ArrayList<>();

    private ImageView imgMenu, imgSearch, imgSort;
    private FrameLayout imgNotification;
    private TextView txtNotificationCounter;

    private PaymentsClient mPaymentsClient;
    private RelativeLayout mGooglePayButton;

    private DriveServiceHelper mDriveServiceHelper;
    private String TAG = this.getClass().getSimpleName();

    private DrawerLayout tagsDrawerLayout;

    private DropboxServiceHelper dropboxServiceHelper;

    public static MainActivity getInstance() {
        return instance;
    }

    // QR code scanning
    private void scanQRCode() {
        new IntentIntegrator(MainActivity.this)
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

    private HashMap<String, Boolean> createTagsMap(ArrayList<Entry> entries) {
        HashMap<String, Boolean> tagsHashMap = new HashMap<>();

        for (Entry entry : entries) {
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

    // Initialize the main application
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.app_name);

        if (!settings.getScreenshotsEnabled())
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences(SaveSharedPref.PREF, Context.MODE_PRIVATE);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        settings.registerPreferenceChangeListener(this);

        instance = this;
        dropboxServiceHelper = new DropboxServiceHelper(this);

        speedDial = findViewById(R.id.speedDial);
        progressBar = findViewById(R.id.progressBar);
        btnGetPremium = findViewById(R.id.btnGetPremium);
        stub = findViewById(R.id.container_stub);
        actionbarLayout = findViewById(R.id.actionbarLayout);

        mPaymentsClient = PaymentsUtil.createPaymentsClient(this);

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

        speedDial.inflate(R.menu.menu_fab);

        speedDial.setOnActionSelectedListener(new SpeedDialView.OnActionSelectedListener() {
            @Override
            public boolean onActionSelected(SpeedDialActionItem speedDialActionItem) {

               /* if (settings.getBackupPasswordEnc().isEmpty()) {

//                    getSupportActionBar().hide();
                    actionbarLayout.setVisibility(View.GONE);
                    speedDial.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    btnGetPremium.setVisibility(View.GONE);

                    stub.inflate();

                    getFragmentManager().beginTransaction()
                            .replace(R.id.container_content, new BackupPasswordFragment())
                            .commit();

                }*/

                if (sp.getBoolean(SaveSharedPref.isLocalAutobackupChecked, false) || sp.getBoolean(SaveSharedPref.isCloudAutobackupChecked, false)) {

                    switch (speedDialActionItem.getId()) {
                        case R.id.fabScanQR:
                            scanQRCode();
                            return false;
                        case R.id.fabEnterDetails:
                            ManualEntryDialog.show(MainActivity.this, settings, adapter);
                            return false;
                        default:
                            return false;
                    }


                  /*  if (sp.getString(SaveSharedPref.checkPlan, "").toLowerCase().equals("free")) {

                        Log.e("listsize", " " + adapter.getListSize());
                        if (adapter.getListSize() < 5) {
                            switch (speedDialActionItem.getId()) {
                                case R.id.fabScanQR:
                                    scanQRCode();
                                    return false;
                                case R.id.fabEnterDetails:
                                    ManualEntryDialog.show(MainActivity.this, settings, adapter);
                                    return false;
                                default:
                                    return false;
                            }
                        } else {

                            String msg = "You can't add new account because Your FREE Subscription has exceeded its limit. You need to Get Paid with Universal Hosted Premium plans into our official website at www.dipauthenticator.com.";

                            // Linkify the message
                            final SpannableString spannableString = new SpannableString(msg); // msg should have url to enable clicking
                            Linkify.addLinks(spannableString, Linkify.ALL);

                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("Alert")
                                    .setMessage(spannableString)
                                    .setCancelable(false)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.dismiss();
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();

                            // Make the textview clickable. Must be called after show()
                            ((TextView) alert.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                        }

                    } else {
                        switch (speedDialActionItem.getId()) {
                            case R.id.fabScanQR:
                                scanQRCode();
                                return false;
                            case R.id.fabEnterDetails:
                                ManualEntryDialog.show(MainActivity.this, settings, adapter);
                                return false;
                            default:
                                return false;
                        }
                    }*/

                } else {

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Warning!")
                            .setMessage("You need to Enable Auto Backup First before Enable 2FA account.")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Intent backupIntent = new Intent(MainActivity.this, BackupActivity.class);
                                    backupIntent.putExtra(Constants.EXTRA_BACKUP_ENCRYPTION_KEY, adapter.getEncryptionKey().getEncoded());
                                    startActivityForResult(backupIntent, Constants.INTENT_MAIN_BACKUP);
                                }
                            })
                            .create()
                            .show();


/*                    actionbarLayout.setVisibility(View.GONE);
                    speedDial.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    btnGetPremium.setVisibility(View.GONE);

                    stub.inflate();

                    getFragmentManager().beginTransaction()
                            .replace(R.id.container_content, new BackupPasswordFragment())
                            .commit();*/

                }

                return false;
            }
        });

        recList = findViewById(R.id.cardList);
        recList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recList.setLayoutManager(llm);

        tagsDrawerAdapter = new TagsAdapter(this, new HashMap<String, Boolean>());
        adapter = new EntriesCardAdapter(this, tagsDrawerAdapter);
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

        adapter.setCallback(new EntriesCardAdapter.Callback() {
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

                final int progress = (int) (TokenCalculator.TOTP_DEFAULT_PERIOD - (System.currentTimeMillis() / 1000) % TokenCalculator.TOTP_DEFAULT_PERIOD);
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
                ManualEntryDialog.show(MainActivity.this, settings, adapter);
            } else if (callingIntent.hasExtra("title")) {
                if (callingIntent.getStringExtra("title").equalsIgnoreCase("notification")) {
                    startActivity(new Intent(getApplicationContext(), NotificationActivity.class));
                }
            }
        }

        checkPlan();

        setActionBarData();

        btnGetPremium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPremiumDialog();
//                paymentDialog();
            }
        });

    }

    private void setActionBarData() {

        imgMenu = findViewById(R.id.imgMenu);
        imgSearch = findViewById(R.id.imgSearch);
        imgSort = findViewById(R.id.imgSort);
        imgNotification = findViewById(R.id.imgNotification);
        txtNotificationCounter = findViewById(R.id.txtNotificationCounter);
        final EditText edtSearch = findViewById(R.id.edtSearch);
        final TextView txtTitle = findViewById(R.id.txtTitle);
        final ImageView imgClose = findViewById(R.id.imgClose);

        edtSearch.setVisibility(View.GONE);
        imgClose.setVisibility(View.GONE);
        getNotifications();

        imgNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), NotificationActivity.class));
            }
        });

        imgSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtTitle.setVisibility(View.GONE);
                edtSearch.setVisibility(View.VISIBLE);
                imgClose.setVisibility(View.VISIBLE);
            }
        });

        if (adapter != null) {
            SortMode mode = adapter.getSortMode();

            if (mode == SortMode.UNSORTED) {
                imgSort.setImageResource(R.drawable.ic_sort_inverted_white);
            } else if (mode == SortMode.LABEL) {
                imgSort.setImageResource(R.drawable.ic_sort_inverted_label_white);
            } else if (mode == SortMode.LAST_USED) {
                imgSort.setImageResource(R.drawable.ic_sort_inverted_time_white);
            }
        }

        imgSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final PopupMenu popup = new PopupMenu(MainActivity.this, imgSort);
                popup.getMenuInflater()
                        .inflate(R.menu.menu_sort, popup.getMenu());

                final Menu popupMenu = popup.getMenu();

                if (adapter != null) {
                    SortMode mode = adapter.getSortMode();

                    if (mode == SortMode.UNSORTED) {
                        popupMenu.findItem(R.id.menu_sort_none).setChecked(true);
                    } else if (mode == SortMode.LABEL) {
                        popupMenu.findItem(R.id.menu_sort_label).setChecked(true);
                    } else if (mode == SortMode.LAST_USED) {
                        popupMenu.findItem(R.id.menu_sort_last_used).setChecked(true);
                    }
                }

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {

                        if (item.getItemId() == R.id.menu_sort_none) {
                            item.setChecked(true);
                            imgSort.setImageResource(R.drawable.ic_sort_inverted_white);
                            saveSortMode(SortMode.UNSORTED);
                            if (adapter != null) {
                                adapter.setSortMode(SortMode.UNSORTED);
                                touchHelperCallback.setDragEnabled(true);
                            }
                        } else if (item.getItemId() == R.id.menu_sort_label) {
                            item.setChecked(true);
                            imgSort.setImageResource(R.drawable.ic_sort_inverted_label_white);
                            saveSortMode(SortMode.LABEL);
                            if (adapter != null) {
                                adapter.setSortMode(SortMode.LABEL);
                                touchHelperCallback.setDragEnabled(false);
                            }
                        } else if (item.getItemId() == R.id.menu_sort_last_used) {
                            item.setChecked(true);
                            imgSort.setImageResource(R.drawable.ic_sort_inverted_time_white);
                            saveSortMode(SortMode.LAST_USED);
                            if (adapter != null) {
                                adapter.setSortMode(SortMode.LAST_USED);
                                touchHelperCallback.setDragEnabled(false);
                            }
                            if (!settings.getLastUsedDialogShown())
                                showLastUsedDialog();
                        }

                        return true;
                    }
                });

                popup.show(); //showing popup menu

            }
        });

        imgMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                openOrCloseDrawer();
               /* final PopupMenu popup = new PopupMenu(MainActivity.this, imgMenu);
                popup.getMenuInflater()
                        .inflate(R.menu.menu_side, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {

                        int id = item.getItemId();

                        if (id == R.id.action_backup) {
                            Intent backupIntent = new Intent(MainActivity.this, BackupActivity.class);
                            backupIntent.putExtra(Constants.EXTRA_BACKUP_ENCRYPTION_KEY, adapter.getEncryptionKey().getEncoded());
                            startActivityForResult(backupIntent, Constants.INTENT_MAIN_BACKUP);
                        } else if (id == R.id.action_settings) {
                            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                            if (adapter.getEncryptionKey() != null)
                                settingsIntent.putExtra(Constants.EXTRA_SETTINGS_ENCRYPTION_KEY, adapter.getEncryptionKey().getEncoded());
                            startActivityForResult(settingsIntent, Constants.INTENT_MAIN_SETTINGS);
                        } else if (id == R.id.action_faqs) {
                            Uri uri = Uri.parse("https://www.dipauthenticator.com/FAQ.php"); // missing 'http://' will cause crashed
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                        } else if (id == R.id.action_about) {
                            Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
                            startActivity(aboutIntent);
                            return true;
                        }

                        return true;
                    }
                });

                popup.show(); //showing popup menu
*/
            }
        });

        if (edtSearch.getText().toString().trim().isEmpty()) {
            adapter.filterByTags(tagsDrawerAdapter.getActiveTags());
        }

        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                edtSearch.getText().clear();
                edtSearch.setVisibility(View.GONE);
                imgClose.setVisibility(View.GONE);
                txtTitle.setVisibility(View.VISIBLE);
            }
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                String newText = charSequence.toString();
                if (newText.isEmpty())
                    adapter.filterByTags(tagsDrawerAdapter.getActiveTags());
                else
                    adapter.getFilter().filter(newText);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

    }

    private void getNotifications() {

        final ProgressDialog pDialog = new ProgressDialog(this);
        pDialog.setMessage("Please wait...");
        pDialog.setCancelable(false);
        pDialog.show();

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("seedkey", sp.getString(SaveSharedPref.universalseedkey, ""));
//        jsonObject.addProperty("seedkey", "2a58b7740122dc1b");

        Call<ResponseBody> calling = RestClient.getEcombidService().create(RestClient.ServiceInterface.class).getNotification(jsonObject);
        calling.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {

                if (pDialog.isShowing())
                    pDialog.dismiss();

                if (response.code() == 200) {
                    try {
                        JSONObject object = new JSONObject(response.body().string());

                        if (object.getString("success").equals("1")) {

                            JSONArray jsonArray = object.getJSONArray("posts");

                            String notifications = String.valueOf(jsonArray.length());

                            txtNotificationCounter.setText(notifications);

                            for (int i = 0; i < jsonArray.length(); i++) {

                                JSONObject object1 = jsonArray.getJSONObject(i);

                                NotificationModel model = new NotificationModel();
                                model.setCount(object1.getString("count"));
                                model.setMessage(object1.getString("message"));
                                model.setTime(object1.getString("time"));
                                model.setTitle(object1.getString("title"));

                                notificationList.add(model);
                            }

                            notificationAdapter = new NotificationAdapter(notificationList);
//                            recyclerViewNotification.setAdapter(notificationAdapter);
                            notificationAdapter.setCustomCallback(new AdapterItemClickListener<String>() {
                                @Override
                                public void onItemClickData(String data) {

                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setMessage(data)
                                            .setCancelable(false)
                                            .setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.dismiss();
                                                }
                                            });
                                    AlertDialog alert = builder.create();
                                    alert.show();

                                }
                            });

                        }
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }

            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (pDialog.isShowing())
                    pDialog.dismiss();
            }
        });

    }

    public void BackupPasswordChanged() {

//        getSupportActionBar().show();
        actionbarLayout.setVisibility(View.VISIBLE);
        stub.setVisibility(View.GONE);
        speedDial.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
//        btnGetPremium.setVisibility(View.VISIBLE);

    }

    /**
     * Starts a sign-in activity using {@link #REQUEST_CODE_SIGN_IN}.
     */
    private void requestSignIn() {
        Log.d(TAG, "Requesting sign-in");

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    private void paymentDialog() {

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_payment);

        RadioGroup radioGrp = dialog.findViewById(R.id.radioGrp);
        ((RadioButton) radioGrp.getChildAt(0)).setChecked(true);

        mGooglePayButton = dialog.findViewById(R.id.googlepay_button);
        TextView txtGoogleError = dialog.findViewById(R.id.txtGoogleError);
//        mPaymentsClient = PaymentsUtil.createPaymentsClient(this);
//        possiblyShowGooglePayButton();

        final JSONObject isReadyToPayJson = PaymentsUtil.getIsReadyToPayRequest();
        Log.e("isReadyToPayJson", " " + isReadyToPayJson);
        if (isReadyToPayJson.length() == 0) {
            return;
        }
        IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString());
        if (request == null) {
            return;
        }

        // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
        // OnCompleteListener to be triggered when the result of the call is known.
        Task<Boolean> task = mPaymentsClient.isReadyToPay(request);
        task.addOnCompleteListener(this,
                new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
//                            setGooglePayAvailable(task.getResult());
                            mGooglePayButton.setVisibility(View.VISIBLE);
                            txtGoogleError.setVisibility(View.GONE);
                            Log.w("isReadyToPay Successful", " " + task.getResult());
                        } else {
                            Log.w("isReadyToPay failed", task.getException());
                            mGooglePayButton.setVisibility(View.GONE);
                            txtGoogleError.setVisibility(View.VISIBLE);
                        }
                    }
                });


        mGooglePayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int selectedId = radioGrp.getCheckedRadioButtonId();
                RadioButton radioButton = dialog.findViewById(selectedId);

                requestPayment(view, radioButton.getText().toString().trim().replace("$", ""));
                dialog.dismiss();
            }
        });

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = (int) ((int) displaymetrics.widthPixels * 0.9);
        int height = (int) ((int) displaymetrics.heightPixels * 0.5);

        dialog.getWindow().setLayout(width, height);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        dialog.show();

    }

    public void requestPayment(View view, String price) {
        // Disables the button to prevent multiple clicks.
        mGooglePayButton.setClickable(false);

        // The price provided to the API should include taxes and shipping.
        // This price is not displayed to the user.
//        String price = "5";

        // TransactionInfo transaction = PaymentsUtil.createTransaction(price);
/*        Optional<JSONObject> paymentDataRequestJson = PaymentsUtil.getPaymentDataRequest(price);
        Log.e("paymentDataRequestJson"," "+paymentDataRequestJson.isPresent());
        if (!paymentDataRequestJson.isPresent()) {
            return;
        }
        PaymentDataRequest request =
                PaymentDataRequest.fromJson(paymentDataRequestJson.get().toString());*/


        JSONObject paymentDataRequestJson = PaymentsUtil.getPaymentDataRequest(price);
        Log.e("paymentDataRequestJson", " " + paymentDataRequestJson);
        if (paymentDataRequestJson.length() == 0) {
            return;
        }
        PaymentDataRequest request =
                PaymentDataRequest.fromJson(paymentDataRequestJson.toString());

        // Since loadPaymentData may show the UI asking the user to select a payment method, we use
        // AutoResolveHelper to wait for the user interacting with it. Once completed,
        // onActivityResult will be called with the result.
        if (request != null) {
            AutoResolveHelper.resolveTask(
                    mPaymentsClient.loadPaymentData(request), this, LOAD_PAYMENT_DATA_REQUEST_CODE);
        }
    }

    private void getPremiumDialog() {

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.getpremium_dialog);

        final EditText edtPaymentCode = dialog.findViewById(R.id.edtPaymentCode);
        final Button btnSubmit = dialog.findViewById(R.id.btnSubmit);
        final Button btnClose = dialog.findViewById(R.id.btnClose);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String plancode = edtPaymentCode.getText().toString().trim();
                if (plancode.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Enter Transaction Id OR Payment Code", Toast.LENGTH_LONG).show();
                } else {

                    final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                    progressDialog.setCancelable(false);
                    progressDialog.setMessage("Please wait...");
                    progressDialog.show();

                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("plancode", plancode);
                    jsonObject.addProperty("seedkey", sp.getString(SaveSharedPref.universalseedkey, ""));

                    Call<ResponseBody> calling = RestClient.getEcombidService().create(RestClient.ServiceInterface.class).submitPlan(jsonObject);
                    calling.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                            if (progressDialog.isShowing())
                                progressDialog.dismiss();

                            try {
                                JSONObject responseObj = new JSONObject(response.body().string());

                                if (responseObj.getString("success").equals("1")) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setMessage(responseObj.getJSONObject("posts").getString("message"))
                                            .setCancelable(false)
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog1, int id) {
                                                    dialog1.dismiss();
                                                    dialog.dismiss();
                                                }
                                            });
                                    AlertDialog alert = builder.create();
                                    alert.show();
                                } else {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setMessage(responseObj.getString("posts"))
                                            .setCancelable(false)
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog1, int id) {
                                                    dialog1.dismiss();
                                                    dialog.dismiss();
                                                }
                                            });
                                    AlertDialog alert = builder.create();
                                    alert.show();
                                }
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            if (progressDialog.isShowing())
                                progressDialog.dismiss();
                            dialog.dismiss();
                        }
                    });

                }


            }
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        dialog.show();

    }

    private void possiblyShowGooglePayButton() {
       /* final Optional<JSONObject> isReadyToPayJson = PaymentsUtil.getIsReadyToPayRequest();
        Log.e("isReadyToPayJson"," "+isReadyToPayJson);
        if (!isReadyToPayJson.isPresent()) {
            return;
        }
        IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(isReadyToPayJson.get().toString());
        if (request == null) {
            return;
        }*/

        final JSONObject isReadyToPayJson = PaymentsUtil.getIsReadyToPayRequest();
        Log.e("isReadyToPayJson", " " + isReadyToPayJson);
        if (isReadyToPayJson.length() == 0) {
            return;
        }
        IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString());
        if (request == null) {
            return;
        }

        // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
        // OnCompleteListener to be triggered when the result of the call is known.
        Task<Boolean> task = mPaymentsClient.isReadyToPay(request);
        task.addOnCompleteListener(this,
                new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
//                            setGooglePayAvailable(task.getResult());
                            Log.w("isReadyToPay Successful", " " + task.getResult());
                        } else {
                            Log.w("isReadyToPay failed", task.getException());
                        }
                    }
                });
    }

    private void handlePaymentSuccess(PaymentData paymentData) {
        String paymentInformation = paymentData.toJson();

        // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
        if (paymentInformation == null) {
            return;
        }
        JSONObject paymentMethodData;

        try {
            paymentMethodData = new JSONObject(paymentInformation).getJSONObject("paymentMethodData");
            // If the gateway is set to "example", no payment information is returned - instead, the
            // token will only consist of "examplePaymentMethodToken".
            if (paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("type")
                    .equals("PAYMENT_GATEWAY")
                    && paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("token")
                    .equals("examplePaymentMethodToken")) {
                AlertDialog alertDialog =
                        new AlertDialog.Builder(this)
                                .setTitle("Warning")
                                .setMessage(
                                        "Gateway name set to \"example\" - please modify "
                                                + "Constants.java and replace it with your own gateway.")
                                .setPositiveButton("OK", null)
                                .create();
                alertDialog.show();
            }
            Log.d("paymentMethodData", " " + paymentMethodData);

   /*         String billingName =
                    paymentMethodData.getJSONObject("info").getJSONObject("billingAddress").getString("name");
            Log.d("BillingName", billingName);
            Toast.makeText(this, "Successfully received payment data for " + billingName, Toast.LENGTH_LONG)
                    .show();*/

            // Logging token string.
            Log.d("GooglePaymentToken", paymentMethodData.getJSONObject("tokenizationData").getString("token"));
        } catch (JSONException e) {
            Log.e("handlePaymentSuccess", "Error: " + e.toString());
            return;
        }
    }

    private void checkPlan() {

        final ProgressDialog pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);
        pDialog.setMessage("Please wait...");
        pDialog.show();

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("seedkey", sp.getString(SaveSharedPref.universalseedkey, ""));

        Call<ResponseBody> calling = RestClient.getEcombidService().create(RestClient.ServiceInterface.class).checkPlan(jsonObject);

        calling.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {

                if (pDialog.isShowing())
                    pDialog.dismiss();

                try {

                    JSONObject jsonObject = new JSONObject(response.body().string());

                    if (jsonObject.getString("success").equals("1")) {
                        sp.edit().putString(SaveSharedPref.checkPlan, jsonObject.getString("posts")).apply();
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (pDialog.isShowing())
                    pDialog.dismiss();
                System.out.println("onFailure");
            }
        });

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        tagsToggle.syncState();
    }

    // Controls for the updater background task
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
    public void onPause() {
        super.onPause();
        stopUpdater();
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
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

    // Activity results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result != null) {
            if (result.getContents() != null) {

                Log.e("result ", " getContents " + result.getContents());
                String contents = result.getContents() + "&seedkey=" + sp.getString(SaveSharedPref.universalseedkey, "");
                try {
                    Entry e = new Entry(contents);
                    e.updateOTP();
                    adapter.addEntry(e);
                    adapter.saveEntries();
                    refreshTags();

                    if (sp.getBoolean(SaveSharedPref.isLocalAutobackupChecked, false)) {
                        doAutoBackup();
                    }

                    if (sp.getBoolean(SaveSharedPref.isCloudAutobackupChecked, false)) {
                        if (sp.getBoolean(SaveSharedPref.isGoogleDriveBackupChecked, false)) {
                            requestSignIn();
                        }

                        if (sp.getBoolean(SaveSharedPref.isDropboxBackupChecked, false)) {
                            dropboxServiceHelper.getAccessToken();
                            if (dropboxServiceHelper.tokenExists()) {
                                dropboxServiceHelper.getDropboxUserAccount();
                                doAutoBackup();
                            }
                        }
                    }

                } catch (Exception e) {
//                    Toast.makeText(this, R.string.toast_invalid_qr_code, Toast.LENGTH_LONG).show();
                    Toast.makeText(this, "Your Scanned/Manual QR Code is Invalid. Try Again !", Toast.LENGTH_LONG).show();
                }
            }
        } else if (requestCode == Constants.INTENT_MAIN_BACKUP && resultCode == RESULT_OK) {
            if (intent.getBooleanExtra("reload", false)) {
                adapter.loadEntries();
                if (adapter.getListSize() > 0) {
                    sp.edit().putString(SaveSharedPref.universalseedkey, adapter.getSeedkeyFromAdapter()).apply();
                }
                refreshTags();
            }
        } else if (requestCode == Constants.INTENT_MAIN_SETTINGS && resultCode == RESULT_OK) {
            boolean encryptionChanged = intent.getBooleanExtra(Constants.EXTRA_SETTINGS_ENCRYPTION_CHANGED, false);
            byte[] newKey = intent.getByteArrayExtra(Constants.EXTRA_SETTINGS_ENCRYPTION_KEY);

            if (encryptionChanged) {
                updateEncryption(newKey);
            }

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
        } else if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    PaymentData paymentData = PaymentData.getFromIntent(intent);
                    handlePaymentSuccess(paymentData);
                    break;
                case Activity.RESULT_CANCELED:
                    // Nothing to here normally - the user simply cancelled without selecting a
                    // payment method.
                    break;
                case AutoResolveHelper.RESULT_ERROR:
                    Status status = AutoResolveHelper.getStatusFromIntent(intent);
                    Log.w("loadPaymentData failed", String.format("Error code: %d", status.getStatusCode()));
                    break;
                default:
                    // Do nothing.
            }

            // Re-enables the Google Pay payment button.
            mGooglePayButton.setClickable(true);
        } else if (requestCode == REQUEST_CODE_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                handleSignInResult(intent);
            }
        }
    }

    /**
     * Handles the {@code result} of a completed sign-in activity initiated from {@link
     * #requestSignIn()}.
     */

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.d(TAG, "Signed in as " + googleAccount.getEmail());

                    // Use the authenticated account to sign in to the Drive service.
                    GoogleAccountCredential credential =
                            GoogleAccountCredential.usingOAuth2(
                                    this, Collections.singleton(DriveScopes.DRIVE_FILE));
                    credential.setSelectedAccount(googleAccount.getAccount());
                    Drive googleDriveService =
                            new Drive.Builder(
                                    AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(),
                                    credential)
                                    .setApplicationName("Drive API Migration")
                                    .build();

                    // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                    // Its instantiation is required before handling any onClick actions.
                    mDriveServiceHelper = new DriveServiceHelper(googleDriveService);

                    doAutoBackup();

//                    createFile();
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));

    }

    private void uploadBackupToGoogleDrive() {

        if (NetworkConnectivity.isConnected()) {
            if (mDriveServiceHelper != null) {

                String mimeType = Constants.BACKUP_MIMETYPE_CRYPT;
                String filename = Constants.AUTOBACKUP_FILE_NAME;
                Uri uri = Tools.buildUri(settings.getBackupDir(), filename);

                mDriveServiceHelper.getExistsFolder(Constants.BACKUP_FOLDER_NAME, "root").addOnSuccessListener(new OnSuccessListener<File>() {
                    @Override
                    public void onSuccess(File folder) {

                        if (folder == null) {

                            mDriveServiceHelper.createFolder(Constants.BACKUP_FOLDER_NAME).addOnSuccessListener(new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(String folderid) {

                                    mDriveServiceHelper.getExistsFilesFromFolder(mimeType, filename, folderid).addOnSuccessListener(new OnSuccessListener<File>() {
                                        @Override
                                        public void onSuccess(File file) {

                                            if (file == null) {
                                                mDriveServiceHelper.createFile(mimeType, uri, filename, folderid)
                                                        .addOnSuccessListener(new OnSuccessListener<String>() {
                                                            @Override
                                                            public void onSuccess(String fileid) {

                                                            }
                                                        })
                                                        .addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {

                                                            }
                                                        });
                                            } else {

                                                mDriveServiceHelper.deleteFile(file.getId()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        mDriveServiceHelper.createFile(mimeType, uri, filename, folderid)
                                                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                                                    @Override
                                                                    public void onSuccess(String fileid) {

                                                                    }
                                                                })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                    }
                                                                });
                                                    }
                                                });
                                            }

                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                        }
                                    });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                }
                            });

                        } else {

                            mDriveServiceHelper.getExistsFilesFromFolder(mimeType, filename, folder.getId()).addOnSuccessListener(new OnSuccessListener<File>() {
                                @Override
                                public void onSuccess(File file) {

                                    if (file == null) {
                                        mDriveServiceHelper.createFile(mimeType, uri, filename, folder.getId())
                                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                                    @Override
                                                    public void onSuccess(String fileid) {

                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                    }
                                                });
                                    } else {
                                        mDriveServiceHelper.deleteFile(file.getId()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                mDriveServiceHelper.createFile(mimeType, uri, filename, folder.getId())
                                                        .addOnSuccessListener(new OnSuccessListener<String>() {
                                                            @Override
                                                            public void onSuccess(String fileid) {

                                                            }
                                                        })
                                                        .addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                            }
                                                        });
                                            }
                                        });
                                    }

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                }
                            });


                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });

            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(SaveSharedPref.ERROR)
                    .setMessage(SaveSharedPref.CHECK_INTERNET_CONNECTION)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create()
                    .show();
        }


    }

    private void uploadBackupToDropbox(Uri uri) {

        java.io.File file = new java.io.File(uri.getPath());
        if (file != null) {
            //Initialize UploadTask

            new UploadTask(DropboxClient.getClient(dropboxServiceHelper.retrieveAccessToken()), file, Constants.BACKUP_FOLDER_NAME, getApplicationContext(), new UploadTask.Callback() {
                @Override
                public void onUploadComplete(FileMetadata result) {

                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            }).execute();
        }

    }

    private void updateEncryption(byte[] newKey) {
//        SecretKey encryptionKey = null;

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

    public void doAutoBackup() {
        saveFileWithPermissions(Constants.BACKUP_MIMETYPE_CRYPT, Constants.BackupType.ENCRYPTED, Constants.INTENT_BACKUP_SAVE_DOCUMENT_CRYPT, Constants.PERMISSIONS_BACKUP_WRITE_EXPORT_CRYPT);
    }

    private void saveFileWithPermissions(String mimeType, Constants.BackupType backupType, int intentId, int requestId) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            showSaveFileSelector(mimeType, backupType, intentId);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestId);
        }
    }

    private void showSaveFileSelector(String mimeType, Constants.BackupType backupType, int intentId) {

        if (Tools.mkdir(settings.getBackupDir())) {
            doBackupCrypt(Tools.buildUri(settings.getBackupDir(), Constants.AUTOBACKUP_FILE_NAME), mimeType, Constants.AUTOBACKUP_FILE_NAME);
        } else {
            Toast.makeText(this, R.string.backup_toast_mkdir_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void doBackupCrypt(final Uri uri, String mimeType, String filename) {
        String password = settings.getBackupPasswordEnc();

        if (password.isEmpty()) {
            PasswordEntryDialog pwDialog = new PasswordEntryDialog(this, PasswordEntryDialog.Mode.UPDATE, new PasswordEntryDialog.PasswordEnteredCallback() {
                @Override
                public void onPasswordEntered(String newPassword) {
                    doBackupCryptWithPassword(uri, newPassword, mimeType, filename);
                }
            });
            pwDialog.show();
        } else {
            doBackupCryptWithPassword(uri, password, mimeType, filename);
        }
    }

    private void doBackupCryptWithPassword(Uri uri, String password, String mimeType, String filename) {
        if (Tools.isExternalStorageWritable()) {
            ArrayList<Entry> entries = DatabaseHelper.loadDatabase(this, encryptionKey);
            String plain = DatabaseHelper.entriesToString(entries);

            boolean success = true;

            try {
                SecretKey key = EncryptionHelper.generateSymmetricKeyFromPassword(password);
                byte[] encrypted = EncryptionHelper.encrypt(key, plain.getBytes(StandardCharsets.UTF_8));

                FileHelper.writeBytesToFile(this, uri, encrypted);
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            }

            if (success) {

                Toast.makeText(this, R.string.backup_toast_export_success, Toast.LENGTH_SHORT).show();

                if (sp.getBoolean(SaveSharedPref.isCloudAutobackupChecked, false)) {
                    if (sp.getBoolean(SaveSharedPref.isGoogleDriveBackupChecked, false)) {
                        uploadBackupToGoogleDrive();
                    }

                    if (sp.getBoolean(SaveSharedPref.isDropboxBackupChecked, false)) {
                        uploadBackupToDropbox(uri);
                    }
                }

            } else {
                Toast.makeText(this, R.string.backup_toast_export_failed, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, R.string.backup_toast_storage_not_accessible, Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Constants.PERMISSIONS_BACKUP_WRITE_EXPORT_CRYPT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSaveFileSelector(Constants.BACKUP_MIMETYPE_CRYPT, Constants.BackupType.ENCRYPTED, Constants.INTENT_BACKUP_SAVE_DOCUMENT_CRYPT);
            } else {
                Toast.makeText(this, R.string.backup_toast_storage_permissions, Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // Options menu
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
            Intent backupIntent = new Intent(this, BackupActivity.class);
            backupIntent.putExtra(Constants.EXTRA_BACKUP_ENCRYPTION_KEY, adapter.getEncryptionKey().getEncoded());
            startActivityForResult(backupIntent, Constants.INTENT_MAIN_BACKUP);
        } else if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            if (adapter.getEncryptionKey() != null)
                settingsIntent.putExtra(Constants.EXTRA_SETTINGS_ENCRYPTION_KEY, adapter.getEncryptionKey().getEncoded());
            startActivityForResult(settingsIntent, Constants.INTENT_MAIN_SETTINGS);
        } else if (id == R.id.action_notification) {
            startActivity(new Intent(getApplicationContext(), NotificationActivity.class));
        } else if (id == R.id.action_faqs) {
            Uri uri = Uri.parse("https://www.dipauthenticator.com/FAQ.php"); // missing 'http://' will cause crashed
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
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
//        tagsDrawerListView = findViewById(R.id.tags_list_in_drawer);
//        recyclerViewNotification = findViewById(R.id.recyclerViewNotification);
//        recyclerViewNotification.setLayoutManager(new LinearLayoutManager(this));

        View navigation_drawer = findViewById(R.id.navigation_drawer);

        LinearLayout linearBackup = navigation_drawer.findViewById(R.id.linearBackup);
        LinearLayout linearSettings = navigation_drawer.findViewById(R.id.linearSettings);
        LinearLayout linearFaq = navigation_drawer.findViewById(R.id.linearFaq);
        LinearLayout linearAbout = navigation_drawer.findViewById(R.id.linearAbout);

        tagsDrawerLayout = findViewById(R.id.drawer_layout);

        tagsToggle = new ActionBarDrawerToggle(this, tagsDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
//                getSupportActionBar().setTitle(R.string.label_tags);
//                getSupportActionBar().setTitle("Notifications");
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
//                getSupportActionBar().setTitle(R.string.app_name);
                invalidateOptionsMenu();
            }
        };

        tagsToggle.setDrawerIndicatorEnabled(true);
        tagsDrawerLayout.addDrawerListener(tagsToggle);

        linearBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                openOrCloseDrawer();

                Intent backupIntent = new Intent(MainActivity.this, BackupActivity.class);
                backupIntent.putExtra(Constants.EXTRA_BACKUP_ENCRYPTION_KEY, adapter.getEncryptionKey().getEncoded());
                startActivityForResult(backupIntent, Constants.INTENT_MAIN_BACKUP);

            }
        });

        linearSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                openOrCloseDrawer();


                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                if (adapter.getEncryptionKey() != null)
                    settingsIntent.putExtra(Constants.EXTRA_SETTINGS_ENCRYPTION_KEY, adapter.getEncryptionKey().getEncoded());
                startActivityForResult(settingsIntent, Constants.INTENT_MAIN_SETTINGS);

            }
        });

        linearFaq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                openOrCloseDrawer();

                Uri uri = Uri.parse("https://www.dipauthenticator.com/FAQ.php"); // missing 'http://' will cause crashed
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);

            }
        });

        linearAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                openOrCloseDrawer();

                Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(aboutIntent);

            }
        });

        getNotifications();

    }

    private void openOrCloseDrawer() {

        if (tagsDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            tagsDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            tagsDrawerLayout.openDrawer(GravityCompat.START);
        }
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

    public static class BackupPasswordFragment extends PreferenceFragment {

        Settings settings;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.backuppassword_preferences);
        }
    }

}