package app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import app.common.NetUtil;
import app.common.Usb;
import app.netfilter.FilterVpnService;

public class TetheringStateReceiver extends BroadcastReceiver {
    private static final int NO_TETHER = 1;
    private static final int USB_TETHER = 2;
    private static final int WIFI_TETHER = 3;

    private int state = NO_TETHER;

    @Override
    public void onReceive(Context context, Intent intent) {
        int _state;

        if (NetUtil.isSharingWiFi((WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE))) {
            _state = WIFI_TETHER;
        } else if (Usb.isUsbTethered()) {
            _state = USB_TETHER;
        } else {
            _state = NO_TETHER;
        }

        if (state != _state) {
            state = _state;
            if (state == NO_TETHER) {
                FilterVpnService.notifyConfigChanged(context, FilterVpnService.STATE_NO_TETHERING);
            } else {
                FilterVpnService.notifyConfigChanged(context, FilterVpnService.STATE_TETHERING);
            }
        }
    }

}
