package org.shadowice.flocke.andotp.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.shadowice.flocke.andotp.Database.Entry;
import org.shadowice.flocke.andotp.Dialogs.PasswordEntryDialog;
import org.shadowice.flocke.andotp.R;
import org.shadowice.flocke.andotp.Utilities.Constants;
import org.shadowice.flocke.andotp.Utilities.DatabaseHelper;
import org.shadowice.flocke.andotp.Utilities.EncryptionHelper;
import org.shadowice.flocke.andotp.Utilities.FileHelper;
import org.shadowice.flocke.andotp.Utilities.Tools;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.crypto.SecretKey;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class EncryptedBackupActivity extends BaseActivity {

    private SecretKey encryptionKey = null;

    private boolean reload = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encrypted_backup);

        Intent callingIntent = getIntent();
        byte[] keyMaterial = callingIntent.getByteArrayExtra(Constants.EXTRA_BACKUP_ENCRYPTION_KEY);
        encryptionKey = EncryptionHelper.generateSymmetricKey(keyMaterial);

        ImageView imgBack = findViewById(R.id.imgBack);

        LinearLayout backupCrypt = findViewById(R.id.button_backup_crypt);
        LinearLayout cloudBackup = findViewById(R.id.button_cloud_backup);

        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        backupCrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveFileWithPermissions(Constants.BACKUP_MIMETYPE_CRYPT, Constants.BackupType.ENCRYPTED, Constants.INTENT_BACKUP_SAVE_DOCUMENT_CRYPT, Constants.PERMISSIONS_BACKUP_WRITE_EXPORT_CRYPT);
            }
        });

        cloudBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cloudBackupIntent = new Intent(EncryptedBackupActivity.this, CloudBackupActivity.class);
                cloudBackupIntent.putExtra(Constants.EXTRA_BACKUP_ENCRYPTION_KEY, keyMaterial);
                cloudBackupIntent.putExtra("from", "manualbackup");
                startActivityForResult(cloudBackupIntent, Constants.INTENT_BACKUP_SAVE_CLOUD_CRYPT);
            }
        });
    }

    private void saveFileWithPermissions(String mimeType, Constants.BackupType backupType, int intentId, int requestId) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            showSaveFileSelector(mimeType, backupType, intentId);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestId);
        }
    }

    private void showSaveFileSelector(String mimeType, Constants.BackupType backupType, int intentId) {
        if (settings.getBackupAsk()) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_TITLE, FileHelper.backupFilename(this, backupType));
            startActivityForResult(intent, intentId);
        } else {
            if (Tools.mkdir(settings.getBackupDir())) {
                if (intentId == Constants.INTENT_BACKUP_SAVE_DOCUMENT_CRYPT)
                    doBackupCrypt(Tools.buildUri(settings.getBackupDir(), FileHelper.backupFilename(this, Constants.BackupType.ENCRYPTED)));
            } else {
                Toast.makeText(this, R.string.backup_toast_mkdir_failed, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void doBackupCrypt(final Uri uri) {
        String password = settings.getBackupPasswordEnc();

        if (password.isEmpty()) {
            PasswordEntryDialog pwDialog = new PasswordEntryDialog(this, PasswordEntryDialog.Mode.UPDATE, new PasswordEntryDialog.PasswordEnteredCallback() {
                @Override
                public void onPasswordEntered(String newPassword) {
                    doBackupCryptWithPassword(uri, newPassword);
                }
            });
            pwDialog.show();
        } else {
            doBackupCryptWithPassword(uri, password);
        }
    }

    private void doBackupCryptWithPassword(Uri uri, String password) {
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
                Toast.makeText(this, R.string.backup_toast_export_success, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.backup_toast_export_failed, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, R.string.backup_toast_storage_not_accessible, Toast.LENGTH_LONG).show();
        }

        finishWithResult();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == Constants.INTENT_BACKUP_SAVE_DOCUMENT_CRYPT && resultCode == RESULT_OK) {
            if (intent != null) {
                doBackupCrypt(intent.getData());
            }
        } else if (requestCode == Constants.INTENT_BACKUP_SAVE_CLOUD_CRYPT) {
            if (intent != null) {
                reload = intent.getBooleanExtra("reload", false);
                finishWithResult();
            }
        }

    }

    // End with a result
    public void finishWithResult() {
        Intent data = new Intent();
        data.putExtra("reload", reload);
        setResult(RESULT_OK, data);
        finish();
    }

}
