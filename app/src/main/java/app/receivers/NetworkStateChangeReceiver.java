package app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import app.App;
import app.common.NetUtil;
import app.netfilter.FilterVpnService;
import app.security.Firewall;
import app.security.Policy;
import app.util.Toolbox;

public class NetworkStateChangeReceiver extends BroadcastReceiver {

    private int state = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!App.isLibsLoaded()) {
            return;
        }

        int status = NetUtil.getStatus();
        boolean netOn = (status != -1);

        if (netOn) {
            Firewall.onNetworkChanged();
            Policy.refreshToken(false);
        }

        updateNetworkState(context);
    }

    private void updateNetworkState(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        boolean isConnected = networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        boolean isVpnConnected = Toolbox.hasVpn(context);


        int _state;
        if (isVpnConnected) {
            _state = FilterVpnService.STATE_CONNECTED;
        } else {
            _state = isConnected ? FilterVpnService.STATE_CONNECTED : FilterVpnService.STATE_DISCONNECTED;
        }

        if (state != _state) {
            this.state = _state;
            FilterVpnService.notifyConfigChanged(context, state);
        }
    }

}
