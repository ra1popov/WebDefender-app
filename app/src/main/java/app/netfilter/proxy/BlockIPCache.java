package app.netfilter.proxy;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import app.common.LibNative;
import app.common.LibTypes;
import app.common.Utils;
import app.security.PolicyRules;

public class BlockIPCache {

    private static HashSet<IP> ips = new HashSet<IP>(100); // black Ip and domain set
    private static Hashtable<IP, PolicyRules> rules = new Hashtable<IP, PolicyRules>(100);
    private static long lastUpdateTime = 0;
    private static ByteBuffer ips_c = LibTypes.intSetCreate(); // clean Ip set
    private static boolean clearOnUpdate = false;

    public static void addIp(String domain, byte[] ip, long ttl, PolicyRules rules) {
        boolean compressed = rules != null && rules.hasPolicy(PolicyRules.COMPRESSED);

        //L.w(Settings.TAG_BLOCKIPCACHE, "Adding IP: " + Utils.ipToString(ip, 0), " for domain: ", domain);
        if (ip != null && (compressed || !containsClean(ip))) {
            if (compressed && ttl < 2678400) // 31*24*60*60 TODO XXX ooh my god, f***ing spike, fix this
                ttl = 2678400;

            IP key = new IP(domain, ip, System.currentTimeMillis() + ttl * 1000);
            ips.remove(key);
            ips.add(key);
            BlockIPCache.rules.put(key, rules);
        }
    }

    public static void addCleanIp(byte[] ip) {
        if (ip == null)
            return;

        boolean res = LibTypes.intSetAdd(ips_c, Utils.ipToInt(ip));
        if (!res)
            return;

        IP key = new IP(null, ip, 0);
        PolicyRules tmp = rules.get(key);
        if (tmp != null && !tmp.hasPolicy(PolicyRules.COMPRESSED)) {
            ips.remove(key);
            rules.remove(key);
        }
    }

    public static boolean contains(byte[] ip) {
        //L.w(Settings.TAG_BLOCKIPCACHE, "Asking IP: " + Utils.ipToString(ip, 0));
        boolean res = ips.contains(new IP(null, ip, 0));
        //L.w(Settings.TAG_BLOCKIPCACHE, "Asking IP: " + Utils.ipToString(ip, 0) + " found: " + res);

        return res;
    }

    public static boolean containsClean(byte[] ip) {
        //L.w(Settings.TAG_BLOCKIPCACHE, "Asking clean IP: " + Utils.ipToString(ip, 0));
        boolean res = LibTypes.intSetContains(ips_c, Utils.ipToInt(ip));
        //L.w(Settings.TAG_BLOCKIPCACHE, "Asking clean IP: " + Utils.ipToString(ip, 0) + " found: " + res);

        return res;
    }

    public static void clear() {
        ips.clear();
        rules.clear();
        // TODO add ips_c clean
    }

    public static void clearOnUpdate() {
        clearOnUpdate = true;
    }

    public static PolicyRules getPolicy(byte[] ip) {
        return rules.get(new IP(null, ip, 0));
    }

    public static String getDomain(byte[] ip) {
        IP test = new IP(null, ip, 0);
        for (IP i : ips) {
            if (i.domain != null && test.equals(i)) {
                return i.domain;
            }
        }

        return null;
    }

    /**
     * Checks if the given IP belongs to the given domain
     *
     * @param ip     - ip to check
     * @param domain - domain to check. If the domain starts with "." then it returns
     *               true if this IP belongs to any subdomain of given domain
     * @return true if the IP belongs to that domain, false otherwise.
     */
    public static boolean isDomain(byte[] ip, String domain) {
        IP test = new IP(null, ip, 0);
        for (IP i : ips) {
            if (i.domain == null)
                continue;

            if (test.equals(i)) {
                if (LibNative.asciiStartsWith(".", domain) && LibNative.asciiEndsWith(domain, i.domain))
                    return true;
                if (domain.equals(i.domain))
                    return true;
            }
        }

        return false;
    }

    public static void update() {
        long time = System.currentTimeMillis();
        if (time - lastUpdateTime < 60000) // 1 min
            return;
        lastUpdateTime = time;

        if (clearOnUpdate) {
            clear();
            clearOnUpdate = false;
        }

        ArrayList<IP> toDelete = new ArrayList<IP>();
        for (IP ip : ips) {
            if (ip.time < time)
                toDelete.add(ip);
        }

        ips.removeAll(toDelete);
        for (IP key : toDelete)
            rules.remove(key);
    }

    //

    static class IP {
        public byte[] ip;
        public long time;
        public String domain = null;

        public IP(String domain, byte[] ip, long time) {
            this.domain = domain;
            this.ip = ip;
            this.time = time;
        }

        @Override
        public int hashCode() {
            if (ip == null || ip.length != 4)
                return 0;

            return ((ip[0] & 0xff) << 24) & ((ip[1] & 0xff) << 16) & ((ip[2] & 0xff) << 8) & (ip[3] & 0xff);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (!(o instanceof IP))
                return false;

            IP test = (IP) o;
            return (ip[0] == test.ip[0] && ip[1] == test.ip[1] && ip[2] == test.ip[2] && ip[3] == test.ip[3]);
        }
    }
}
