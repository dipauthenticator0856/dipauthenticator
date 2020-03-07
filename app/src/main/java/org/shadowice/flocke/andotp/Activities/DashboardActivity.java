package org.shadowice.flocke.andotp.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;
import org.shadowice.flocke.andotp.Common.Network.Rest.RestClient;
import org.shadowice.flocke.andotp.Common.Utilities.NetworkConnectivity;
import org.shadowice.flocke.andotp.R;
import org.shadowice.flocke.andotp.Utilities.SaveSharedPref;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity implements View.OnClickListener {

    SharedPreferences sp;
    private MediaPlayer mp;
    private String title = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sp = getSharedPreferences(SaveSharedPref.PREF, Context.MODE_PRIVATE);
        mp = MediaPlayer.create(this, R.raw.click_sound);

        Button btnGoogleAuth = findViewById(R.id.btnGoogleAuth);
        Button btnDipAuth = findViewById(R.id.btnDipAuth);
        LinearLayout linearGoogleAuth = findViewById(R.id.linearGoogleAuth);
        LinearLayout linearDipAuth = findViewById(R.id.linearDipAuth);
        TextView txtGoogleAuth = findViewById(R.id.txtGoogleAuth);
        TextView txtDipAuth = findViewById(R.id.txtDipAuth);
        TextView txtComingsoon = findViewById(R.id.txtComingsoon);
        txtComingsoon.setPaintFlags(txtComingsoon.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        btnGoogleAuth.setOnClickListener(this);
        btnDipAuth.setOnClickListener(this);
        linearGoogleAuth.setOnClickListener(this);
        linearDipAuth.setOnClickListener(this);
        txtGoogleAuth.setOnClickListener(this);
        txtDipAuth.setOnClickListener(this);

        if (sp.getString(SaveSharedPref.REGID, "") == null) {
            displayFirebaseRegId();
        }

        if (!sp.getBoolean(SaveSharedPref.isSeedkeyTaken, false)) {

            if (NetworkConnectivity.isConnected()) {

                Call<ResponseBody> calling = RestClient.getEcombidService().create(RestClient.ServiceInterface.class).generateSeedkey();

                calling.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        try {

                            JSONObject jsonObject = new JSONObject(response.body().string());

                            if (jsonObject.getString("success").equals("1")) {

                                sp.edit().putString(SaveSharedPref.universalseedkey, jsonObject.getJSONObject("posts").getString("universalseedkey")).apply();
                                sp.edit().putString(SaveSharedPref.dipseedkey, jsonObject.getJSONObject("posts").getString("dipseedkey")).apply();
                                sp.edit().putBoolean(SaveSharedPref.isSeedkeyTaken, true).apply();
                            }
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        System.out.println("onFailure");
                    }
                });

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

        Intent intent = getIntent();

        if (intent != null) {
            if (intent.hasExtra("title")) {
                title = intent.getStringExtra("title");

                Intent Nintent = new Intent(DashboardActivity.this, NotificationActivity.class);
                Nintent.putExtra("title", title);
                startActivity(Nintent);
            }
        }

    }

    @Override
    public void onClick(View view) {

        int id = view.getId();
        if (id == R.id.btnGoogleAuth || id == R.id.linearGoogleAuth || id == R.id.txtGoogleAuth) {
            mp.start();
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            intent.putExtra("title", title);
            startActivity(intent);
        }/* else if (id == R.id.btnDipAuth || id == R.id.linearDipAuth || id == R.id.txtDipAuth) {
            mp.start();
            startActivity(new Intent(DashboardActivity.this, DipAuthenticateActivity.class));
        }*/

    }

    private void displayFirebaseRegId() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(this, new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String mToken = instanceIdResult.getToken();
                Log.e("Token", mToken);
                sp.edit().putString(SaveSharedPref.REGID, mToken).apply();

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("useedkey", sp.getString(SaveSharedPref.universalseedkey, ""));
                jsonObject.addProperty("dseedkey", sp.getString(SaveSharedPref.dipseedkey, ""));
                jsonObject.addProperty("devicetoken", mToken);

                Call<ResponseBody> calling = RestClient.getEcombidService().create(RestClient.ServiceInterface.class).addDeviceId(jsonObject);
                calling.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {

                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {

                    }
                });

            }
        });
    }
}
