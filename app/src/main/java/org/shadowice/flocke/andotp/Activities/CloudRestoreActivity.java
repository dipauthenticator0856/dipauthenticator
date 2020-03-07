package org.shadowice.flocke.andotp.Activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.gson.Gson;

import org.shadowice.flocke.andotp.Common.Utilities.NetworkConnectivity;
import org.shadowice.flocke.andotp.Database.Entry;
import org.shadowice.flocke.andotp.Dialogs.PasswordEntryDialog;
import org.shadowice.flocke.andotp.Helpers.DriveServiceHelper;
import org.shadowice.flocke.andotp.Helpers.DropboxServices.DownloadFileTask;
import org.shadowice.flocke.andotp.Helpers.DropboxServices.DropboxClient;
import org.shadowice.flocke.andotp.Helpers.DropboxServices.DropboxServiceHelper;
import org.shadowice.flocke.andotp.Helpers.DropboxServices.FilesAdapter;
import org.shadowice.flocke.andotp.Helpers.DropboxServices.ListFolderTask;
import org.shadowice.flocke.andotp.Helpers.GoogleDriveFileHolder;
import org.shadowice.flocke.andotp.R;
import org.shadowice.flocke.andotp.Utilities.Constants;
import org.shadowice.flocke.andotp.Utilities.DatabaseHelper;
import org.shadowice.flocke.andotp.Utilities.EncryptionHelper;
import org.shadowice.flocke.andotp.Utilities.FileHelper;
import org.shadowice.flocke.andotp.Utilities.SaveSharedPref;
import org.shadowice.flocke.andotp.Utilities.Tools;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.SecretKey;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CloudRestoreActivity extends BaseActivity implements View.OnClickListener {

    public final static String EXTRA_PATH = "FilesActivity_Path";
    private static final int REQUEST_CODE_SIGN_IN = 992;
    String decryptedString = "";
    private SaveSharedPref.BackupMode backupMode = SaveSharedPref.BackupMode.GOOGLE;
    private DropboxServiceHelper dropboxServiceHelper;
    private String TAG = this.getClass().getSimpleName();
    private ImageView imgBack, imgGoogleDrive, imgDropbox;
    private DriveServiceHelper mDriveServiceHelper;
    private SecretKey encryptionKey = null;
    private boolean reload = false;
    //    private ProgressDialog pDialog = null;
    private Dialog dialog;
    private CheckBox googleCheckBox, dropboxCheckBox;
    private SharedPreferences sp;
    private FilesAdapter mFilesAdapter;

    private boolean isDropboxClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_restore);

        sp = getSharedPreferences(SaveSharedPref.PREF, Context.MODE_PRIVATE);

        imgBack = findViewById(R.id.imgBack);
        imgGoogleDrive = findViewById(R.id.imgGoogleDrive);
        imgDropbox = findViewById(R.id.imgDropbox);

        googleCheckBox = findViewById(R.id.googleCheckBox);
        dropboxCheckBox = findViewById(R.id.dropboxCheckBox);

        dropboxServiceHelper = new DropboxServiceHelper(this);

        Intent callingIntent = getIntent();
        byte[] keyMaterial = callingIntent.getByteArrayExtra(Constants.EXTRA_BACKUP_ENCRYPTION_KEY);
        encryptionKey = EncryptionHelper.generateSymmetricKey(keyMaterial);

        if (sp.getBoolean(SaveSharedPref.isGoogleDriveBackupChecked, false)) {
            googleCheckBox.setChecked(true);
        }

        if (sp.getBoolean(SaveSharedPref.isDropboxBackupChecked, false)) {
            dropboxCheckBox.setChecked(true);
        }

        imgBack.setOnClickListener(this);
        imgGoogleDrive.setOnClickListener(this);
        imgDropbox.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {

        int id = view.getId();

        if (id == R.id.imgBack) {
            onBackPressed();
        } else if (id == R.id.imgGoogleDrive) {
            backupMode = SaveSharedPref.BackupMode.GOOGLE;
            if (NetworkConnectivity.isConnected()) {
                requestGoogleSignIn();
            } else {
                showConnectivityErrorMessage();
            }
        } else if (id == R.id.imgDropbox) {
            backupMode = SaveSharedPref.BackupMode.DROPBOX;
            isDropboxClicked = true;
            dropboxServiceHelper.getAccessToken();
            if (dropboxServiceHelper.tokenExists()) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    dropboxServiceHelper.getDropboxUserAccount();
                    dropboxFilesDialog();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.PERMISSIONS_BACKUP_WRITE_EXPORT_CRYPT);
                }
            } else {
                Auth.startOAuth2Authentication(getApplicationContext(), SaveSharedPref.DROPBOX_APP_KEY);
            }
        }
    }

    private void requestGoogleSignIn() {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                handleGoogleSignInResult(data);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Constants.PERMISSIONS_BACKUP_WRITE_EXPORT_CRYPT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dropboxServiceHelper.getDropboxUserAccount();
                dropboxFilesDialog();
            } else {
                Toast.makeText(this, R.string.backup_toast_storage_permissions, Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void handleGoogleSignInResult(Intent result) {
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

                    mDriveServiceHelper = new DriveServiceHelper(googleDriveService);

                    sp.edit().putBoolean(SaveSharedPref.isGoogleDriveBackupChecked, true).apply();
                    googleCheckBox.setChecked(true);

                    downloadFile();
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    private void downloadFile() {

        if (NetworkConnectivity.isConnected()) {

            ProgressDialog pDialog = new ProgressDialog(this);
            pDialog.setMessage("Getting files...");
            pDialog.setCancelable(false);
            pDialog.show();

            if (mDriveServiceHelper != null) {

                String mimeType = Constants.BACKUP_MIMETYPE_CRYPT;
                String filename = Constants.AUTOBACKUP_FILE_NAME;

                mDriveServiceHelper.getExistsFolder(Constants.BACKUP_FOLDER_NAME, "root").addOnSuccessListener(new OnSuccessListener<File>() {
                    @Override
                    public void onSuccess(File folder) {

                        if (folder != null) {

                            Log.e("getName", " " + folder.getName());
                            mDriveServiceHelper.queryFiles(folder.getId(), mimeType)
                                    .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
                                        @Override
                                        public void onSuccess(List<GoogleDriveFileHolder> googleDriveFileHolders) {
                                            Gson gson = new Gson();
                                            Log.d(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolders));

                                            if (pDialog.isShowing()) {
                                                pDialog.dismiss();
                                            }

                                            googleDriveFilesDialog(googleDriveFileHolders, folder.getId());
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d(TAG, "onFailure: " + e);
                                            e.printStackTrace();

                                            if (pDialog.isShowing()) {
                                                pDialog.dismiss();
                                            }
                                        }
                                    });
                        } else {
                            Toast.makeText(CloudRestoreActivity.this, "No Backup Found!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (pDialog.isShowing()) {
                            pDialog.dismiss();
                        }
                        Toast.makeText(CloudRestoreActivity.this, "No Backup Found!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } else {
            showConnectivityErrorMessage();
        }

    }

    private void doDropboxRestoreCryptWithPassword(String password, java.io.File file) {

        ProgressDialog pDialog = new ProgressDialog(this);
        pDialog.setMessage("Restoring your Data..");
        pDialog.setCancelable(false);
        pDialog.show();

        if (Tools.isExternalStorageReadable()) {
            boolean success = true;
            String decryptedString = "";

            try {
                byte[] encrypted = FileHelper.readFileToBytes(this, Uri.fromFile(file));

                SecretKey key = EncryptionHelper.generateSymmetricKeyFromPassword(password);
                byte[] decrypted = EncryptionHelper.decrypt(key, encrypted);

                decryptedString = new String(decrypted, StandardCharsets.UTF_8);
            } catch (Exception e) {
                success = false;
                e.printStackTrace();
            }

            if (success) {
                if (pDialog.isShowing()) {
                    pDialog.dismiss();
                }
                restoreEntries(decryptedString);
            } else {
                if (pDialog.isShowing()) {
                    pDialog.dismiss();
                }
                Toast.makeText(this, R.string.backup_toast_import_decryption_failed, Toast.LENGTH_LONG).show();
            }
        } else {
            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
            Toast.makeText(this, R.string.backup_toast_storage_not_accessible, Toast.LENGTH_LONG).show();
        }
    }

    private void doGoogleRestoreCryptWithPassword(String password, String fileid) {
        boolean success = true;

        ProgressDialog pDialog = new ProgressDialog(this);
        pDialog.setMessage("Restoring your Data..");
        pDialog.setCancelable(false);
        pDialog.show();

        try {

            mDriveServiceHelper.readBytesFromGoogleDrive(fileid).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    SecretKey key = null;
                    try {
                        if (pDialog.isShowing()) {
                            pDialog.dismiss();
                        }
                        key = EncryptionHelper.generateSymmetricKeyFromPassword(password);

                        byte[] decrypted = EncryptionHelper.decrypt(key, bytes);

                        decryptedString = new String(decrypted, StandardCharsets.UTF_8);

                        restoreEntries(new String(decrypted, StandardCharsets.UTF_8));

                    } catch (Exception e) {
                        e.printStackTrace();
                        if (pDialog.isShowing()) {
                            pDialog.dismiss();
                        }
                        Toast.makeText(CloudRestoreActivity.this, R.string.backup_toast_import_decryption_failed, Toast.LENGTH_LONG).show();
                    }

                }
            });

        } catch (Exception e) {
            success = false;
            e.printStackTrace();

            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
            Toast.makeText(this, R.string.backup_toast_import_decryption_failed, Toast.LENGTH_LONG).show();
        }

      /*  if (success) {
            restoreEntries(decryptedString);
        } else {
            Toast.makeText(this, R.string.backup_toast_import_decryption_failed, Toast.LENGTH_LONG).show();
        }*/

    }

    private void restoreEntries(String text) {
        ArrayList<Entry> entries = DatabaseHelper.stringToEntries(text);

        if (entries.size() > 0) {

            ArrayList<Entry> currentEntries = DatabaseHelper.loadDatabase(this, encryptionKey);

            entries.removeAll(currentEntries);
            entries.addAll(currentEntries);

            if (DatabaseHelper.saveDatabase(this, entries, encryptionKey)) {
                reload = true;
                Toast.makeText(this, R.string.cloud_toast_import_success, Toast.LENGTH_LONG).show();
                finishWithResult();
            } else {
                Toast.makeText(this, R.string.backup_toast_import_save_failed, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, R.string.backup_toast_import_no_entries, Toast.LENGTH_LONG).show();
        }
    }

    // End with a result
    public void finishWithResult() {

   /*     if (reload) {

            if (backupMode == SaveSharedPref.BackupMode.GOOGLE) {
                sp.edit().putBoolean(SaveSharedPref.isGoogleDriveBackupChecked, true).apply();
                googleCheckBox.setChecked(true);

            } else if (backupMode == SaveSharedPref.BackupMode.DROPBOX) {
                sp.edit().putBoolean(SaveSharedPref.isDropboxBackupChecked, true).apply();
                dropboxCheckBox.setChecked(true);

            }

        }*/

        Intent data = new Intent();
        data.putExtra("reload", reload);
        setResult(RESULT_OK, data);
        finish();
    }

    private void googleDriveFilesDialog(List<GoogleDriveFileHolder> list, String folderid) {

        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_googlefiles);

        ProgressBar progressBar = dialog.findViewById(R.id.progressBar);

        RecyclerView recyclerView = dialog.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new DriverFilesAdapter(list, folderid));

        progressBar.setVisibility(View.GONE);

        ImageView imgBack = dialog.findViewById(R.id.imgBack);
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = (int) ((int) displaymetrics.widthPixels * 0.9);
        int height = (int) ((int) displaymetrics.heightPixels * 0.5);

        dialog.getWindow().setLayout(width, height);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));

        dialog.show();

    }

    private void dropboxFilesDialog() {

        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_googlefiles);

//        RelativeLayout relativeMain = dialog.findViewById(R.id.relativeMain);
//        LinearLayout linearSelectBackup = dialog.findViewById(R.id.linearSelectBackup);
//        RadioButton rbtnAutobackup = dialog.findViewById(R.id.rbtnAutobackup);
//        RadioButton rbtnManualbackup = dialog.findViewById(R.id.rbtnManualbackup);
//        Button btnContinue = dialog.findViewById(R.id.btnContinue);

        ProgressBar progressBar = dialog.findViewById(R.id.progressBar);

        RecyclerView recyclerView = dialog.findViewById(R.id.recyclerView);

        mFilesAdapter = new FilesAdapter(new FilesAdapter.Callback() {
            @Override
            public void onFolderClicked(FolderMetadata folder) {

            }

            @Override
            public void onFileClicked(final FileMetadata file) {

                if (file != null) {
                    downloadDropboxFile(file);
                }

            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mFilesAdapter);

        new ListFolderTask(DropboxClient.getClient(dropboxServiceHelper.retrieveAccessToken()), new ListFolderTask.Callback() {
            @Override
            public void onDataLoaded(ListFolderResult result) {
                progressBar.setVisibility(View.GONE);
                mFilesAdapter.setFiles(result.getEntries());
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                e.printStackTrace();
            }
        }).execute("/" + Constants.BACKUP_FOLDER_NAME);

        ImageView imgBack = dialog.findViewById(R.id.imgBack);
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

       /* rbtnManualbackup.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                rbtnAutobackup.setChecked(false);
            }
        });

        rbtnAutobackup.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                rbtnManualbackup.setChecked(false);
            }
        });

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                linearSelectBackup.setVisibility(View.GONE);
                relativeMain.setVisibility(View.VISIBLE);
            }
        });*/

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = (int) ((int) displaymetrics.widthPixels * 0.9);
        int height = (int) ((int) displaymetrics.heightPixels * 0.5);

        dialog.getWindow().setLayout(width, height);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));

        dialog.show();

    }

    private void showConnectivityErrorMessage() {
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

    private void downloadDropboxFile(FileMetadata file) {

        ProgressDialog pDialog = new ProgressDialog(this);
        pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pDialog.setCancelable(false);
        pDialog.setMessage("Downloading");
        pDialog.show();

        new DownloadFileTask(this, DropboxClient.getClient(dropboxServiceHelper.retrieveAccessToken()), new DownloadFileTask.Callback() {
            @Override
            public void onDownloadComplete(java.io.File result) {
                pDialog.dismiss();

                if (result != null) {

                    PasswordEntryDialog pwDialog = new PasswordEntryDialog(CloudRestoreActivity.this, PasswordEntryDialog.Mode.ENTER, new PasswordEntryDialog.PasswordEnteredCallback() {
                        @Override
                        public void onPasswordEntered(String newPassword) {
                            doDropboxRestoreCryptWithPassword(newPassword, result);
                        }
                    });
                    pwDialog.show();

                }
            }

            @Override
            public void onError(Exception e) {
                pDialog.dismiss();

                Log.e(TAG, "Failed to download file.", e);

            }
        }).execute(file);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (isDropboxClicked) {
            isDropboxClicked = false;
            backupMode = SaveSharedPref.BackupMode.DROPBOX;
            if (dropboxServiceHelper != null) {
                dropboxServiceHelper.getAccessToken();

                if (dropboxServiceHelper.tokenExists()) {
                    sp.edit().putBoolean(SaveSharedPref.isDropboxBackupChecked, true).apply();
                    dropboxCheckBox.setChecked(true);
                    dropboxServiceHelper.getDropboxUserAccount();

                }
            }
        }
    }

    private class DriverFilesAdapter extends RecyclerView.Adapter<DriverFilesAdapter.MyViewHolder> {

        private List<GoogleDriveFileHolder> files;
        private String folderId;

        DriverFilesAdapter(List<GoogleDriveFileHolder> files, String folderid) {
            this.files = files;
            this.folderId = folderid;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_drivefiles, parent, false);
            return new DriverFilesAdapter.MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

            holder.txtFileName.setText(files.get(position).getName());

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (dialog != null) {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    }

                    mDriveServiceHelper.getExistsFilesFromFolder(Constants.BACKUP_MIMETYPE_CRYPT, files.get(position).getName(), folderId).addOnSuccessListener(new OnSuccessListener<File>() {
                        @Override
                        public void onSuccess(File file) {

                            if (file != null) {

                                PasswordEntryDialog pwDialog = new PasswordEntryDialog(CloudRestoreActivity.this, PasswordEntryDialog.Mode.ENTER, new PasswordEntryDialog.PasswordEnteredCallback() {
                                    @Override
                                    public void onPasswordEntered(String newPassword) {
                                        doGoogleRestoreCryptWithPassword(newPassword, file.getId());
                                    }
                                });
                                pwDialog.show();

                            } else {
                                Toast.makeText(CloudRestoreActivity.this, "No Backup Found!", Toast.LENGTH_SHORT).show();
                            }

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(CloudRestoreActivity.this, "No Backup Found!", Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            });
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {

            private TextView txtFileName;

            MyViewHolder(@NonNull View itemView) {
                super(itemView);

                txtFileName = itemView.findViewById(R.id.txtFileName);
            }
        }
    }

}