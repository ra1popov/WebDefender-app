package app.ui;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import app.R;
import app.analytics.FirebaseAnalyticsSDK;
import app.dependencies.ApplicationDependencies;
import app.util.Util;

public class Toasts {

    public static void showToast(final CharSequence message) {
        Util.runOnMain(() -> Toast.makeText(ApplicationDependencies.getApplication(), message, Toast.LENGTH_LONG).show());
    }

    public static void showToast(final int resId) {
        showToast(ApplicationDependencies.getApplication().getResources().getText(resId));
    }

    public static void showToast(final String message) {
        showToast((CharSequence) message);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void showVpnEstablishError() {
        FirebaseAnalyticsSDK.LogWarning("Toasts.showVpnEstablishError method has been called");
        showToast(R.string.vpn_error);
    }

    public static void showVpnEstablishException() {
        FirebaseAnalyticsSDK.LogWarning("Toasts.showVpnEstablishException method has been called");
        showToast(R.string.vpn_exception);
    }

    public static void showProxyDeleteError(String proxyPackage) {
        FirebaseAnalyticsSDK.LogWarning("Toasts.showProxyDeleteError method has been called");
        Context context = ApplicationDependencies.getApplication();

        String message;
        if (proxyPackage == null) {
            message = context.getString(R.string.proxy_needs_to_be_disabled2);
        } else {
            if (TextUtils.isEmpty(proxyPackage)) {
                message = context.getString(R.string.proxy_needs_to_be_disabled, context.getString(R.string.app_not_found));
            } else {
                message = context.getString(R.string.proxy_needs_to_be_disabled, proxyPackage);
            }
        }

        showToast(message);
    }

    public static void showFirmwareVpnUnavailable() {
        FirebaseAnalyticsSDK.LogWarning("Toasts.showFirmwareVpnUnavailable method has been called");
        showToast(R.string.vpn_unavailable);
    }

}
