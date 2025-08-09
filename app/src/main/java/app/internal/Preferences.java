package app.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import app.App;
import app.common.Hasher;
import app.common.LibNative;
import app.common.Utils;
import app.common.debug.L;
import app.util.Util;

public class Preferences {

    private static final SharedPreferences prefs;
    private static SharedPreferences.Editor editor = null;
    private static final ReentrantLock editLock = new ReentrantLock();
    private static int lockCounter = 0;

    private static int appVersion;
    private static int updateRetryTime;
    private static String appCertHash = null;
    private static boolean rooted = false;
    private static boolean netlinkWork = false;

    static {
        prefs = App.getContext().getSharedPreferences(App.getContext().getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    public static SharedPreferences getSharedPreferences() {
        return prefs;
    }

    public static void load(Context context) {
        String country = get_s(Settings.PREF_PROXY_COUNTRY);
        ProxyBase.setCurrentCountry(country);

        ApplicationInfo info = null;
        PackageManager pm = context.getPackageManager();
        try {
            info = pm.getApplicationInfo(App.packageName(), PackageManager.GET_META_DATA);
            PackageInfo pinfo = pm.getPackageInfo(App.packageName(), PackageManager.GET_META_DATA);
            appVersion = pinfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (info == null) {
            return;
        }

        Preferences.editStart();
        try {
            updateRetryTime = info.metaData.getInt(Settings.PREF_UPDATE_RETRY_TIME, 3 * 60 * 60); // 10800, 3h in seconds

            if (get_s(Settings.PREF_UPDATE_URL) == null) {
                putString(Settings.PREF_UPDATE_URL, info.metaData.getString(Settings.MANIFEST_UPDATE_URL));
            }

        } finally {
            Preferences.editEnd();
        }

        getCertHash(context, pm);

        rooted = hasRoot();
        netlinkWork = LibNative.netlinkIsWork();
    }


    private static void getCertHash(Context context, PackageManager pm) {
        try {
            Signature sig = pm.getPackageInfo(App.packageName(), PackageManager.GET_SIGNATURES).signatures[0];
            byte[] hash = Hasher.md5(sig.toByteArray());
            appCertHash = Utils.toHex(hash);
        } catch (Exception ignored) {
        }
    }

    public static void clearToken() {
        Preferences.editStart();
        try {
            Preferences.putString(Settings.PREF_USER_TOKEN, null);
            Preferences.putLong(Settings.PREF_USER_TOKEN_TIME, 0);
        } finally {
            Preferences.editEnd();
        }
    }

    public static boolean isActive() {
        return getBoolean(Settings.PREF_ACTIVE, false);
    }

    // return update interval in seconds
    public static int getUpdateRetryTime() {
        int t = getInt(Settings.PREF_UPDATE_RETRY_TIME, 0);
        if (t <= 0)
            t = updateRetryTime;

        return t;
    }

    public static int getAppVersion() {
        return appVersion;
    }

    public static ArrayList<String> getDNSServers() {
        editLock.lock();
        try {
            ArrayList<String> res = new ArrayList<>();
            final Set<String> set = prefs.getStringSet(Settings.PREF_DNS_SERVERS, null);
            if (set != null) {
                res.addAll(set);
            }

            return res;
        } finally {
            editLock.unlock();
        }
    }

    public static void putDNSServers(ArrayList<String> servers) {
        editLock.lock();
        try {
            Set<String> set = new HashSet<>();
            for (String s : servers)
                set.add(s);

            final SharedPreferences.Editor e = prefs.edit();
            e.putStringSet(Settings.PREF_DNS_SERVERS, set);
            commit(e);
        } finally {
            editLock.unlock();
        }
    }

    public static boolean hasRoot() {
        // checkRootMethod2
        if (new File("/system/app/Superuser.apk").exists())
            return true;

        // checkRootMethod3
        String[] paths = {"/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su",
                "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su",
                "/data/local/su"};
        for (String path : paths)
            if (new File(path).exists()) return true;

        return false;
    }

    //

    public static int getInt(String name, int def) {
        editLock.lock();
        try {
            return prefs.getInt(name, def);
        } finally {
            editLock.unlock();
        }
    }

    public static int get_i(String name) {
        int def = 0;

        if (name.equals(Settings.PREF_UPDATE_ERROR_COUNT))
            // use such value to force updates if no network on install (will be increased by 2 or 3 if no network)
            def = -1111;

        editLock.lock();
        try {
            return prefs.getInt(name, def);
        } finally {
            editLock.unlock();
        }
    }

    public static long getLong(String name, long def) {
        editLock.lock();
        try {
            return prefs.getLong(name, def);
        } finally {
            editLock.unlock();
        }
    }

    public static long get_l(String name) {
        long def = 0;

        editLock.lock();
        try {
            return prefs.getLong(name, def);
        } finally {
            editLock.unlock();
        }
    }

    public static boolean getBoolean(String name, boolean def) {
        editLock.lock();
        try {
            return prefs.getBoolean(name, def);
        } finally {
            editLock.unlock();
        }
    }

    public static boolean get(String name) {
        boolean def = false;

        if (name.equals(Settings.PREF_BLOCK_MALICIOUS) ||
                name.equals(Settings.PREF_BLOCK_APKS) ||

                name.equals(Settings.PREF_SOCIAL_OTHER) ||
                name.equals(Settings.PREF_SOCIAL_GPLUS) ||
                name.equals(Settings.PREF_SOCIAL_VK) ||
                name.equals(Settings.PREF_SOCIAL_FB) ||
                name.equals(Settings.PREF_SOCIAL_TWITTER) ||
                name.equals(Settings.PREF_SOCIAL_OK) ||
                name.equals(Settings.PREF_SOCIAL_MAILRU) ||
                name.equals(Settings.PREF_SOCIAL_LINKEDIN) ||
                name.equals(Settings.PREF_SOCIAL_MOIKRUG) ||

                name.equals(Settings.PREF_ANONYMIZE_ONLY_BRW) ||
                name.equals(Settings.PREF_APP_PROXY) ||
                name.equals(Settings.PREF_STATS)) {
            def = true;
        }

        editLock.lock();
        try {
            return prefs.getBoolean(name, def);
        } finally {
            editLock.unlock();
        }
    }

    public static String getString(String name, String def) {
        editLock.lock();
        try {
            return prefs.getString(name, def);
        } finally {
            editLock.unlock();
        }
    }

    public static String get_s(String name) {
        String def = null;

        if (name.equals(Settings.PREF_BASES_VERSION)) def = "0";
        else if (name.equals(Settings.PREF_PROXY_COUNTRY)) def = "auto";

        editLock.lock();
        try {
            return prefs.getString(name, def);
        } finally {
            editLock.unlock();
        }
    }

    public static Set<String> getStrings(String name, Set<String> def) {
        editLock.lock();
        try {
            return prefs.getStringSet(name, def);
        } finally {
            editLock.unlock();
        }
    }

    public static void editStart() {
        editLock.lock();

        if (lockCounter == 0)
            editor = prefs.edit();
        lockCounter++;
    }

    public static void editEnd() {
        lockCounter--;
        if (lockCounter == 0) {
            commit(editor); // TODO XXX ahaha, was java.lang.ArrayIndexOutOfBoundsException on texet
            editor = null;
        }

        editLock.unlock();
    }

    private static boolean commit(SharedPreferences.Editor editor) {
        boolean result = true;

        // TODO XXX apply is faster (commit could take up to 100ms for writing to the XML backed file)
        // but apply don't guarantee settings save if app killed by OOM or crashed

        if (Util.isMainThread())
            editor.apply();
        else
            result = editor.commit();

        return result;
    }

    public static void putBoolean(String name, boolean value) {
        editLock.lock();
        try {
            if (Settings.DEBUG) L.d(Settings.TAG_PREFERENCES, "'" + name + "' -> " + value);

            SharedPreferences.Editor e = (editor != null) ? editor : prefs.edit();
            e.putBoolean(name, value);
            if (editor == null)
                commit(e);
        } finally {
            editLock.unlock();
        }
    }

    public static void putInt(String name, int value) {
        editLock.lock();
        try {
            if (Settings.DEBUG) L.d(Settings.TAG_PREFERENCES, "'" + name + "' -> " + value);

            SharedPreferences.Editor e = (editor != null) ? editor : prefs.edit();
            e.putInt(name, value);
            if (editor == null)
                commit(e);
        } finally {
            editLock.unlock();
        }
    }

    public static void putString(String name, String value) {
        editLock.lock();
        try {
            if (Settings.DEBUG) L.d(Settings.TAG_PREFERENCES, "'" + name + "' -> '" + value + "'");

            SharedPreferences.Editor e = (editor != null) ? editor : prefs.edit();
            e.putString(name, value);
            if (editor == null)
                commit(e);
        } finally {
            editLock.unlock();
        }
    }

    public static void putLong(String name, long value) {
        editLock.lock();
        try {
            if (Settings.DEBUG) L.d(Settings.TAG_PREFERENCES, "'" + name + "' -> " + value);

            SharedPreferences.Editor e = (editor != null) ? editor : prefs.edit();
            e.putLong(name, value);
            if (editor == null)
                commit(e);
        } finally {
            editLock.unlock();
        }
    }

}
