package org.shadowice.flocke.andotp.Activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.shadowice.flocke.andotp.Common.Network.Rest.RestClient;
import org.shadowice.flocke.andotp.R;
import org.shadowice.flocke.andotp.Services.API.Models.NotificationModel;
import org.shadowice.flocke.andotp.Utilities.SaveSharedPref;
import org.shadowice.flocke.andotp.View.ItemTouchHelper.AdapterItemClickListener;
import org.shadowice.flocke.andotp.View.NotificationAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter notificationAdapter;
    private List<NotificationModel> notificationList = new ArrayList<>();
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        Objects.requireNonNull(getSupportActionBar()).hide();

        sp = getSharedPreferences(SaveSharedPref.PREF, Context.MODE_PRIVATE);

        ImageView imgBack = findViewById(R.id.imgBack);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        getNotifications();
    }

    private void getNotifications() {

        final ProgressDialog pDialog = new ProgressDialog(this);
        pDialog.setMessage("Please wait...");
        pDialog.setCancelable(false);
        pDialog.show();

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("seedkey", sp.getString(SaveSharedPref.universalseedkey, ""));
//        jsonObject.addProperty("seedkey","2a58b7740122dc1b");

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
                            recyclerView.setAdapter(notificationAdapter);
                            notificationAdapter.setCustomCallback(new AdapterItemClickListener<String>() {
                                @Override
                                public void onItemClickData(String data) {

                                    AlertDialog.Builder builder = new AlertDialog.Builder(NotificationActivity.this);
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

}
