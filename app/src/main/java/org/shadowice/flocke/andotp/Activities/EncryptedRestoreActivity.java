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

public class EncryptedRestoreActivity extends BaseActivity {

    private SecretKey encryptionKey = null;
    private boolean reload = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encrypted_restore);

        ImageView imgBack = findViewById(R.id.imgBack);
        LinearLayout restoreCrypt = findViewById(R.id.button_restore_crypt);
        LinearLayout cloudRestore = findViewById(R.id.button_cloud_restore);

        Intent callingIntent = getIntent();
        byte[] keyMaterial = callingIntent.getByteArrayExtra(Constants.EXTRA_BACKUP_ENCRYPTION_KEY);
        encryptionKey = EncryptionHelper.generateSymmetricKey(keyMaterial);

        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        cloudRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cloudRestoreIntent = new Intent(EncryptedRestoreActivity.this, CloudRestoreActivity.class);
                cloudRestoreIntent.putExtra(Constants.EXTRA_BACKUP_ENCRYPTION_KEY, keyMaterial);
                startActivityForResult(cloudRestoreIntent, Constants.INTENT_BACKUP_OPEN_CLOUD_CRYPT);
            }
        });

        restoreCrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFileWithPermissions(Constants.INTENT_BACKUP_OPEN_DOCUMENT_CRYPT, Constants.PERMISSIONS_BACKUP_READ_IMPORT_CRYPT);
            }
        });
    }

    private void openFileWithPermissions(int intentId, int requestId) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            showOpenFileSelector(intentId);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, requestId);
        }
    }

    private void showOpenFileSelector(int intentId) {
        if (settings.getBackupAsk() || settings.getIsAppendingDateTimeToBackups()) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, intentId);
        } else {
            if (intentId == Constants.INTENT_BACKUP_OPEN_DOCUMENT_CRYPT)
                doRestoreCrypt(Tools.buildUri(settings.getBackupDir(), Constants.BACKUP_FILENAME_CRYPT));

        }
    }

    private void doRestoreCrypt(final Uri uri) {
        String password = settings.getBackupPasswordEnc();

        PasswordEntryDialog pwDialog = new PasswordEntryDialog(this, PasswordEntryDialog.Mode.ENTER, new PasswordEntryDialog.PasswordEnteredCallback() {
            @Override
            public void onPasswordEntered(String newPassword) {
                doRestoreCryptWithPassword(uri, newPassword);
            }
        });
        pwDialog.show();

    }

    private void doRestoreCryptWithPassword(Uri uri, String password) {
        if (Tools.isExternalStorageReadable()) {
            boolean success = true;
            String decryptedString = "";

            try {
                byte[] encrypted = FileHelper.readFileToBytes(this, uri);

                SecretKey key = EncryptionHelper.generateSymmetricKeyFromPassword(password);
                byte[] decrypted = EncryptionHelper.decrypt(key, encrypted);

                decryptedString = new String(decrypted, StandardCharsets.UTF_8);
            } catch (Exception e) {
                success = false;
                e.printStackTrace();
            }

            if (success) {
                restoreEntries(decryptedString);
            } else {
                Toast.makeText(this, R.string.backup_toast_import_decryption_failed, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, R.string.backup_toast_storage_not_accessible, Toast.LENGTH_LONG).show();
        }
    }

    private void restoreEntries(String text) {
        ArrayList<Entry> entries = DatabaseHelper.stringToEntries(text);

        if (entries.size() > 0) {

            ArrayList<Entry> currentEntries = DatabaseHelper.loadDatabase(this, encryptionKey);

            entries.removeAll(currentEntries);
            entries.addAll(currentEntries);

            if (DatabaseHelper.saveDatabase(this, entries, encryptionKey)) {
                reload = true;
                Toast.makeText(this, R.string.backup_toast_import_success, Toast.LENGTH_LONG).show();
                finishWithResult();
            } else {
                Toast.makeText(this, R.string.backup_toast_import_save_failed, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, R.string.backup_toast_import_no_entries, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
       if (requestCode == Constants.PERMISSIONS_BACKUP_READ_IMPORT_CRYPT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showOpenFileSelector(Constants.INTENT_BACKUP_OPEN_DOCUMENT_CRYPT);
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

       if (requestCode == Constants.INTENT_BACKUP_OPEN_DOCUMENT_CRYPT && resultCode == RESULT_OK) {
            if (intent != null) {
                doRestoreCrypt(intent.getData());
            }
        } else if (requestCode == Constants.INTENT_BACKUP_OPEN_CLOUD_CRYPT) {
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
