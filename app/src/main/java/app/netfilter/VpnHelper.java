package app.netfilter;

import static android.content.Context.BIND_AUTO_CREATE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

public class VpnHelper {

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FilterVpnService.VpnBinder binder = (FilterVpnService.VpnBinder) service;
            vpnService = binder.getService();
            notifyServiceReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private final Context context;
    private final List<Runnable> serviceReadyCallbacks = new ArrayList<>();
    private FilterVpnService vpnService;
    private boolean isServiceReady;
    private boolean isInitializationReady;

    public VpnHelper(Context context) {
        this.context = context;
    }

    public void bind() {
        Intent intent = new Intent(context, FilterVpnService.class);
        context.bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    public void unbind() {
        context.unbindService(serviceConnection);
    }

    public void startVpn() {
        if (vpnService == null || !isInitializationReady) {
            Runnable callback = () -> vpnService._startVpn();
            serviceReadyCallbacks.add(callback);
        } else {
            vpnService._startVpn();
        }
    }

    public void stopVpn() {
        if (vpnService != null) {
            vpnService._stopVpn();
        }
    }

    public void restartVpnIfRunning() {
        if (vpnService != null) {
            vpnService._restartVpnIfRunning();
        }
    }

    public synchronized void notifyServiceReady() {
        isServiceReady = true;
        executeServiceReadyCallbacks();
    }

    public synchronized void notifyInitializationReady() {
        isInitializationReady = true;
        executeServiceReadyCallbacks();
    }

    private synchronized void executeServiceReadyCallbacks() {
        if (isServiceReady && isInitializationReady) {
            for (Runnable callback : serviceReadyCallbacks) {
                callback.run();
            }
            serviceReadyCallbacks.clear();
        }
    }

}
