package org.shadowice.flocke.andotp.Activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.files.FileMetadata;
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

import org.shadowice.flocke.andotp.Common.Utilities.NetworkConnectivity;
import org.shadowice.flocke.andotp.Database.Entry;
import org.shadowice.flocke.andotp.Dialogs.PasswordEntryDialog;
import org.shadowice.flocke.andotp.Helpers.DriveServiceHelper;
import org.shadowice.flocke.andotp.Helpers.DropboxServices.DropboxClient;
import org.shadowice.flocke.andotp.Helpers.DropboxServices.DropboxServiceHelper;
import org.shadowice.flocke.andotp.Helpers.DropboxServices.UploadTask;
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

import javax.crypto.SecretKey;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class CloudBackupActivity extends BaseActivity implements View.OnClickListener {

    private static final int REQUEST_CODE_GOOGLE_SIGN_IN = 992;

    private DropboxServiceHelper dropboxServiceHelper;

    private String TAG = this.getClass().getSimpleName();
    private Context context = this;
    private SecretKey encryptionKey = null;

    private ImageView imgBack, imgGoogleDrive, imgDropbox;
    private DriveServiceHelper mDriveServiceHelper;
    private boolean reload = false;

    private CheckBox googleCheckBox, dropboxCheckBox;
    private SharedPreferences sp;

    private String from = "";

    private SaveSharedPref.BackupMode backupMode = SaveSharedPref.BackupMode.GOOGLE;
    private boolean isDropboxClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_backup);

        sp = getSharedPreferences(SaveSharedPref.PREF, Context.MODE_PRIVATE);

        Intent callingIntent = getIntent();
        byte[] keyMaterial = callingIntent.getByteArrayExtra(Constants.EXTRA_BACKUP_ENCRYPTION_KEY);
        from = callingIntent.getStringExtra("from");
        encryptionKey = EncryptionHelper.generateSymmetricKey(keyMaterial);

        dropboxServiceHelper = new DropboxServiceHelper(this);

        imgBack = findViewById(R.id.imgBack);
        imgGoogleDrive = findViewById(R.id.imgGoogleDrive);
        imgDropbox = findViewById(R.id.imgDropbox);

        googleCheckBox = findViewById(R.id.googleCheckBox);
        dropboxCheckBox = findViewById(R.id.dropboxCheckBox);

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
            if (NetworkConnectivity.isConnected()) {
                backupMode = SaveSharedPref.BackupMode.GOOGLE;
                requestGoogleSignIn();
            } else {
                showConnectivityErrorMessage();
            }
        } else if (id == R.id.imgDropbox) {
            if (NetworkConnectivity.isConnected()) {
                backupMode = SaveSharedPref.BackupMode.DROPBOX;
                isDropboxClicked = true;
                dropboxServiceHelper.getAccessToken();
                if (dropboxServiceHelper.tokenExists()) {
                    dropboxServiceHelper.getDropboxUserAccount();
                    doBackup();
                } else {
                    Auth.startOAuth2Authentication(getApplicationContext(), SaveSharedPref.DROPBOX_APP_KEY);
                }
            } else {
                showConnectivityErrorMessage();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_CODE_GOOGLE_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                handleGoogleSignInResult(intent);
            }
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

    private void requestGoogleSignIn() {
        Log.d(TAG, "Requesting sign-in");

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_GOOGLE_SIGN_IN);
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

                    if (from.equals("manualbackup")) {
                        doBackup();
                    } else {
                        reload = true;
                        finishWithResult();
                    }


                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Unable to sign in " + e);
                    }
                });
    }

    public void doBackup() {
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
            doBackupCrypt(Tools.buildUri(settings.getBackupDir(), FileHelper.backupFilename(this, Constants.BackupType.ENCRYPTED)), mimeType, FileHelper.backupFilename(this, Constants.BackupType.ENCRYPTED));
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

                if (NetworkConnectivity.isConnected()) {

                    if (backupMode == SaveSharedPref.BackupMode.GOOGLE) {
                        doBackupToGoogleDrive(mimeType, filename, uri);
                    } else if (backupMode == SaveSharedPref.BackupMode.DROPBOX) {
                        doBackupToDropbox(uri);
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

            } else {
                reload = false;
                Toast.makeText(context, R.string.cloud_backup_toast_export_failed, Toast.LENGTH_LONG).show();
                finishWithResult();
            }
        } else {
            reload = false;
            Toast.makeText(context, R.string.cloud_backup_toast_export_failed, Toast.LENGTH_LONG).show();
            finishWithResult();
        }

    }

    private void doBackupToGoogleDrive(String mimeType, String filename, Uri uri) {

        if (mDriveServiceHelper != null) {

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

                                                            reload = true;
                                                            Toast.makeText(context, R.string.cloud_backup_toast_export_success, Toast.LENGTH_LONG).show();
                                                            finishWithResult();
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            reload = false;
                                                            Toast.makeText(context, R.string.cloud_backup_toast_export_failed, Toast.LENGTH_LONG).show();
                                                            finishWithResult();
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
                                                                    reload = true;
                                                                    Toast.makeText(context, R.string.cloud_backup_toast_export_success, Toast.LENGTH_LONG).show();
                                                                    finishWithResult();
                                                                }
                                                            })
                                                            .addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    reload = false;
                                                                    Toast.makeText(context, R.string.cloud_backup_toast_export_failed, Toast.LENGTH_LONG).show();
                                                                    finishWithResult();
                                                                }
                                                            });
                                                }
                                            });
                                        }

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        reload = false;
                                        Toast.makeText(context, R.string.cloud_backup_toast_export_failed, Toast.LENGTH_LONG).show();
                                        finishWithResult();
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                reload = false;
                                Toast.makeText(context, R.string.cloud_backup_toast_export_failed, Toast.LENGTH_LONG).show();
                                finishWithResult();
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
                                                    reload = true;
                                                    Toast.makeText(context, R.string.cloud_backup_toast_export_success, Toast.LENGTH_LONG).show();
                                                    finishWithResult();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    reload = false;
                                                    Toast.makeText(context, R.string.cloud_backup_toast_export_failed, Toast.LENGTH_LONG).show();
                                                    finishWithResult();
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
                                                            reload = true;
                                                            Toast.makeText(context, R.string.cloud_backup_toast_export_success, Toast.LENGTH_LONG).show();
                                                            finishWithResult();

                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {

                                                            reload = false;
                                                            Toast.makeText(context, R.string.cloud_backup_toast_export_failed, Toast.LENGTH_LONG).show();
                                                            finishWithResult();

                                                        }
                                                    });
                                        }
                                    });
                                }

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                reload = false;
                                Toast.makeText(context, R.string.cloud_backup_toast_export_failed, Toast.LENGTH_LONG).show();
                                finishWithResult();
                            }
                        });


                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    reload = false;
                    Toast.makeText(context, R.string.cloud_backup_toast_export_failed, Toast.LENGTH_LONG).show();
                    finishWithResult();
                }
            });

        }
    }

    private void doBackupToDropbox(Uri uri) {

        java.io.File file = new java.io.File(uri.getPath());
        if (file != null) {
            //Initialize UploadTask
//                            new UploadTask(DropboxClient.getClient(dropboxServiceHelper.retrieveAccessToken()), file ,Constants.BACKUP_FOLDER_NAME, getApplicationContext()).execute();

            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(false);
            dialog.setMessage("Uploading");
            dialog.show();

            new UploadTask(DropboxClient.getClient(dropboxServiceHelper.retrieveAccessToken()), file, Constants.BACKUP_FOLDER_NAME, getApplicationContext(), new UploadTask.Callback() {
                @Override
                public void onUploadComplete(FileMetadata result) {

                    dialog.dismiss();

                    reload = true;
                    Toast.makeText(context, R.string.cloud_backup_toast_export_success, Toast.LENGTH_LONG).show();
                    finishWithResult();

                }

                @Override
                public void onError(Exception e) {

                    dialog.dismiss();

                    reload = false;
                    Toast.makeText(context, R.string.cloud_backup_toast_export_failed, Toast.LENGTH_LONG).show();
                    finishWithResult();

                }
            }).execute();
        }
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

    // End with a result
    public void finishWithResult() {

        if (reload) {

 /*           if (backupMode == SaveSharedPref.BackupMode.GOOGLE) {
                sp.edit().putBoolean(SaveSharedPref.isGoogleDriveBackupChecked, true).apply();
                googleCheckBox.setChecked(true);

            } else if (backupMode == SaveSharedPref.BackupMode.DROPBOX) {
                sp.edit().putBoolean(SaveSharedPref.isDropboxBackupChecked, true).apply();
                dropboxCheckBox.setChecked(true);

            }*/

        }

        Intent data = new Intent();
        data.putExtra("reload", reload);
        setResult(RESULT_OK, data);
        finish();
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
                    doBackup();

                }
            }
        }


    }
}
