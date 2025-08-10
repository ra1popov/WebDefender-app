package app.analytics;

import android.app.Application;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

import app.Config;

public class FirebaseAnalyticsSDK {

    private static FirebaseAnalytics _instance;

    public static final String WARNING = "warning";


    public static void init(Application application) {
        if (Config.DEBUG) {
            return;
        }
        _instance = FirebaseAnalytics.getInstance(application.getApplicationContext());
    }

    public static void LogWarning(String msg) {
        if (Config.DEBUG) {
            return;
        }

        Bundle params = new Bundle();
        params.putString("msg", msg);

        _instance.logEvent(WARNING, params);
    }

}
