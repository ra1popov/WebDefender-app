package app.common;

import android.content.Context;

import app.analytics.FirebaseAnalyticsSDK;

public class WiFi {

    public static boolean isProxySet(Context context) {
        return false;
    }

    public static void unsetWifiProxySettings(Context context) {
        FirebaseAnalyticsSDK.LogWarning("WiFi.unsetWifiProxySettings method has been called");
    }

}
