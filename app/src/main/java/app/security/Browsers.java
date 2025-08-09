package app.security;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import app.App;
import app.common.LibNative;
import app.internal.Preferences;
import app.scanner.LibScan;

public class Browsers {
    private static ByteBuffer browsersDB = null;

    // TODO XXX use BitSet
    private static final Set<Integer> browsersUidCache = new HashSet<Integer>(); // cache for browsers uids
    private static final Set<Integer> appsUidCache = new HashSet<Integer>(); // cache for other apps uids

    public static boolean load() {
        String version = Database.getCurrentVersion();
        final boolean result = load(version);

        return result;
    }

    public static boolean load(String version) {
        String fileName = App.getContext().getFilesDir().getAbsolutePath() + "/" + version + "/" + "browsers.db";

        if (browsersDB != null)
            LibScan.unloadDB(browsersDB); // TODO XXX sync (see Scanner)

        browsersDB = LibScan.loadDB(LibScan.SCAN_TYPE_DOMAIN, fileName, Preferences.getAppVersion());
        if (!LibScan.dbIsLoaded(browsersDB)) {
            browsersDB = null;
            clearCaches();
            return false;
        }

        //L.d(Settings.TAG_BROWSERS, "Count = " + LibScan.dbGetRecordsNumber(this.browsersDB));
        clearCaches();
        return true;
    }

    public static void clearCaches() {
        synchronized (browsersUidCache) {
            browsersUidCache.clear();
            appsUidCache.clear();
        }
    }

    /*
     * return true if package name belong to browser
     * use isBrowser(uid) as far as possible, because it uses cache
     */
    public static boolean isBrowser(String pkgName) {
        boolean result = false;

        if (pkgName == null)
            return false;

        if (browsersDB != null) {
            pkgName = LibNative.asciiToLower(pkgName);

            int recordType = LibScan.dbScanData(browsersDB, pkgName.getBytes(), pkgName.length());
            if (recordType != LibScan.RECORD_TYPE_CLEAN)
                result = true;
//			  if (!res && "adbd".equals(pkgName)) result = true; // PROFILE
        }

        //L.d(Settings.TAG_BROWSERS, "isBrowser: " + pkgName + " = " + result);
        return result;
    }

    /*
     * return true if one of packages names belong to browser
     * use isBrowser(uid) as far as possible, because it uses cache
     */
    public static boolean isBrowser(String[] pkgNames) {
        if (pkgNames == null)
            return false;

        for (String pkgName : pkgNames) {
            if (isBrowser(pkgName))
                return true;
        }

        return false;
    }

    // works only if app with uid is running!
    public static boolean isBrowser(int uid) {
        Integer value = uid;

        synchronized (browsersUidCache) // TODO XXX may be optimize locking?
        {
            if (browsersUidCache.contains(value))
                return true;
            if (appsUidCache.contains(value))
                return false;
        }

        String[] pkgNames = Processes.getNamesFromUid(uid);
        final boolean browser = isBrowser(pkgNames);

        synchronized (browsersUidCache) {
            if (browser)
                browsersUidCache.add(value);
            else
                appsUidCache.add(value);
        }

        return browser;
    }

    // TODO XXX temp function for opera referrer workaround (see TCPStateMachine)
    public static boolean isOperaClassic(String[] pkgNames) {
        if (pkgNames == null)
            return false;

        for (String pkgName : pkgNames) {
            if ("com.opera.browser.classic".equals(pkgName) || "com.opera.browser.yandex".equals(pkgName))
                return true;
        }

        return false;
    }
}
