package org.shadowice.flocke.andotp;

import android.app.Activity;
import android.content.Context;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;
import androidx.appcompat.app.AppCompatDelegate;
import android.view.Display;
import android.view.WindowManager;

public class DipAuthenticatorApplication extends MultiDexApplication {

    public static final String TAG = DipAuthenticatorApplication.class.getSimpleName();
    public static int windowHeight;
    private static DipAuthenticatorApplication _intance = null;
    Activity activity;

    public DipAuthenticatorApplication() {
        _intance = this;
    }

    public static Context getContext() {
        return _intance;
    }

    public static synchronized DipAuthenticatorApplication getInstance() {
        return _intance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        MultiDex.install(this);
        windowManager();
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    private void windowManager() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        windowHeight = display.getHeight();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }
}
