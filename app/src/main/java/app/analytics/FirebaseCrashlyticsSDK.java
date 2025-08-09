package app.analytics;

import android.app.Application;

import androidx.annotation.NonNull;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import app.Config;

public class FirebaseCrashlyticsSDK {

    private static FirebaseCrashlytics _instance;


    public static void init(Application application) {
        if (Config.DEBUG) {
            return;
        }
        _instance = FirebaseCrashlytics.getInstance();
    }

    public static void LogException(@NonNull Throwable throwable) {
        if (Config.DEBUG) {
            return;
        }

        _instance.recordException(throwable);
    }

}
