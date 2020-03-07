package org.shadowice.flocke.andotp.Services.API.Managers;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.shadowice.flocke.andotp.Common.Network.Rest.RestCallback;
import org.shadowice.flocke.andotp.Common.Utilities.NetworkConnectivity;
import org.shadowice.flocke.andotp.Services.API.BaseList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeleteDigitManager extends BaseList<ResponseBody> {

    private static volatile DeleteDigitManager _instance = null;

    private DeleteDigitManager() {
        super(ResponseBody.class);
    }

    public static DeleteDigitManager Instance() {
        if (_instance == null) {
            synchronized (DeleteDigitManager.class) {
                _instance = new DeleteDigitManager();
            }
        }
        return _instance;
    }

    public void deleteDipDigit(final JsonObject jsonObject, final RestCallback.CommonInfoDelegate<ResponseBody> delegate1) {

        if (NetworkConnectivity.isConnected()) {

            Call<ResponseBody> call = getEcombidInterface().deleteDipDevice(jsonObject);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                    try {

                        Log.e("param", " " + jsonObject);

                        if (!response.isSuccessful() && delegate1 != null) {
                            delegate1.CallFailedWithError("Something went wrong");
                            return;
                        }

                        if (response.body() == null && delegate1 != null) {
                            delegate1.CallFailedWithError("Something went wrong");
                            return;
                        }

                        if (delegate1 != null) {
                            delegate1.CallDidSuccess(response.body());
                        } else {
                            delegate1.CallFailedWithError("Something went wrong");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (delegate1 != null)
                            delegate1.CallFailedWithError("" + e.getMessage());
                    }

                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    if (delegate1 != null)
                        delegate1.CallFailedWithError(t.getMessage());
                }

            });

        } else {
            if (delegate1 != null)
                delegate1.CallFailedWithError("Please check internet connection");

        }

    }


}
