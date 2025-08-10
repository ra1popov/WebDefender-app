package app.analytics;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class FirebaseAppSDK {

    public static void init(Application application) {
        FirebaseApp.initializeApp(application);
    }
}
