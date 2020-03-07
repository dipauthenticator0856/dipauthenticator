package org.shadowice.flocke.andotp.Common.Utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.shadowice.flocke.andotp.DipAuthenticatorApplication;


public class NetworkConnectivity {

    public static boolean isConnected() {
        try {
            ConnectivityManager cm = (ConnectivityManager) DipAuthenticatorApplication.getContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
//            Log.e("netinfo"," "+netInfo.isConnected());
            if (netInfo != null && netInfo.isConnected()) {
                Log.e("netinfo"," "+netInfo.isConnected());
                return true;
            } else {
                Log.e("netinfo"," false ");
                return false;
            }
        } catch (Exception e) {
            Log.e("netinfo"," catch ");
            e.printStackTrace();
            return false;
        }
    }

}
