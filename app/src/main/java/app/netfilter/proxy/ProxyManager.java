package app.netfilter.proxy;

import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import app.netfilter.IFilterVpnPolicy;
import app.netfilter.IHasherListener;
import app.netfilter.IPacketLogger;
import app.netfilter.IUserNotifier;

public class ProxyManager {

    private static final ProxyManager instance = new ProxyManager();
    private ProxyWorker worker = null;

    public static ProxyManager getInstance() {
        return instance;
    }

    public static boolean isStarted() {
        ProxyManager pm = getInstance();
        if (pm == null)
            return false;

        if (pm.worker != null && pm.worker.isStopped())
            pm.worker = null;

        return (pm.worker != null);
    }

    public boolean notifyBlockingRuleChanged() {
        if (worker != null) {
            worker.notifyBlockingRuleChanged();
            return true;
        }

        return false;
    }

    public void closeProxyConnections() {
        if (worker != null) {
            worker.closeProxyConnections();
        }
    }

    public void closeAllConnections() {
        if (worker != null) {
            worker.closeAllConnections();
        }
    }

    public void start(VpnService vpnService, ParcelFileDescriptor descriptor, IFilterVpnPolicy policy,
                      IPacketLogger logger, IHasherListener hasherListener, IUserNotifier notifier) {
        if (worker != null) {
            worker.stop();
            try {
                worker.ensureStopped();
            } catch (NullPointerException ignored) {
            }
            worker = null;
        }

        worker = new ProxyWorker(vpnService, descriptor, policy, logger, hasherListener, notifier);
        worker.start();
    }

    public void stop() {
        if (worker != null) {
            worker.stop();
            try {
                worker.ensureStopped();
            } catch (NullPointerException ignored) {
            }
            worker = null;
        }
    }

    public int[] getClientCounts() {
        int[] res = new int[2];

        if (worker != null) {
            res[0] = worker.getTcpRefCount();
            res[1] = worker.getUdpRefCount();
        } else {
            // TODO XXX why?
            res[0] = 0;
            res[1] = 0;
        }

        return res;
    }

    @NonNull
    @Override
    public String toString() {
        if (worker != null) {
            return worker.getEvent().toString();
        }

        return "(no worker)";
    }
}
