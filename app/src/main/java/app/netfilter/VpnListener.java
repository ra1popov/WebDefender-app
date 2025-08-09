package app.netfilter;

import android.content.Context;

import app.App;
import app.common.Utils;
import app.common.WiFi;
import app.common.debug.L;
import app.internal.Settings;
import app.netfilter.dns.DNSUtils;
import app.security.Exceptions;
import app.ui.Toasts;
import app.workers.TimerWorker;

public class VpnListener implements IFilterVpnServiceListener {

    public VpnListener(Context context) {
    }

    @Override
    public void onBeforeServiceStart(FilterVpnService service) {

    }

    @Override
    public void onServiceStarted(FilterVpnService service) {
        L.a(Settings.TAG_VPNLISTENER, "Service started");

    }

    @Override
    public void onServiceStopped(FilterVpnService service) {
        L.a(Settings.TAG_VPNLISTENER, "Service stopped");

    }

    @Override
    public void onVPNStarted(FilterVpnService service) {
        L.a(Settings.TAG_VPNLISTENER, "VPN established");

        startResolvingThread();
    }

    @Override
    public void onVPNStopped(FilterVpnService service) {
        L.a(Settings.TAG_VPNLISTENER, "VPN stopped");

    }

    @Override
    public void onVPNRevoked(FilterVpnService service) {
        L.a(Settings.TAG_VPNLISTENER, "VPN revoked by user");

        App.disable();
        TimerWorker.updateSettingsStartTimer(true);
    }

    @Override
    public void onVPNEstablishError(FilterVpnService service) {
        L.a(Settings.TAG_VPNLISTENER, "VPN establish error");

        Toasts.showVpnEstablishError();
    }

    @Override
    public void onVPNEstablishException(FilterVpnService service, Exception e) {
        L.a(Settings.TAG_VPNLISTENER, "Error establishing VPN!");

        e.printStackTrace();
        Toasts.showVpnEstablishException();
    }

    @Override
    public void onOtherError(String error) {
        L.e(Settings.TAG_VPNLISTENER, error);
    }

    @Override
    public void onProxyIsSet(FilterVpnService service) {
    }

    @Override
    public void saveStats(int[] clientsCounts, int[] netinfo, long[] policy) {

    }

    private void startResolvingThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // wait 5 sec and ...
                // TODO XXX if no inet? if not started?
                Utils.sleep(5000);

                // resolve Chrome compression domains (to block ssl connections)
                String[] buf = Exceptions.getCompressed();
                for (String domain : buf) {
                    DNSUtils.resolve(domain);
                }

                // tun test
                Utils.canConnect(Settings.TEST_TUN_WORK_IP, 80);
                // check proxy bug (traffic -> proxy -> vpn)
                if (WiFi.isProxySet(App.getContext())) {
                    Utils.canConnect(Settings.TEST_LOCAL_PROXY_IP, 80);
                }
            }
        }).start();
    }
}
