package app.common;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import app.common.debug.L;
import app.internal.Settings;
import app.util.InetAddressUtils;

public class Usb {

    public static String getIPAddressUsb(final boolean useIPv4) {
        try {
            final List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (final NetworkInterface intf : interfaces) {
                final String name = intf.getDisplayName();
                //L.d(Settings.TAG_USB, "Interface " + name);

                if (!name.startsWith("usb") && !name.startsWith("rndis"))
                    continue;

                // find

                final List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (final InetAddress addr : addrs) {
                    final String sAddr = Objects.requireNonNull(addr.getHostAddress()).toUpperCase();
                    final boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                    if (useIPv4) {
                        if (isIPv4)
                            return sAddr;
                    } else {
                        if (!isIPv4) {
                            final int delim = sAddr.indexOf('%');
                            return ((delim < 0) ? sAddr : sAddr.substring(0, delim));
                        }
                    }
                }
            } // for
        } catch (final Exception ex) {
            // for now eat exceptions
        }

        return "";
    }

    /*
     * TODO XXX getIPAddressUsb -> getNetworkInterfaces VERY SLOW!
     * 11% getNetworkInterfaces -> getByName
     * http://osxr.org/android/source/libcore/luni/src/main/java/java/net/NetworkInterface.java#0266
     * http://osxr.org/android/source/libcore/luni/src/main/java/java/net/NetworkInterface.java#0106
     * http://osxr.org/android/source/libcore/luni/src/main/java/java/net/NetworkInterface.java#0123
     */
    public static boolean isUsbTethered() {
        String ipAddr = getIPAddressUsb(true);
        if (ipAddr.length() == 0) {
            L.d(Settings.TAG_USB, "Tethering not enabled");
            return false;
        } else {
            L.d(Settings.TAG_USB, "Tethering enabled");
            return true;
        }
    }

}
