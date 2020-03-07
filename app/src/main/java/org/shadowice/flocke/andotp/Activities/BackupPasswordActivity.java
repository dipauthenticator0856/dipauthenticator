package org.shadowice.flocke.andotp.Activities;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.shadowice.flocke.andotp.R;
import org.shadowice.flocke.andotp.Utilities.Settings;

public class BackupPasswordActivity extends BaseActivity {

    ViewStub stub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backuppassword);

        stub = findViewById(R.id.container_stub);
        stub.inflate();

        ImageView imgBack = findViewById(R.id.imgBack);
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        getFragmentManager().beginTransaction()
                .replace(R.id.container_content, new BackupPasswordFragment())
                .commit();
    }

    public static class BackupPasswordFragment extends PreferenceFragment {

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.backuppassword_preferences);
        }
    }

}
