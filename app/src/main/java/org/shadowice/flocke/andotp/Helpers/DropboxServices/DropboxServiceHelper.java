package org.shadowice.flocke.andotp.Helpers.DropboxServices;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.users.FullAccount;

import org.shadowice.flocke.andotp.Utilities.SaveSharedPref;

public class DropboxServiceHelper {

    private Context context;

    public DropboxServiceHelper(Context context) {
        this.context = context;
    }

    public boolean tokenExists() {
        SharedPreferences prefs = context.getSharedPreferences(SaveSharedPref.PREF, Context.MODE_PRIVATE);
        String accessToken = prefs.getString(SaveSharedPref.DROPBOX_ACCESS_TOKEN, null);
        return accessToken != null;
    }

    public void getAccessToken() {
        String accessToken = Auth.getOAuth2Token(); //generate Access Token
        if (accessToken != null) {
            //Store accessToken in SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences(SaveSharedPref.PREF, Context.MODE_PRIVATE);
            prefs.edit().putString(SaveSharedPref.DROPBOX_ACCESS_TOKEN, accessToken).apply();

        }
    }

    public String retrieveAccessToken() {
        //check if ACCESS_TOKEN is stored on previous app launches
        SharedPreferences prefs =  context.getSharedPreferences(SaveSharedPref.PREF, Context.MODE_PRIVATE);
        String accessToken = prefs.getString(SaveSharedPref.DROPBOX_ACCESS_TOKEN, null);
        if (accessToken == null) {
            Log.d("AccessToken Status", "No token found");
            return null;
        } else {
            //accessToken already exists
            Log.d("AccessToken Status", "Token exists");
            return accessToken;
        }
    }

    public void getDropboxUserAccount() {

            String DROPBOX_ACCESS_TOKEN = retrieveAccessToken();

            if (DROPBOX_ACCESS_TOKEN == null) return;
            new UserAccountTask(DropboxClient.getClient(DROPBOX_ACCESS_TOKEN), new UserAccountTask.TaskDelegate() {
                @Override
                public void onAccountReceived(FullAccount account) {
                    //Print account's info
                    Log.d("User", account.getEmail());
                    Log.d("User", account.getName().getDisplayName());
                    Log.d("User", account.getAccountType().name());
//                updateUI(account);
                }

                @Override
                public void onError(Exception error) {
                    Log.d("User", "Error receiving account details.");
                }
            }).execute();

    }

}
