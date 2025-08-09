package app.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.FragmentActivity;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.Config;
import app.R;
import app.analytics.MyAnalyticsSDK;
import app.dependencies.ApplicationDependencies;
import app.internal.Settings;

public class Toolbox {

    public static void openURL(@NonNull Context context, String url) {
        if (!TextUtils.isEmpty(url)) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ignored) {
            }
        }
    }

    public static void reviewApp(@NonNull Context context) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Settings.APP_STORE)));
        } catch (Exception ignored) {
        }
    }

    public static void feedback(@NonNull Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + Config.SUPPORT));
            intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name));
            String out = "Manufaceturer: " + Toolbox.getDeviceName() + "\n" +
                    "OS: " + Build.VERSION.SDK_INT + "\n" +
                    "Version code: " + getAppVersion(context) + "\n" +
                    "Model: " + Build.MODEL;
            intent.putExtra(Intent.EXTRA_TEXT, out);
            context.startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer);
        }
    }

    public static String capitalize(String s) {
        if (TextUtils.isEmpty(s)) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public static void setWindowFlag(@NonNull AppCompatActivity activity, final int bits, boolean on) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    public static void setStatusBarAndHomeTransparent(@NonNull FragmentActivity activity, boolean isFullscreen) {
        Window window = activity.getWindow();
        if (isFullscreen) {
            window.setNavigationBarColor(Color.BLACK);
        } else {
            window.setNavigationBarColor(Color.BLACK);
        }

        int flags = window.getDecorView().getSystemUiVisibility();
        flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        flags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

        window.getDecorView().setSystemUiVisibility(flags);

        //make fully Android Transparent Status bar
        setWindowFlag((AppCompatActivity) activity, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);

        if (isFullscreen) {
            window.setStatusBarColor(Color.TRANSPARENT);
        } else {
            window.setStatusBarColor(Color.BLACK);
        }
    }

    public static void setStatusBarLight(@NonNull FragmentActivity activity, boolean isLight) {
        Window window = activity.getWindow();
        int flags = window.getDecorView().getSystemUiVisibility();
        if (isLight) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }

    public static String getAppVersion(@NonNull Context context) {
        int appVersion = 1;
        String packageName = context.getPackageName();
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            appVersion = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return String.valueOf(appVersion);
    }

    public static Drawable getIconApplication(@NotNull Context context, @NonNull String packageName) throws PackageManager.NameNotFoundException {
        return context.getPackageManager().getApplicationIcon(packageName);
    }

    @NonNull
    public static String getCurrentLanguage() {
        return Locale.getDefault().getLanguage();
    }

    public static boolean hasPermission(@NonNull Context context, @NonNull String permission) {
        return PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED;
    }

    public static boolean isScreenOn(@NonNull Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return powerManager.isInteractive();
    }

    public static Notification getNotification(@NonNull Context context, @NonNull String channelName, @NonNull String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return getNotification26(context, channelName, channelId);
        } else {
            return getNotificationPrior26(context, channelId);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static Notification getNotification26(@NonNull Context context, @NonNull String channelName, @NonNull String channelId) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setShowBadge(false);
        channel.setSound(null, null); // Disabling sound for the channel.

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS) // Notification with LED blinking only, without sound or vibration.
                .setSmallIcon(R.drawable.ic_logo_notification)
                .setCategory(Notification.CATEGORY_SERVICE);

        return builder.build();
    }

    private static Notification getNotificationPrior26(@NonNull Context context, @NonNull String channelId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setSmallIcon(R.drawable.ic_logo_notification)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setSound(null); // Disabling notification sound.

        return builder.build();
    }

    public static void showAlertNotification(@NonNull Context context, int id, String title, String text) {
        MyAnalyticsSDK.log("Toolbox.showAlertNotification method has been called");
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        Notification notification = Toolbox.getAlertNotification(ApplicationDependencies.getApplication(), Settings.APP_NAME + " Alert", Settings.APP_PACKAGE + ":alert", intent, title, text);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(id, notification);
    }

    public static boolean hasAlertNotification(@NonNull Context context, int id) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        for (StatusBarNotification notification : manager.getActiveNotifications()) {
            if (notification.getId() == id) {
                return true;
            }
        }
        return false;
    }

    private static Notification getAlertNotification(@NonNull Context context, @NonNull String channelName, @NonNull String channelId, @NonNull Intent intent, String title, String text) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return getAlertNotification26(context, channelName, channelId, pendingIntent, title, text);
        } else {
            return getAlertNotificationPrior26(context, channelId, pendingIntent, title, text);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static Notification getAlertNotification26(@NonNull Context context, @NonNull String channelName, @NonNull String channelId, @NonNull PendingIntent intent, String title, String text) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setShowBadge(false);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_logo))
                .setSmallIcon(R.drawable.ic_logo_notification_alert)
                .setCategory(Notification.CATEGORY_SERVICE);

        return builder.build();
    }

    private static Notification getAlertNotificationPrior26(@NonNull Context context, @NonNull String channelId, @NonNull PendingIntent intent, String title, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_logo))
                .setSmallIcon(R.drawable.ic_logo_notification_alert)
                .setCategory(Notification.CATEGORY_SERVICE);

        return builder.build();
    }

    public static boolean hasVpn(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return hasVpn30(context);
        } else {
            return hasVpnPrior30(context);
        }
    }

    @TargetApi(Build.VERSION_CODES.R)
    private static boolean hasVpn30(@NonNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = connectivityManager.getAllNetworks();
        List<NetworkCapabilities> capabilitiesList = new ArrayList<>();
        int myUid = context.getApplicationInfo().uid;
        boolean isVpn = false;

        loop:
        while (true) {
            for (Network _network : networks) {
                NetworkCapabilities _capabilities = connectivityManager.getNetworkCapabilities(_network);
                capabilitiesList.add(_capabilities);
                if (_capabilities == null) {
                    capabilitiesList.clear();
                    continue loop;
                }
            }

            for (NetworkCapabilities _capabilities : capabilitiesList) {
                int ownerUid = _capabilities.getOwnerUid();
                if (myUid == ownerUid
                        && _capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                        && !_capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) {
                    isVpn = true;
                    break loop;
                }
            }

            break;
        }

        return isVpn;
    }

    private static boolean hasVpnPrior30(@NonNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = connectivityManager.getAllNetworks();
        List<NetworkCapabilities> capabilitiesList = new ArrayList<>();
        boolean isVpn = false;

        loop:
        while (true) {
            for (Network _network : networks) {
                NetworkCapabilities _capabilities = connectivityManager.getNetworkCapabilities(_network);
                capabilitiesList.add(_capabilities);
                if (_capabilities == null) {
                    capabilitiesList.clear();
                    continue loop;
                }
            }

            for (NetworkCapabilities _capabilities : capabilitiesList) {
                if (_capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                        && !_capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) {
                    isVpn = true;
                    break loop;
                }
            }

            break;
        }

        return isVpn;
    }

    public static boolean hasInternet(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    public static String getSystemProperty(String propName) {
        String line;
        BufferedReader input = null;
        try {
            Process process = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(process.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException e) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }
        return line;
    }

    public static void setCursorDrawable(@NonNull Context context, @NonNull SearchView.SearchAutoComplete searchAutoComplete, @ColorRes int id) {
        try {
            @SuppressWarnings("JavaReflectionMemberAccess")
            @SuppressLint("SoonBlockedPrivateApi")
            Field cursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            cursorDrawableRes.setAccessible(true);
            int cursorDrawable = cursorDrawableRes.getInt(searchAutoComplete);

            @SuppressWarnings("JavaReflectionMemberAccess")
            @SuppressLint("DiscouragedPrivateApi")
            Field editor = TextView.class.getDeclaredField("mEditor");
            editor.setAccessible(true);
            Object editorObject = editor.get(searchAutoComplete);
            if (editorObject == null) {
                return;
            }

            Class<?> editorClass = editorObject.getClass();
            Field cursorDrawableField = editorClass.getDeclaredField("mCursorDrawable");
            cursorDrawableField.setAccessible(true);

            Drawable[] drawables = new Drawable[2];
            drawables[0] = ContextCompat.getDrawable(context, cursorDrawable);
            drawables[1] = ContextCompat.getDrawable(context, cursorDrawable);
            if (drawables[0] == null || drawables[1] == null) {
                return;
            }

            drawables[0].setColorFilter(context.getColor(id), PorterDuff.Mode.SRC_IN);
            drawables[1].setColorFilter(context.getColor(id), PorterDuff.Mode.SRC_IN);

            cursorDrawableField.set(editorObject, drawables);
        } catch (Exception ignored) {
        }
    }

}
