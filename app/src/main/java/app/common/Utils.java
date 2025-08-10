package app.common;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Process;
import android.util.Base64;
import android.view.WindowManager.BadTokenException;

import org.squareup.okhttp.OkHttpClient;
import org.squareup.okhttp.OkUrlFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import app.App;
import app.common.debug.L;
import app.internal.Settings;
import app.util.FileUtils;

public class Utils {

    public static boolean isEndOfChuncked(byte[] data, int offset, int size) {
        return (data[offset + size - 5] == '0' &&
                data[offset + size - 4] == '\r' && data[offset + size - 3] == '\n' &&
                data[offset + size - 2] == '\r' && data[offset + size - 1] == '\n');
    }

    public static long fileSize(String path) {
        File f = new File(path);
        return f.length();
    }

    public static String formatDateTime(long time, boolean getDate, boolean getTime, boolean getSeconds) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTimeInMillis(time);
        StringBuilder sb = new StringBuilder(30);

        if (getDate) {
            int day = cal.get(Calendar.DAY_OF_MONTH);
            int month = cal.get(Calendar.MONTH) + 1;
            int year = cal.get(Calendar.YEAR);

            if (day < 10)
                sb.append('0');
            sb.append(day).append('.');
            if (month < 10)
                sb.append('0');
            sb.append(month).append('.');
            if (year < 10)
                sb.append('0');
            sb.append(year);

            if (getTime)
                sb.append(' ');
        }

        if (getTime) {
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);
            int second = cal.get(Calendar.SECOND);

            if (hour < 10)
                sb.append('0');
            sb.append(hour).append(':');
            if (minute < 10)
                sb.append('0');
            sb.append(minute);

            if (getSeconds) {
                sb.append(':');
                if (second < 10)
                    sb.append('0');
                sb.append(second);
            }
        }

        return sb.toString();
    }

    public static String getDomain(String url) {
        if (url == null)
            return null;

        final int len = url.length();

        int schemePos = LibNative.asciiIndexOf("://", url);
        if (schemePos > 0 && len > schemePos + 3)
            url = url.substring(schemePos + 3);

        int s = url.indexOf('/');
        int p = url.indexOf(':');
        int q = url.indexOf('?');
        if ((p >= 0 && s >= 0 && p < s) || s < 0) s = p;
        if ((q >= 0 && s >= 0 && q < s) || s < 0) s = q;

        if (s >= 0) return url.substring(0, s); // TODO XXX may be check lenght also?
        else return url;
    }

    public static String ipToString(byte[] ip, int port) {
        return LibNative.ipToString(ip, port);
    }

    public static int ipToInt(byte[] ip) {
        if (ip == null || ip.length != 4)
            return -1;

        return ((ip[0] & 0xFF) | ((ip[1] & 0xFF) << 8) |
                ((ip[2] & 0xFF) << 16) | ((ip[3] & 0xFF) << 24));
    }

    public static byte[] intToIp(int i) {
        byte[] buf = new byte[4];

        buf[0] = (byte) (i & 0xFF);
        buf[1] = (byte) ((i >> 8) & 0xFF);
        buf[2] = (byte) ((i >> 16) & 0xFF);
        buf[3] = (byte) ((i >> 24) & 0xFF);

        return buf;
    }

    public static boolean ip4Cmp(byte[] ip1, byte[] ip2) {
        if (ip1 == null || ip1.length != 4 || ip2 == null || ip2.length != 4) {
            return false;
        }

        return ip1[0] == ip2[0] && ip1[1] == ip2[1] && ip1[2] == ip2[2] && ip1[3] == ip2[3];
    }

    public static String concatStrings(String[] strings) {
        String res = null;

        if (strings != null) {
            StringBuilder sb = new StringBuilder();

            if (strings.length > 0 && strings[0] != null)
                sb.append(strings[0].trim());

            for (int i = 1; i < strings.length; i++) {
                if (strings[i] != null)
                    sb.append(',').append(strings[i].trim());
            }

            res = sb.toString();
        }

        return res;
    }

    public static boolean isNumber(String s) {
        if (s == null)
            return false;

        char ch;

        for (int i = 0; i < s.length(); ++i) {
            ch = s.charAt(i);
            if (ch < '0' || ch > '9')
                return false;
        }

        return true;
    }

    // TODO XXX move in native
    public static String getMainDomain(String domain) {
        if (LibNative.asciiStartsWith("www.", domain) && domain.length() > 4)
            domain = domain.substring(4);

        if (!isIp(domain)) {
            String[] parts = domain.split("\\.");
            if (parts.length >= 2)
                return parts[parts.length - 2] + "." + parts[parts.length - 1];
        }

        return domain;
    }

    public static String getThirdLevelDomain(String domain) {
        if (LibNative.asciiStartsWith("www.", domain) && domain.length() > 4)
            domain = domain.substring(4);

        if (!isIp(domain)) {
            String[] parts = domain.split("\\.");
            if (parts.length >= 2) {
                if (parts.length == 2)
                    return parts[parts.length - 2] + "." + parts[parts.length - 1];
                else if (parts.length >= 3)
                    return parts[parts.length - 3] + "." + parts[parts.length - 2] + "." + parts[parts.length - 1];
            }
        }

        return domain;
    }

    public static boolean isIp(String domain) {
        if (domain == null) return false;

        for (int i = 0; i < domain.length(); i++) {
            char c = domain.charAt(i);
            if (c != '.' && (c < '0' || c > '9'))
                return false;
        }

        return true;
    }

    public static boolean isHttpRequestLine(String s) {
        return (s != null && (LibNative.asciiStartsWith("GET ", s) || LibNative.asciiStartsWith("POST ", s) ||
                LibNative.asciiStartsWith("HEAD ", s) || LibNative.asciiStartsWith("PUT ", s) ||
                LibNative.asciiStartsWith("DELETE ", s) || LibNative.asciiStartsWith("OPTIONS ", s) ||
                LibNative.asciiStartsWith("TRACE ", s)));
    }

    public static byte[] concatArrays(byte[] a1, byte[] a2) {
        if (a1 == null || a2 == null)
            throw new NullPointerException();

        byte[] res = new byte[a1.length + a2.length];

        System.arraycopy(a1, 0, res, 0, a1.length);
        System.arraycopy(a2, 0, res, a1.length, a2.length);

        return res;
    }

    public static int indexOf(byte[] array, byte[] target, int start, int maxpos) {
        if (target.length == 0 || array.length < target.length || array.length < start) // TODO XXX
            return -1;

        maxpos = Math.min(maxpos, array.length - target.length + 1);

        indexOf_outer:
        for (int i = start; i < maxpos; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j])
                    continue indexOf_outer;
            }

            return i;
        }

        return -1;
    }

    public static byte[] deflate(byte[] data) {
        if (data == null)
            return null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(baos, new Deflater(Deflater.BEST_COMPRESSION, true));

        try {
            dos.write(data);
            dos.finish();
            dos.flush();
            baos.flush();

            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                dos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static PublicKey getPublicKeyFromAsset(String fileName) {
        try {
            // Loading certificate file
            InputStream inStream = App.getContext().getAssets().open(fileName);
            byte[] key = new byte[1024]; // TODO XXX
            int size = 0;

            while (true) {
                int result = inStream.read(key, size, key.length - size);
                if (result == -1)
                    break;
                size += result;
            }
            inStream.close();
            if (size == 0)
                return null;

            byte[] decodedKey = Base64.decode((new String(key, 0, 0, size)).replaceAll("\\s", ""), Base64.DEFAULT);
            X509EncodedKeySpec x509 = new X509EncodedKeySpec(decodedKey);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(x509);

            // Read the public key from certificate file
            //pubKey = (RSAPublicKey) cert.getPublicKey();
            //L.d(Settings.TAG_UTILS, "Public Key Algorithm = " + cert.getPublicKey().getAlgorithm() + "\n");
        } catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String toHex(byte[] data) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < data.length; i++) {
            int b = data[i] & 0xff;
            String str = Integer.toHexString(b);
            if (str.length() < 2)
                sb.append('0');
            sb.append(str);
        }

        return sb.toString();
    }

    public static void saveFile(InputStream is, String path) throws IOException {
        byte[] buf = new byte[4096];
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(path);
            int read;
            while ((read = is.read(buf)) > 0)
                fos.write(buf, 0, read);
        } finally {
            if (fos != null)
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public static void saveFile(byte[] data, String path) throws IOException {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(path);
            fos.write(data, 0, data.length);
        } finally {
            if (fos != null)
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public static String getNameOnly(String fileNameWithExt) {
        if (fileNameWithExt == null)
            return null;

        final int pos = fileNameWithExt.indexOf('.');
        if (pos <= 0)
            return fileNameWithExt;

        return fileNameWithExt.substring(0, pos);
    }

    public static byte[] getFileContentsRaw(String fileName) throws IOException {
        FileInputStream fis = null;
        ByteArrayOutputStream baos = null;

        try {
            fis = new FileInputStream(fileName);
            baos = new ByteArrayOutputStream();
            byte[] buf = new byte[2048];

            int read;
            while ((read = fis.read(buf)) > 0)
                baos.write(buf, 0, read);
        } catch (FileNotFoundException e) {
            L.w(Settings.TAG_UTILS, "File not found: ", fileName);
            return null;
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (baos != null)
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        if (baos == null)
            return null;

        return baos.toByteArray();
    }

    public static String getFileContents(String fileName) throws IOException {
        final byte[] content = getFileContentsRaw(fileName);
        if (content == null)
            return null;

        return (new String(content)); // TODO XXX may be use String(content, 0, 0, len)?
    }

    public static String getAssetAsString(Context context, String fileName) {
        try {
            InputStream inStream = context.getAssets().open(fileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(150);

            int read;
            byte[] buf = new byte[4096];
            while ((read = inStream.read(buf)) > 0)
                baos.write(buf, 0, read);

            inStream.close();

            return (baos.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean canConnect(String host, int port) {
        try {
            Socket s = new Socket(host, port);
            s.close();

            return true;
        } catch (IOException e) { /*e.printStackTrace();*/ }

        return false;
    }

    public static boolean hasPermissions(Context c, String[] permissions, String packageName) {
        PackageManager pm = c.getPackageManager();

        for (int i = 0; i < permissions.length; i++) {
            if (pm.checkPermission(permissions[i], packageName) == PackageManager.PERMISSION_DENIED)
                return false;
        }

        return true;
    }

    /*
     * return false if dialog.show don't called (activity finished)
     * TODO we can use SYSTEM_ALERT_WINDOW dialog in some cases:
     *	  http://stackoverflow.com/questions/2634991/android-1-6-android-view-windowmanagerbadtokenexception-unable-to-add-window
     */
    public static boolean dialogShow(Activity activity, Dialog dialog) {
        if (activity != null && activity.isFinishing())
            return false; // dialog or activity canceled

        // TODO XXX workaround for
        // android.view.WindowManager$BadTokenException: Unable to add window - token android.os.BinderProxy@42e93628 is not valid; is your activity running?
        try {
            dialog.show();
        } catch (BadTokenException e) {
            e.printStackTrace();
        }

        return true;
    }

    public static void runOnUiThread(Activity activity, Runnable action) {
        if (activity == null)
            return;

        activity.runOnUiThread(action);
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    // TODO used only for debug.db
    public static byte[] getData(String addr) {
        byte[] res = null;
        HttpURLConnection con = null;
        BufferedInputStream in = null;

        try {
            // see Utils.postData (TODO XXX merge all code)
            URL url = new URL(addr);

            //con = (HttpURLConnection) url.openConnection();
            final OkUrlFactory factory = new OkUrlFactory(new OkHttpClient());
            con = factory.open(url);

            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setUseCaches(false);
            con.setDefaultUseCaches(false);
            con.setRequestProperty("Connection", "close");
            con.setRequestProperty("Cache-Control", "no-cache");
            con.setRequestProperty("Accept-Encoding", "");
            //System.setProperty("http.keepAlive", "false");

            try {
                con.connect();
            } catch (SecurityException ex) {
                return res;
            }

            if (con.getResponseCode() == 200) {
                int contentLength = con.getContentLength();
                //String transferEncoding = con.getHeaderField("Transfer-Encoding");
                //boolean canRead = (contentLength > 0 || (transferEncoding != null && transferEncoding.trim().equals("chunked")));
                //if (Settings.DEBUG) L.e(Settings.TAG_UTILS, "postData ", "Content-Length: ", Integer.toString(contentLength));

                try {
                    in = new BufferedInputStream(con.getInputStream());
                } catch (IOException e) {
                }

                if (in != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream((contentLength > 0) ? contentLength : 4096);
                    byte[] buf = new byte[4096];
                    int read;
                    while ((read = in.read(buf)) != -1)
                        baos.write(buf, 0, read);

                    res = baos.toByteArray();
                }
            } else {
                res = new byte[0];
                if (Settings.DEBUG)
                    L.e(Settings.TAG_UTILS, "getData ", "ResponseCode: ", Integer.toString(con.getResponseCode()));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            if (con != null)
                con.disconnect();
        }

        return res;
    }

    // TODO used only by inAppBilling
    public static UtilsHttpResult postData(String addr, byte[] data) {
        return postData(addr, data, 15000); // TODO XXX 15 sec, maybe low?
    }

    public static UtilsHttpResult postData(String addr, byte[] data, int timeout) {
        UtilsHttpResult res = new UtilsHttpResult();
        HttpURLConnection con = null;
        OutputStream out = null;
        BufferedInputStream in = null;

        try {
            URL url = new URL(addr);

            // resolve ip manually because DNS requests by HTTP class
            // can be unresolved during vpn start/restart
            String host = url.getHost();
            String ip = null;
            if (host != null)
                ip = NetUtil.lookupIp(host, 3);

            //con = (HttpURLConnection) url.openConnection();
            final OkUrlFactory factory = new OkUrlFactory(new OkHttpClient());
            con = factory.open(url, ip); // use patched OkHttp and OkIo to set server ip address

            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setUseCaches(false);
            con.setDefaultUseCaches(false);
            con.setRequestProperty("Connection", "close");
            con.setRequestProperty("Cache-Control", "no-cache");
            con.setRequestProperty("Accept-Encoding", "");
            //System.setProperty("http.keepAlive", "false");
            if (timeout > 0) {
                con.setConnectTimeout(timeout);
                con.setReadTimeout(timeout);
            }

            if (data != null) {
                con.setDoOutput(true);
                con.setRequestProperty("Content-Length", Integer.toString(data.length));
                //con.setFixedLengthStreamingMode(data.length);
                //con.setRequestProperty("Content-Type", "application/octet-stream");
                //con.setRequestProperty("Content-Type", "application/stat_data");
                //con.setRequestProperty("Content-Type", "text/plain");
                //con.setRequestProperty("Content-Type", "multipart/form-data");
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            }

            // see UpdaterService.sendData
            try {
                con.connect();
            } catch (SecurityException ex) {
                return res;
            }

            if (data != null) {
                out = new BufferedOutputStream(con.getOutputStream());
                out.write(data);
                out.flush();
                out.close();
                out = null;
            }

            int code = con.getResponseCode();
            byte[] responseData = null;

            //if (code == 200)
            {
                int contentLength = con.getContentLength();
                //String transferEncoding = con.getHeaderField("Transfer-Encoding");
                //boolean canRead = (contentLength > 0 || (transferEncoding != null && transferEncoding.trim().equals("chunked")));
                //if (Settings.DEBUG) L.e(Settings.TAG_UTILS, "postData ", "Content-Length: ", Integer.toString(contentLength));

                try {
                    in = new BufferedInputStream(con.getInputStream());
                } catch (IOException e) {
                }
                if (in == null)
                    in = new BufferedInputStream(con.getErrorStream()); // some HTTP codes treated as error

                // TODO XXX if no any data???

                ByteArrayOutputStream baos = new ByteArrayOutputStream((contentLength > 0) ? contentLength : 128);
                byte[] buf = new byte[4096];
                int read;
                while ((read = in.read(buf)) != -1)
                    baos.write(buf, 0, read);

                responseData = baos.toByteArray();
            }
            //else
            //{
            //	  //rdata = new byte[0];
            //	  if (Settings.DEBUG) L.e(Settings.TAG_UTILS, "postData ", "ResponseCode: ", Integer.toString(con.getResponseCode()));
            //}

            res = new UtilsHttpResult(code, responseData);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            if (con != null)
                con.disconnect();
        }

        return res;
    }

    // return path where we can create file: sdcard, tmp, app data dir
    public static String getWritablePath(String filename) {
        String filePath = null;

        File sdcard = Environment.getExternalStorageDirectory();
        File f = new File(sdcard.getPath() + "/" + filename);
        File tmp = new File("/data/local/tmp");
        File ft = new File("/data/local/tmp/" + filename); // adb shell chmod 0777 /data/local/tmp/

        if ((sdcard.isDirectory() && sdcard.canWrite()) || f.canWrite())
            // can write to sdcard
            filePath = f.getAbsolutePath();
        else if ((tmp.isDirectory() && tmp.canWrite()) || ft.canWrite())
            // can write to tmp
            // adb shell chmod 0777 /data/local/tmp/
            filePath = ft.getAbsolutePath();
        else
            // adb backup -f backup.ab -noapk app.webdefender
            // ( printf "\x1f\x8b\x08\x00\x00\x00\x00\x00" ; tail -c +25 backup.ab ) | tar xfvz -
            filePath = App.getContext().getFilesDir().getAbsolutePath() + "/" + filename;

        return filePath;
    }

    public static int unsignedByte(byte b) {
        return (b & 0xFF);
    }

    // android docs tells that we can set priority < 0 manually, but we can =)
    public static void maximizeThreadPriority() {
        //final int priorityNew = Process.THREAD_PRIORITY_DEFAULT; // 0
        //final int priorityNew = Process.THREAD_PRIORITY_FOREGROUND; // -2
        final int priorityNew = Process.THREAD_PRIORITY_DISPLAY; // -4
        //final int priorityNew = Process.THREAD_PRIORITY_AUDIO; // -16, too brutal

        try {
            int tid = Process.myTid();
            int priorityOld = Process.getThreadPriority(tid);
            if (priorityOld > priorityNew) // lower is better
            {
                Process.setThreadPriority(priorityNew);
                //L.a(Settings.TAG_UTILS, "PRIORITY ", "new max " + Process.getThreadPriority(tid));
            }
        } catch (Exception e) {
        }
    }

    public static void minimizeThreadPriority() {
        final int priorityNew = Process.THREAD_PRIORITY_BACKGROUND; // 10
        //final int priorityNew = Process.THREAD_PRIORITY_LOWEST; // 19, too brutal

        try {
            int tid = Process.myTid();
            int priorityOld = Process.getThreadPriority(tid);
            if (priorityOld < priorityNew) // lower is better
            {
                Process.setThreadPriority(priorityNew);
                //L.a(Settings.TAG_UTILS, "PRIORITY ", "new min " + Process.getThreadPriority(tid));
            }
        } catch (Exception e) {
        }
    }

    public static void activityReload(Activity activity) {
        Intent intent = activity.getIntent();

        activity.overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.finish();
        activity.overridePendingTransition(0, 0);

        //Context context = activity.getApplicationContext();
        activity.startActivity(intent);
    }

    /*
     * clear all applications cache (return false on error)
     *
     * http://stackoverflow.com/questions/17313721/how-to-delete-other-applications-cache-from-our-android-app
     * http://stackoverflow.com/questions/14507092/android-clear-cache-of-all-apps
     * https://phonesecurity.googlecode.com/svn/trunk/FastAppMgr/src/com/herry/fastappmgr/view/CacheAppsListActivity.java
     *
     * may not clean all apps:
     * W/PackageManager(  730): Couldn't clear application caches
     */
    //public static boolean clearCaches(IPackageDataObserver clearCacheObserver)
    public static boolean clearCaches() {
        try {
            Context context = App.getContext();
            PackageManager pm = context.getPackageManager();
            Method[] methods = pm.getClass().getDeclaredMethods();

            for (Method m : methods) {
                if (!m.getName().equals("freeStorage"))
                    continue;

                try {
                    long desiredFreeStorage = Long.MAX_VALUE; // request max of free space
                    m.invoke(pm, desiredFreeStorage, null);

                    return true;
                } catch (Exception e) { /* method invocation failed (permission?)*/ }

                break;
            }
        } catch (Exception e) {
        }

        return false;
    }

    /*
     * kills all packages running in background (return false on error)
     *
     * http://stackoverflow.com/questions/7397668/how-to-kill-all-running-applications-in-android
     *
     * TODO XXX may be slow? test with 100 applications!
     */
    public static void killBackgroundApps() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Context context = App.getContext();
                    PackageManager packageManager = context.getPackageManager();
                    List<ApplicationInfo> packages = packageManager.getInstalledApplications(0); // get a minimal list of installed apps
                    String myPackageName = context.getPackageName();

                    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

                    for (ApplicationInfo packageInfo : packages) {
                        final String pkgName = packageInfo.packageName;
                        if (pkgName == null || pkgName.indexOf('.') < 0 || Objects.equals(myPackageName, pkgName)) {
                            continue;
                        }
                        activityManager.killBackgroundProcesses(pkgName);
                    }
                } catch (Exception ignored) {
                }
            }
        });
        t.setName("killApps");
        t.start();
    }

    public static String byteCountToHuman(long bytes) {
        return FileUtils.byteCountToDisplaySize(bytes);
    }

}
