package org.shadowice.flocke.andotp.Services.API.Managers;

import android.util.Log;

import com.google.gson.JsonObject;

import org.shadowice.flocke.andotp.Common.Network.Rest.RestCallback;
import org.shadowice.flocke.andotp.Common.Utilities.NetworkConnectivity;
import org.shadowice.flocke.andotp.Services.API.BaseList;
import org.shadowice.flocke.andotp.Services.API.Models.DipDeviceModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceManager extends BaseList<DipDeviceModel> {

    private static volatile DeviceManager _instance = null;

    private DeviceManager() {
        super(DipDeviceModel.class);
    }

    public static DeviceManager Instance() {
        if (_instance == null) {
            synchronized (DeviceManager.class) {
                _instance = new DeviceManager();
            }
        }
        return _instance;
    }

    public void sendDipDevice(final JsonObject object, final RestCallback.CommonInfoDelegate<DipDeviceModel> delegate1) {

        if (NetworkConnectivity.isConnected()) {

            Call<DipDeviceModel> call = getEcombidInterface().sendDipDevice(object);

            call.enqueue(new Callback<DipDeviceModel>() {
                @Override
                public void onResponse(Call<DipDeviceModel> call, Response<DipDeviceModel> response) {
                    try {
                        if (!response.isSuccessful() && delegate1 != null) {
                            delegate1.CallFailedWithError("Something went wrong");
                            return;
                        }

                        if (response.body() == null && delegate1 != null) {
                            delegate1.CallFailedWithError("Something went wrong");
                            return;
                        }

                        DipDeviceModel dipDeviceModel = response.body();

                        if (delegate1 != null) {
                            delegate1.CallDidSuccess(dipDeviceModel);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (delegate1 != null)
                            delegate1.CallFailedWithError("Something went wrong");
                    }

                }

                @Override
                public void onFailure(Call<DipDeviceModel> call, Throwable t) {
                    if (delegate1 != null)
                        delegate1.CallFailedWithError("Something went wrong");
                }

            });

        } else {
            if (delegate1 != null)
                delegate1.CallFailedWithError("Your Mobile Internet is OFF!");
        }

    }


}
