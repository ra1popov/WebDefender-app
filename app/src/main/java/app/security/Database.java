package app.security;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import app.App;
import app.common.LibPatch;
import app.common.Utils;
import app.internal.Preferences;
import app.internal.Settings;
import app.scanner.Scanner;

public class Database {

    private static final Object lock = new Object();

    // return true if db was reinstalled
    public static boolean init() {
        synchronized (lock) {
            String versionCur = getCurrentVersion();
            if (versionCur != null && !versionCur.equals("0"))
                return false;

            // unpack db from assets

            Context context = App.getContext();
            AssetManager am = context.getAssets();

            try {
                String[] dbDirs = am.list("db");
                for (String version : dbDirs) {
                    if (version.startsWith("fast")) {
                        // fast db

                        String path = context.getFilesDir().getAbsolutePath() + "/" + version;
                        Utils.saveFile(am.open("db/" + version), path);
                    } else {
                        // main db

                        String path = context.getFilesDir().getAbsolutePath() + "/" + version + "/";
                        File dir = new File(path);

                        if (!dir.exists())
                            dir.mkdirs();

                        String[] files = am.list("db/" + version);
                        for (String file : files)
                            Utils.saveFile(am.open("db/" + version + "/" + file), path + file);

                        setCurrentVersion(version);
                    }
                }

                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }


            return false;
        }
    }

    public static String getCurrentVersion() {
        final String version = Preferences.get_s(Settings.PREF_BASES_VERSION);
        if (version == null)
            return "0"; // may be return null?

        return version;
    }

    public static void setCurrentVersion(String version) {
        Preferences.putString(Settings.PREF_BASES_VERSION, version);
    }

    public static long getCurrentFastVersion() {
        final long version = Preferences.get_l(Settings.PREF_LAST_FASTUPDATE_TIME);
        return version;
    }

    public static void setCurrentFastVersion(long version) {
        Preferences.putLong(Settings.PREF_LAST_FASTUPDATE_TIME, version);
    }

    public static String getDistribVersion() {
        Context context = App.getContext();
        AssetManager am = context.getAssets();
        String version = null;

        try {
            String[] dbDirs = am.list("db");
            version = dbDirs[0];
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (version == null)
            return "0"; // may be return null?

        return version;
    }

    public static boolean distribHaveNewer() {
        int dVer = 0;
        int cVer = 0;
        try {
            dVer = Integer.parseInt(getDistribVersion());
        } catch (NumberFormatException e) {
        }
        try {
            cVer = Integer.parseInt(getCurrentVersion());
        } catch (NumberFormatException e) {
        }

        if (dVer > 0 && (dVer > cVer || (Settings.DEBUG_DB_REPLACE && dVer == cVer)))
            return true;

        return false;
    }

    public static String getCurrentDirectory() {
        String version = getCurrentVersion();
        return App.getContext().getFilesDir().getAbsolutePath() + "/" + version + "/";
    }

    public static boolean makeNewFileVersion(String fileName, String action, String version,
                                             InputStream is, String hash) {
        String filesDir = App.getContext().getFilesDir().getAbsolutePath() + "/";

        if (version == null)
            return false;

        try {
            if (action == null || action.equals("replace") || action.equals("copy")) {
                Utils.saveFile(is, filesDir + version + "/" + fileName + ".db");
                return true;
            } else if (action.equals("patch")) {
                String oldFile = fileName + ".db";
                Utils.saveFile(is, filesDir + "temp.patch");
                boolean result = LibPatch.patchFileAndVerify(App.getContext(), getCurrentDirectory() + oldFile,
                        filesDir + "temp.patch", filesDir + version + "/" + oldFile,
                        LibPatch.DELETE_PATCH | LibPatch.DELETE_BADNEW, hash);
                return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean createDatabase(String version) {
        synchronized (lock) {
            File dir = new File(App.getContext().getFilesDir().getAbsolutePath() + "/" + version + "/");
            if (dir.mkdirs()) {
                File oldDir = new File(getCurrentDirectory());
                if (oldDir.exists() && oldDir.isDirectory()) {
                    final String[] list = oldDir.list();
                    for (String file : list) {
                        if (!copyFile(oldDir.getAbsolutePath() + "/" + file, dir.getAbsolutePath() + "/" + file))
                            return false;
                    }
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean copyFile(String input, String output) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        boolean retval = false;
        try {
            fis = new FileInputStream(input);
            fos = new FileOutputStream(output);

            int read;
            byte[] buf = new byte[8192];
            while ((read = fis.read(buf)) > 0)
                fos.write(buf, 0, read);
            fos.flush();

            retval = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (fos != null)
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return retval;
    }

    public static boolean deleteDatabases(String version) {
        boolean deletedAll = false;
        synchronized (lock) {
            String path = App.getContext().getFilesDir().getAbsolutePath() + "/" + version + "/";

            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                deletedAll = true;
                String[] files = dir.list();

                for (String file : files) {
                    File f = new File(path + file);
                    //L.d(Settings.TAG_DATABASE, "deleting file: ", file);
                    deletedAll = deletedAll && f.delete();
                }

                if (deletedAll)
                    deletedAll = dir.delete();
                //L.d(Settings.TAG_DATABASE, "deleting dir: ", Boolean.toString(deletedAll));

                setCurrentVersion(null); // reset db version
            }

            String debugDb =
                    App.getContext().getFilesDir().getAbsolutePath() + "/" + Scanner.debugDBName;
            File dbg = new File(debugDb);
            if (dbg.exists())
                dbg.delete();
        }

        return deletedAll;
    }

    public static boolean deleteDatabases() {
        return deleteDatabases(getCurrentVersion());
    }
}
