package app.common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.annotation.NonNull;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;

import app.common.debug.L;
import app.internal.Settings;

public class NetUtil {

    private static ConnectivityManager cm = null;
    private static WifiManager wm = null;

    public static void init(Context context) {
        if (cm == null)
            cm = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (wm == null)
            wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public static NetworkInfo[] getAllNetworkInfo() {
        if (cm == null)
            return null;

        return cm.getAllNetworkInfo();
    }

    /*
     * return -1 if no network or -2 on tethering
     * use ConnectivityManager, for types see:
     * http://tools.oesf.biz/android-4.4.4_r1.0/xref/frameworks/base/core/java/android/net/ConnectivityManager.java#702
     *
     * TODO XXX use getStatus(false) if tethering not need (see isUsbTethered)
     */
    public static synchronized int getStatus() {
        //return getStatus(true);
        return getStatus(false);
    }

    public static synchronized int getStatus(boolean checkTether) {
        if (cm == null)
            return 0; // ooups, return net ok

        if (isSharingWiFi(wm) || (checkTether && Usb.isUsbTethered())) {
            L.d(Settings.TAG_NETUTIL, "Tethering detected!");
            return -2;
        }

        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null)
            return info.getType();

        L.e(Settings.TAG_NETUTIL, "No active networks!");
        return -1;
    }

    public static synchronized boolean isMobile() {
        if (cm == null)
            return true;

        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null)
            return (info.getType() == ConnectivityManager.TYPE_MOBILE);

        return false;
    }

    public static boolean isSharingWiFi(final WifiManager manager) {
        if (manager == null)
            return false;

        try {
            final Method method = manager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true); //in the case of visibility change in future APIs

            return (Boolean) method.invoke(manager);
        } catch (final Throwable ex) {

            ex.printStackTrace();

            Throwable cause = ex.getCause();
            if (cause != null)
                cause.printStackTrace();
            else
                L.d("NetUtil", "cause is null");
        }

        return false;
    }

    @NonNull
    public static ArrayList<String> getDNSServers() {
        ArrayList<String> servers = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            L.d(Settings.TAG_NETUTIL, "DNS Google: ", "8.8.8.8");
            L.d(Settings.TAG_NETUTIL, "DNS Google: ", "8.8.4.4");
            servers.add("8.8.8.8");
            servers.add("8.8.4.4");

        } else {

            Class<?> SystemProperties;
            try {
                SystemProperties = Class.forName("android.os.SystemProperties");
                Method method = SystemProperties.getMethod("get", new Class[]{String.class});

                for (String name : new String[]{"net.dns1", "net.dns2", "net.dns3", "net.dns4"}) {
                    String value = (String) method.invoke(null, name);
                    if (value != null && !value.isEmpty() && !servers.contains(value)) {
                        L.d(Settings.TAG_NETUTIL, "DNS: ", value);
                        servers.add(value);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return servers;

    }

    public static Record[] lookupNS(String domain) {
        Record[] records = null;

        try {
            records = new Lookup(domain, Type.NS).run();

            if (Settings.DEBUG) {
                for (int i = 0; records != null && i < records.length; i++) {
                    NSRecord ns = (NSRecord) records[i];
                    L.e(Settings.TAG_NETUTIL, "Nameserver: ", ns.getTarget().toString());
                }
            }

        } catch (TextParseException | ExceptionInInitializerError e) {
            e.printStackTrace();
        }

        // bug fix - google dns (8.8.8.8 and 8.8.4.4) does not return the list of authoritative name servers https://developers.google.com/speed/public-dns/
        if (records == null || records.length == 0) {
            try {
                NSRecord ns1 = new NSRecord(new Name(domain + "."), DClass.IN, 1591, new Name("ns1." + domain + "."));
                NSRecord ns2 = new NSRecord(new Name(domain + "."), DClass.IN, 1591, new Name("ns2." + domain + "."));
                records = new Record[2];
                records[0] = ns1;
                records[1] = ns2;
            } catch (TextParseException e) {
                e.printStackTrace();
            }
        }

        return records;
    }

    public static String lookupIp(String domain) {
        String ip = null;

        try {
            Record[] records = new Lookup(domain, Type.A).run();
            if (records != null) {
                for (Record record : records) {
                    if (record instanceof ARecord) {
                        ARecord a = (ARecord) record;
                        InetAddress ia = a.getAddress();
                        if (ia == null) {
                            continue;
                        }

                        ip = ia.getHostAddress();
                        if (Settings.DEBUG) L.e(Settings.TAG_NETUTIL, "Ip: ", ip);
                        break;
                    }
                }
            }
        } catch (TextParseException | ExceptionInInitializerError e) {
            e.printStackTrace();
        }

        return ip;
    }

    public static String lookupIp(String domain, int attempts) {
        for (int i = 0; i < attempts; i++) {
            String ip = NetUtil.lookupIp(domain);
            if (ip != null)
                return ip;

            Utils.sleep(1000); // TODO XXX
        }

        return null;
    }

}
