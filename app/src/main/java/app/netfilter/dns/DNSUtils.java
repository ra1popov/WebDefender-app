package app.netfilter.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;

import app.common.debug.L;
import app.internal.Settings;

public class DNSUtils {

    public static boolean resolve(String domain) {
        try {
            final InetAddress address = InetAddress.getByName(domain);
            return true;
        } catch (UnknownHostException e) {
            //e.printStackTrace();
            L.e(Settings.TAG_DNSUTILS, e.getMessage());
            return false;
        } catch (SecurityException e) {
            // in case of disabling background data
            e.printStackTrace();
            return false;
        }
    }

}
