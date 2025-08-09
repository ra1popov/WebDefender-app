package app.security;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import app.App;
import app.common.NetUtil;
import app.common.Utils;
import app.netfilter.FilterVpnService;

// TODO XXX optimize and cache *IsAllowed(uid)!
// TODO XXX if app was added while have live connection? now drop all connection (not app)!

public class Firewall {

    public static final String APPS_BLOCKED = "apps_blocked";

    // lists for packages names blocked on mobile and wifi inet
    private static HashSet<String> mobileBlocked = new HashSet<>(350);
    private static HashSet<String> wifiBlocked = new HashSet<>(350);
    private static boolean isWifiNetwork = false;

    // TODO XXX use BitSet
    private static final Set<Integer> mobileAllowedUidCache = new HashSet<>(); // cache for allowed apps in mobile network
    private static final Set<Integer> mobileBlockedUidCache = new HashSet<>(); // cache for blocked apps in mobile network
    private static final Set<Integer> wifiAllowedUidCache = new HashSet<>(); // cache for allowed apps in wifi network
    private static final Set<Integer> wifiBlockedUidCache = new HashSet<>(); // cache for blocked apps in wifi network

    private static boolean inited = false;
    private static String firewallFileName;
    private static final Object lock = new Object();

    static {
        Context context = App.getContext();
        firewallFileName = context.getFilesDir().getAbsolutePath() + "/firewall.json";
    }

    public static void init() {
        synchronized (lock) {
            if (inited)
                return;

            clear();
            load();

            onNetworkChanged();

            if (mobileBlocked.size() > 0 || wifiBlocked.size() > 0) {
                FilterVpnService.notifyDropConnections(App.getContext()); // firewall inited and have blocked apps
            }

            inited = true;
        }
    }

    private static void clear() {
        mobileBlocked.clear();
        wifiBlocked.clear();
        clearCaches(true, true);
    }

    private static void load() {
        synchronized (lock) {
            try {
                // load
                final String str = Utils.getFileContents(firewallFileName);
                if (str != null) {
                    HashSet<String> mobileBlocked = new HashSet<>(350);
                    HashSet<String> wifiBlocked = new HashSet<>(350);

                    try {
                        JSONObject obj0 = new JSONObject(str);

                        // apps blocked

                        JSONArray arr = obj0.optJSONArray(APPS_BLOCKED);
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                if (obj == null)
                                    continue;

                                String pkgName = obj.optString("packageName");
                                if (pkgName.isEmpty())
                                    continue;

                                boolean allowMobile = obj.optBoolean("mobileAllow", true);
                                if (!allowMobile)
                                    mobileBlocked.add(pkgName);
                                boolean allowWiFi = obj.optBoolean("wifiAllow", true);
                                if (!allowWiFi)
                                    wifiBlocked.add(pkgName);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Firewall.mobileBlocked = mobileBlocked;
                    Firewall.wifiBlocked = wifiBlocked;
                    clearCaches(true, true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        synchronized (lock) {
            if (!inited)
                return;

            JSONObject obj = null;
            try {
                obj = new JSONObject();

                // apps blocked

                JSONArray arr = getBlockedApps();
                if (arr != null)
                    obj.put(APPS_BLOCKED, arr);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // save

            if (obj != null) {
                byte[] data = obj.toString().getBytes();
                try {
                    Utils.saveFile(data, firewallFileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void clearCaches(boolean mobile, boolean wifi) {
        if (mobile) {
            synchronized (mobileAllowedUidCache) {
                mobileAllowedUidCache.clear();
                mobileBlockedUidCache.clear();
            }
        }

        if (wifi) {
            synchronized (wifiAllowedUidCache) {
                wifiAllowedUidCache.clear();
                wifiBlockedUidCache.clear();
            }
        }
    }

    private static JSONArray getBlockedApps() {
        HashSet<String> mobileBlocked = (HashSet) Firewall.mobileBlocked.clone();
        HashSet<String> wifiBlocked = (HashSet) Firewall.wifiBlocked.clone();

        JSONArray arr = new JSONArray();

        // mobile and mobile+wifi blocked

        Iterator iterator = mobileBlocked.iterator();
        while (iterator.hasNext()) {
            final String pkgName = (String) iterator.next();

            try {
                JSONObject obj = new JSONObject();
                obj.put("packageName", pkgName);
                obj.put("mobileAllow", false);
                obj.put("wifiAllow", (!wifiBlocked.contains(pkgName)));

                arr.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // wifi blocked

        iterator = wifiBlocked.iterator();
        while (iterator.hasNext()) {
            final String pkgName = (String) iterator.next();

            if (mobileBlocked.contains(pkgName))
                continue;

            try {
                JSONObject obj = new JSONObject();
                obj.put("packageName", pkgName);
                obj.put("mobileAllow", true);
                obj.put("wifiAllow", false);

                arr.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return arr;
    }

    // TODO XXX optimize this
    public static void onNetworkChanged() {
        isWifiNetwork = !NetUtil.isMobile();
    }

    //

    // change app rule for mobile network
    public static void mobileAppState(String pkgName, boolean allow) {
        if (!allow)
            mobileBlocked.add(pkgName);
        else
            mobileBlocked.remove(pkgName);

        clearCaches(true, false);
    }

    public static boolean mobileAppIsAllowed(String pkgName) {
        return (!mobileBlocked.contains(pkgName));
    }

    /*
     * return false if one app from list blocked in mobile network
     * use mobileAppIsAllowed(uid) as far as possible, because it uses cache
     */
    public static boolean mobileAppIsAllowed(String[] pkgNames) {
        if (pkgNames == null)
            return false;

        for (String pkgName : pkgNames) {
            if (!mobileAppIsAllowed(pkgName))
                return false;
        }

        return true;
    }

    // only works if app with uid running
    public static boolean mobileAppIsAllowed(int uid) {
        Integer value = uid;

        synchronized (mobileAllowedUidCache) // TODO XXX may be optimize locking?
        {
            if (mobileAllowedUidCache.contains(value))
                return true;
            if (mobileBlockedUidCache.contains(value))
                return false;
        }

        String[] pkgNames = Processes.getNamesFromUid(uid);
        boolean allowed = mobileAppIsAllowed(pkgNames);

        synchronized (mobileAllowedUidCache) {
            if (allowed)
                mobileAllowedUidCache.add(value);
            else
                mobileBlockedUidCache.add(value);
        }

        return allowed;
    }

    //

    // change app rule for wifi network
    public static void wifiAppState(String pkgName, boolean allow) {
        if (!allow)
            wifiBlocked.add(pkgName);
        else
            wifiBlocked.remove(pkgName);

        clearCaches(false, true);
    }

    public static boolean wifiAppIsAllowed(String pkgName) {
        return (!wifiBlocked.contains(pkgName));
    }

    /*
     * return false if one app from list blocked in WiFi network
     * use wifiAppIsAllowed(uid) as far as possible, because it uses cache
     */
    public static boolean wifiAppIsAllowed(String[] pkgNames) {
        if (pkgNames == null)
            return false;

        for (String pkgName : pkgNames) {
            if (!wifiAppIsAllowed(pkgName))
                return false;
        }

        return true;
    }

    // only works if app with uid running
    public static boolean wifiAppIsAllowed(int uid) {
        Integer value = uid;

        synchronized (wifiAllowedUidCache) // TODO XXX may be optimize locking?
        {
            if (wifiAllowedUidCache.contains(value))
                return true;
            if (wifiBlockedUidCache.contains(value))
                return false;
        }

        String[] pkgNames = Processes.getNamesFromUid(uid);
        boolean allowed = wifiAppIsAllowed(pkgNames);

        synchronized (wifiAllowedUidCache) {
            if (allowed)
                wifiAllowedUidCache.add(value);
            else
                wifiBlockedUidCache.add(value);
        }

        return allowed;
    }

    //

    public static boolean appIsAllowed(int uid) {
        if (isWifiNetwork)
            return wifiAppIsAllowed(uid);
        else
            return mobileAppIsAllowed(uid);
    }

    public static boolean appIsUser(int uid) {
        String[] pkgNames = Processes.getUserNamesFromUid(uid);
        if (pkgNames == null || pkgNames.length == 0) {
            return false;
        }
        return true;
    }

}
