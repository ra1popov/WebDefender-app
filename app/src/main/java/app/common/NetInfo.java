package app.common;

import static android.os.Process.INVALID_UID;
import static android.system.OsConstants.IPPROTO_TCP;
import static android.system.OsConstants.IPPROTO_UDP;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.net.InetSocketAddress;

import app.App;
import app.common.debug.L;
import app.internal.Settings;


public class NetInfo {

    public final byte[] localIp;
    public final int localPort;
    public final byte[] serverIp;
    public final int serverPort;
    public final int state;
    public final int uid;

    private static boolean netlinkTested = false;
    private static boolean netlinkUse = false;

    private static int netlinkErrCount = 0; // may be use AtomicInteger?
    private static int netlinkNotFoundCount = 0;
    private static int procRetryCount = 0;
    private static int procNotFoundCount = 0;

    private NetInfo(byte[] serverIp, int serverPort, byte[] localIp, int localPort, int state, int uid) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.localIp = localIp;
        this.localPort = localPort;
        this.state = state;
        this.uid = uid;
    }

    public static int findMatchingUidInTcp(int localPort, byte[] remoteIp, int remotePort) {
        int res;

        if (Settings.TCPIP_CON_USE_NETLINK) {
            if (!netlinkTested) {
                netlinkTested = true;
                netlinkUse = LibNative.netlinkIsWork();
            }

            if (netlinkUse) {
                res = LibNative.netlinkFindUid(localPort, remoteIp, remotePort);
                if (res != -2) {
                    if (res == -1)
                        netlinkNotFoundCount++;

                    //L.d(Settings.TAG_NETINFO, "uid from netlink: " + res);
                    return res;
                }

                netlinkErrCount++;
            }
        }

        // netlink subsystem not available try to parse /proc

        res = NetLine.getUidFromProc(remoteIp, remotePort, localPort, true);
        if (res < 0) {
            procRetryCount++;
            L.d(Settings.TAG_NETINFO, "Not found uid in proc!");

            // wait, and try again because /proc slow update
            // TODO XXX may be other timeout?

            Utils.sleep(50);
            res = NetLine.getUidFromProc(remoteIp, remotePort, localPort, false);

            if (res < 0) {
                procNotFoundCount++;
                L.d(Settings.TAG_NETINFO, "Not found uid in proc second time!");
            }
        }

        return res;
    }

    // for Firewall support in Android 10
    // https://stackoverflow.com/questions/58497492/acccess-to-proc-net-tcp-in-android-q
    // https://github.com/M66B/NetGuard/blob/053c11dc1d1e54ecc244b69084ffb6f1cf107e23/app/src/main/java/eu/faircode/netguard/ServiceSinkhole.java
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static int findMatchingUidInTcpQ(byte[] localIp, int localPort, byte[] remoteIp, int remotePort) {
        String _localIp = NetLine.getIpString(localIp);
        String _remoteIp = NetLine.getIpString(remoteIp);

        InetSocketAddress remoteInetSocketAddress = new InetSocketAddress(_remoteIp, remotePort);
        InetSocketAddress localInetSocketAddress = new InetSocketAddress(_localIp, localPort);

        ConnectivityManager connectivityManager = (ConnectivityManager) App.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return 0;
        }

        int uid = connectivityManager.getConnectionOwnerUid(IPPROTO_TCP, localInetSocketAddress, remoteInetSocketAddress);
        if (uid == INVALID_UID) {
            uid = connectivityManager.getConnectionOwnerUid(IPPROTO_UDP, localInetSocketAddress, remoteInetSocketAddress);
        }

        if (uid != INVALID_UID) {
            return uid;
        }

        return 0;
    }

    public static int[] statsGetInfo() {
        int[] res = new int[4];
        res[0] = netlinkErrCount;
        res[1] = netlinkNotFoundCount;
        res[2] = procRetryCount;
        res[3] = procNotFoundCount;

        return res;
    }

}
