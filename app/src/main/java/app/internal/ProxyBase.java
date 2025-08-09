package app.internal;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.ZoneTransferException;
import org.xbill.DNS.ZoneTransferIn;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import app.App;
import app.common.NetUtil;
import app.common.Utils;
import app.common.debug.L;
import app.netfilter.proxy.ProxyWorker;

public class ProxyBase {

    private static final String SERVERS = "servers";
    private static final String CUR_SERVER = "cur_server";
    private static final String TTL = "ttl";
    private static final String LAST_UPDATE_TIME = "last_update_time";

    private static final int NOTIFY_TYPE_AUTO = 1;
    private static final int NOTIFY_TYPE_COUNTRY = 2;

    private static final ArrayList<ProxyServer> servers = new ArrayList<>();
    private static ProxyServer currentServer = null;
    private static ProxyServer localServer = null;
    private static String currentCountry = null;
    private static long lastUpdateTime = 0;
    private static long lastUpdateTryTime = 0;
    private static long lastSelectTime = 0;
    private static long updateTryInterval = 30 * 1000; // 30 sec start interval
    private static long ttl = 0;
    private static boolean ready = false;
    private static int notifyingType = 0;
    private static String baseFileName = null;

    private static Context context = null;
    private static final Random random = new Random(System.currentTimeMillis());
    private static final ReentrantLock lock = new ReentrantLock();
    private static ProxyWorker worker = null;

    static {
        context = App.getContext();
        baseFileName = context.getFilesDir().getAbsolutePath() + "/proxy.json";
    }

    // TODO XXX this function time check incorrect (need to do as scheme)
    public static void updateServers(boolean force) {
        long t;
        boolean update = force;
        long time = System.currentTimeMillis();
        long ttlTime = lastUpdateTime + ttl * 1000;

        //if (ttl > 0) ttl = 60;

        if (!force) {
            // start update if proxy domain ttl expired and N sec from last check
            if (time > ttlTime) {
                t = lastUpdateTryTime + updateTryInterval;
                update = (time > t);
            }
        }

        // TODO XXX try to select servers if not ready (every 1 minute)
        if ((update || (!ready && time > lastSelectTime + 60 * 1000)) && servers.size() > 0) {
            String country = Preferences.get_s(Settings.PREF_PROXY_COUNTRY);
            if (country == null || currentCountry == null || !country.equals(currentCountry))
                currentCountry = country; // select server on first run or country change

            selectServer(null, isConnectAlive());
        }

        if (!update)
            return;

        // update

        // return if too many forces (1 min interval)
        t = lastUpdateTryTime + 60 * 1000;
        if (force && time < t)
            return;

        if (!TextUtils.isEmpty(Settings.WGPROXY_MAIN_DOMAIN)) {
            updateServers(Settings.WGPROXY_MAIN_DOMAIN, null, force);
        }
    }

    // TODO XXX servers var synchronization!
    private static void updateServers(final String domain, final Listener listener,
                                      final boolean force) {
        L.d(Settings.TAG_PROXYBASE, "updateServers");

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!lock.tryLock())
                    return;

                lastUpdateTryTime = System.currentTimeMillis();

                // wait for VPN to establish fully
                // TODO XXX sleep, f*ck
                Utils.sleep(5000);
                if (NetUtil.getStatus() == -1) {
                    updateTryInterval(false, force);
                    lock.unlock();
                    return;
                }

                //
                Record[] nameServers = null;
                for (int i = 0; i < 3; i++) {
                    nameServers = NetUtil.lookupNS(domain);
                    if (nameServers != null && nameServers.length > 0) {
                        break;
                    }
                    Utils.sleep(1000); // TODO XXX
                }
                if (nameServers == null || nameServers.length == 0) {
                    return;
                }

                //
                boolean done = false;

                try {
                    for (Record record : nameServers) {
                        NSRecord rec = (NSRecord) record;
                        final ZoneTransferIn axfr = ZoneTransferIn.newAXFR(new Name(domain), rec.getTarget().toString(), null);
                        axfr.run();
                        final List list = axfr.getAXFR();

                        if (list == null || list.size() == 0) {
                            continue;
                        }

                        synchronized (servers) {
                            servers.clear(); // clear

                            for (Object obj : list) {

                                Record soaRecord = (Record) obj;
                                if (!(soaRecord instanceof ARecord)) {
                                    continue;
                                }

                                String name = soaRecord.getName().toString(true); // omit final dot
                                String firstLabel = soaRecord.getName().getLabelString(0);

                                if (firstLabel.equals("proxy")) {
                                    ProxyServer serv = new ProxyServer(soaRecord.getName().getLabelString(1), name,
                                            ((ARecord) soaRecord).getAddress());
                                    servers.add(serv);
                                    done = true;
                                } else if (name.equals(domain)) {
                                    ttl = soaRecord.getTTL();
                                }

                            }
                        }

                        if (done) {
                            break;
                        }
                    } // for

                    if (servers.size() > 0) {
                        L.d(Settings.TAG_PROXYBASE, "Got servers!");

                        lastUpdateTime = System.currentTimeMillis();
                        selectServer(null, isConnectAlive()); // and save also
                    }

                } catch (SecurityException e) {
                    // In case of disabling background data
                    e.printStackTrace();
                } catch (UnresolvedAddressException | ZoneTransferException | IOException e) {
                    e.printStackTrace();
                } finally {
                    updateTryInterval(done, force);
                    lock.unlock();
                }
            }
        });
        t.start();
    }

    private static void updateTryInterval(boolean done, boolean force) {
        if (done) {
            updateTryInterval = 30000L; // if all ok reset interval
            return;
        }

        if (force) // don't touch interval on failed force update
            return;

        // 30, 60, 300 (5 min), 600 (10 min), 1800 (30 min), ttl

        if (updateTryInterval == 30000L) updateTryInterval = 60000L;
        else if (updateTryInterval == 60000L) updateTryInterval = 300000L;
        else if (updateTryInterval == 300000L) updateTryInterval = 600000L;
        else if (updateTryInterval == 600000L) updateTryInterval = 1800000L;
        else if (updateTryInterval == 1800000L) updateTryInterval = ttl * 1000;
    }

    private static boolean isConnectAlive() {
        boolean alive = (worker != null) ? worker.isConnectAlive() : false;
        if (worker == null) {
            int status = NetUtil.getStatus(false);
            alive = (status != -1);
        }

        return alive;
    }

    public static boolean load() {
        if (Preferences.get_s(Settings.PREF_PROXY_COUNTRY) == null)
            Preferences.putString(Settings.PREF_PROXY_COUNTRY, "auto");

        synchronized (servers) {
            if (Settings.DEBUG)
                L.d(Settings.TAG_PROXYBASE, "servers load(), servers = ", String.valueOf(servers.size()));

            try {
                String s = Utils.getFileContents(baseFileName);
                if (s == null)
                    s = Utils.getAssetAsString(context, "proxy.json");

                if (s != null) {
                    servers.clear(); // clear

                    JSONObject obj = new JSONObject(s);
                    if (obj.has(SERVERS)) {
                        JSONArray arr = obj.getJSONArray(SERVERS);
                        for (int i = 0; i < arr.length(); i++) {
                            final JSONObject sObj = arr.getJSONObject(i);
                            ProxyServer serv = new ProxyServer(sObj);
                            servers.add(serv);
                        }

                        JSONObject o = obj.optJSONObject(CUR_SERVER);
                        if (o != null) {
                            currentServer = new ProxyServer(o);
                            currentCountry = currentServer.country;
                        }
                        lastUpdateTime = obj.getLong(LAST_UPDATE_TIME);
                        ttl = obj.getLong(TTL);

                        return true;
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    public static void save() {
        JSONObject obj = new JSONObject();
        synchronized (servers) {
            if (servers.size() > 0) {
                JSONArray arr = new JSONArray();
                for (ProxyServer serv : servers)
                    arr.put(serv.getJSON(false));

                try {
                    obj.put(SERVERS, arr);
                    if (currentServer != null)
                        obj.put(CUR_SERVER, currentServer.getJSON(false));
                    obj.put(TTL, ttl);
                    obj.put(LAST_UPDATE_TIME, lastUpdateTime);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            try {
                Utils.saveFile(obj.toString().getBytes(), baseFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static CharSequence[] getAvailableServers() {
        ArrayList<CharSequence> list = new ArrayList<CharSequence>(1 + servers.size());
        HashSet<CharSequence> set = new HashSet<CharSequence>();
        CharSequence[] res = null;

        synchronized (servers) {
            if (Settings.DEBUG)
                L.d(Settings.TAG_PROXYBASE, "getAvailableServers(), servers = ", Integer.toString(servers.size()));

            list.add("auto");
            set.add("auto");

            for (ProxyServer serv : servers) {
                if (!set.contains(serv.country)) {
                    list.add(serv.country);
                    set.add(serv.country);
                }
            }

            Collections.sort(list, new Comparator<CharSequence>() {
                @Override
                public int compare(CharSequence lhs, CharSequence rhs) {
                    if (lhs.equals("auto"))
                        return -100;
                    else if (rhs.equals("auto"))
                        return 100;

                    return lhs.toString().compareTo(rhs.toString());
                }
            });

            res = new CharSequence[list.size()];
            list.toArray(res);
        }

        return res;
    }

    private static void selectServer(ProxyServer dead, boolean isConnectAlive) {
        synchronized (servers) {
            lastSelectTime = System.currentTimeMillis();
            currentServer = null;

            if (servers.size() > 0) {
                if (currentCountry == null || currentCountry.equals("auto")) {
                    selectRandom(isConnectAlive);
                } else {
                    boolean found = false;
                    for (ProxyServer serv : servers) {
                        if (serv.country.equals(currentCountry)) {
                            currentServer = serv;
                            found = true;
                            if (random.nextInt(100) < 50)
                                break;
                        }
                    }

                    if (!found) {
                        if (isConnectAlive)
                            notifyNoCountryServers();
                    }
                }

                save();
            }

            ready = (currentServer != null);
            if (Settings.DEBUG) {
                if (ready)
                    L.e(Settings.TAG_PROXYBASE, "selectedServer: ", currentServer.toString());
                else
                    L.e(Settings.TAG_PROXYBASE, "selectedServer: null");
            }

        }
    }

    private static void selectRandom(boolean isConnectAlive) {
        synchronized (servers) {
            currentServer = null;
            int rand = random.nextInt(servers.size());
            int count = 0;
            ProxyServer server = null;

            while (currentServer == null && count < 3) {
                if (rand >= 0 && rand < servers.size()) {
                    server = servers.get(rand);

                    if (Settings.DEBUG)
                        L.e(Settings.TAG_PROXYBASE, "Checking proxy on: ", server.toString());

                    if (!server.country.equals("auto")) {
                        count++;
                        rand = random.nextInt(servers.size());
                        continue;
                    }

                    currentServer = server;
                }

                count++;
            }

            // if didn't find server by random we make full search for undead server :)
            if (currentServer == null) {
                for (ProxyServer serv : servers) {
                    if (!serv.country.equals("auto"))
                        continue;

                    currentServer = serv;
                    break;
                }
            }
        }

        if (currentServer == null && isConnectAlive)
            notifyNoAutoServers();
    }

    public static ProxyServer getCurrentServer() {
        if (currentServer == null)
            selectServer(null, isConnectAlive());

        return currentServer;
    }

    public static ProxyServer getLocalServer() {
        if (localServer != null)
            return localServer;

        try {
            localServer = new ProxyServer("local", null, InetAddress.getByAddress("192.168.1.9", new byte[]{192 - 256, 168 - 256, 1, 9}));

            return localServer;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static void notifyNoAutoServers() {
        if (notifyingType != NOTIFY_TYPE_AUTO) {
            notifyingType = NOTIFY_TYPE_AUTO;
        }
    }

    private static void notifyNoCountryServers() {
        if (notifyingType != NOTIFY_TYPE_COUNTRY) {
            notifyingType = NOTIFY_TYPE_COUNTRY;
        }
    }

    public static void notifyServersUp() {
        notifyingType = 0;

    }

    public static void setCurrentCountry(String country) {
        currentCountry = country;
        synchronized (servers) {
            currentServer = null;
        }
    }

    public static boolean isReady() {
        int size = 0;
        synchronized (servers) {
            size = servers.size();
        }

        return (ready && size > 0);
    }

    public static void setWorker(ProxyWorker worker) {
        ProxyBase.worker = worker;
    }

    public interface Listener {
        void onServersInited();
    }

    //

    public static class ProxyServer {

        private static final String COUNTRY = "country";
        private static final String DOMAIN = "domain";
        private static final String TTL = "ttl";
        private static final String IP = "ip";

        public String country;
        public String domain;
        public byte[] ip;
        private InetAddress addr;

        public ProxyServer(String country, String domain, InetAddress addr) {
            this.country = country;
            this.domain = domain;
            this.ip = addr.getAddress();
            this.addr = addr;
        }

        public ProxyServer(JSONObject obj) {
            if (obj == null)
                return;

            try {
                country = obj.getString(COUNTRY);
                domain = obj.getString(DOMAIN);
                int int_ip = obj.getInt(IP);
                ip = Utils.intToIp(int_ip);
                addr = InetAddress.getByAddress(domain, ip);

                if (Settings.DEBUG) L.e(Settings.TAG_PROXYBASE, "Loaded: ", addr.toString());
            } catch (JSONException | UnknownHostException e) {
                e.printStackTrace();
            }
        }

        public InetAddress getAddr() {
            return addr;
        }

        public JSONObject getJSON(boolean stringIp) {
            JSONObject obj = new JSONObject();

            try {
                obj.put(COUNTRY, country);
                obj.put(DOMAIN, domain);
                if (stringIp)
                    obj.put(IP, Utils.ipToString(ip, 0));
                else
                    obj.put(IP, Utils.ipToInt(ip));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return obj;
        }

        @NonNull
        @Override
        public String toString() {
            return getJSON(true).toString();
        }
    }

}
