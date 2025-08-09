package app.netfilter;

import static app.internal.Settings.PREF_EXCLUDE_SYSTEM_APPS_DATA;
import static app.recovery.RecoveryService.MSG_PING_FROM_VPN;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.squareup.okio.VpnSocketEnumerator;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

import app.App;
import app.R;
import app.analytics.FirebaseAnalyticsSDK;
import app.analytics.MyAnalyticsSDK;
import app.common.LibNative;
import app.common.NetUtil;
import app.common.debug.L;
import app.dependencies.ApplicationDependencies;
import app.internal.Preferences;
import app.internal.ProxyBase;
import app.internal.Settings;
import app.netfilter.proxy.ChannelPool;
import app.netfilter.proxy.ProxyManager;
import app.netfilter.proxy.UDPClient;
import app.recovery.RecoveryService;
import app.security.Policy;
import app.security.PolicyRules;
import app.security.Whitelist;
import app.util.Toolbox;
import app.util.Util;

public class FilterVpnService extends VpnService {

    private final ServiceConnection recoveryServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            final Messenger messenger = new Messenger(service);

            isRecoveryServiceRunning.set(true);

            if (pingRecoveryServiceThread != null) {
                pingRecoveryServiceThread.interrupt();
                pingRecoveryServiceThread = null;
            }

            pingRecoveryServiceThread = new HandlerThread("pingRecoveryServiceThread");
            pingRecoveryServiceThread.start();

            new Handler(pingRecoveryServiceThread.getLooper()).post(() -> {
                while (!Thread.interrupted()) {

                    if (isRecoveryServiceRunning.get()) {
                        try {
                            messenger.send(Message.obtain(null, MSG_PING_FROM_VPN));
                        } catch (RemoteException ignored) {
                        }
                    }

                    try {
                        //noinspection BusyWait
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        break;
                    }

                }

            });

            if (checkRecoveryServiceThread != null) {
                checkRecoveryServiceThread.interrupt();
                checkRecoveryServiceThread = null;
            }

            checkRecoveryServiceThread = new HandlerThread("checkRecoveryServiceThread");
            checkRecoveryServiceThread.start();

            new Handler(checkRecoveryServiceThread.getLooper()).post(() -> {
                while (!Thread.interrupted()) {

                    try {
                        //noinspection BusyWait
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        break;
                    }

                    // Check the VPN status and restart the service if the VPN is disconnected.
                    boolean hasVpn = Toolbox.hasVpn(FilterVpnService.this);
                    boolean hasInternet = Toolbox.hasInternet(FilterVpnService.this);
                    if (!hasVpn && hasInternet) {
                        _restartVpnAsync();
                    }

                    if (ApplicationDependencies.getPreferences().isMadeFirstSetup() && !Toolbox.hasAlertNotification(FilterVpnService.this, ALERT_ID)) {
                        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        boolean isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(getPackageName());
                        if (!isIgnoringBatteryOptimizations) {
                            Toolbox.showAlertNotification(FilterVpnService.this, ALERT_ID, getString(R.string.NotificationAlert_IgnoringBatteryOptimizations_title), getString(R.string.NotificationAlert_IgnoringBatteryOptimizations_text));
                        }
                    }

                    // Check the status of the backup RecoveryService and restart it if it’s unresponsive.
                    if (isRecoveryServiceRunning.get() && !messenger.getBinder().pingBinder()) {
                        RecoveryService.bind(FilterVpnService.this, recoveryServiceConnection);
                        RecoveryService.startService(ApplicationDependencies.getApplication());
                    }
                }

            });

            if (statlogProcessThread != null) {
                statlogProcessThread.interrupt();
                statlogProcessThread = null;
            }

            statlogProcessThread = new HandlerThread("statlogProcessThread");
            statlogProcessThread.start();

            new Handler(statlogProcessThread.getLooper()).post(new Runnable() {
                @Override
                public void run() {
                    while (!Thread.interrupted()) {

                        try {
                            //noinspection BusyWait
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            break;
                        }

                        ApplicationDependencies.getStatlogHelper().process();

                    }

                }
            });

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

    };

    public class VpnBinder extends Binder {
        public FilterVpnService getService() {
            return FilterVpnService.this;
        }
    }

    public static final int JOB_ID = 1351;
    public static final int ALERT_ID = 1352;

    public static final int CMD_START_VPN = 1;
    public static final int CMD_CONFIG_CHANGED = 2;
    public static final int CMD_PROXY_CHANGED = 3;
    public static final int CMD_DROP_CONNECTS = 4;

    public static final int STATE_CONNECTED = 1;
    public static final int STATE_DISCONNECTED = 2;
    public static final int STATE_CHANGED = 3;
    public static final int STATE_TETHERING = 4;
    public static final int STATE_NO_TETHERING = 5;
    public static final int STATE_VPN_CONNECTED = 6;
    public static final int STATE_VPN_DISCONNECTED = 7;


    public static final String EXTRA_CMD = "cmd";
    public static final String STATE = "state";

    private final IBinder binder = new VpnBinder();
    private final AtomicBoolean isRecoveryServiceRunning = new AtomicBoolean(false);
    private final AtomicBoolean isVpnProcess = new AtomicBoolean(false);
    private HandlerThread vpnThread;
    private Handler vpnHandler;
    private HandlerThread pingRecoveryServiceThread;
    private HandlerThread checkRecoveryServiceThread;
    private HandlerThread statlogProcessThread;
    private IFilterVpnServiceListener listener;
    private Policy policy;

    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * The _startVpn method should be called only from {@link VpnHelper#startVpn()}
     */
    public void _startVpn() {
        RecoveryService.bind(this, recoveryServiceConnection);
        RecoveryService.startService(ApplicationDependencies.getApplication());

        _startVpnAndServiceSync();
    }

    /**
     * The _stopVpn method should be called only from {@link VpnHelper#stopVpn()}
     */
    public void _stopVpn() {
        isRecoveryServiceRunning.set(false);

        if (pingRecoveryServiceThread != null) {
            pingRecoveryServiceThread.interrupt();
            pingRecoveryServiceThread = null;
        }

        if (checkRecoveryServiceThread != null) {
            checkRecoveryServiceThread.interrupt();
            checkRecoveryServiceThread = null;
        }

        if (statlogProcessThread != null) {
            statlogProcessThread.interrupt();
            statlogProcessThread = null;
        }

        RecoveryService.unbind(this, recoveryServiceConnection);
        RecoveryService.stopService(ApplicationDependencies.getApplication());

        _stopVpnAndServiceAsync();
    }

    /**
     * The _restartVpnIfRunning method should be called only from {@link VpnHelper#restartVpnIfRunning()}
     */
    public void _restartVpnIfRunning() {
        if (Preferences.isActive()) {
            _restartVpnAsync();
        }
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////

    private void _setupVpnAsync() {
        runOnVpnThread(() -> {
            isVpnProcess.set(true); // Starting VPN setup.
            _setupVpnSync();
            isVpnProcess.set(false); // VPN setup completed.
        });
    }

    private void _stopVpnAsync() {
        runOnVpnThread(() -> {
            isVpnProcess.set(true); // Starting VPN setup.
            _stopVpnSync();
            isVpnProcess.set(false); // VPN setup completed.
        });
    }

    private void _startVpnAndServiceSync() {
        Context context = ApplicationDependencies.getApplication();
        Intent intent = new Intent(context, FilterVpnService.class);
        intent.putExtra(EXTRA_CMD, CMD_START_VPN);
        ContextCompat.startForegroundService(context, intent);
        startForeground();
    }

    private void _stopVpnAndServiceAsync() {
        runOnVpnThread(() -> {
            isVpnProcess.set(true); // Starting VPN setup.
            _stopVpnSync();
            isVpnProcess.set(false); // VPN setup completed.
            Util.runOnMain(() -> {
                stopForeground(true);
                stopSelf();
            });
        });
    }

    private void _restartVpnAsync() {
        runOnVpnThread(() -> {
            isVpnProcess.set(true); // Starting VPN setup.
            _stopVpnSync();
            _setupVpnSync();
            isVpnProcess.set(false); // VPN setup completed.
        });
    }

    private void startForeground() {
        startForeground(JOB_ID, Toolbox.getNotification(ApplicationDependencies.getApplication(), Settings.APP_NAME, Settings.APP_PACKAGE));
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////

    public static void notifyConfigChanged(@NonNull Context context, int state) {
        if (state < 0) {
            return;
        }

        if (!Preferences.isActive()) {
            return;
        }

        Util.runOnMain(new Runnable() {
            @Override
            public void run() {
                Intent vpnServiceIntent = new Intent(context, FilterVpnService.class);
                vpnServiceIntent.putExtra(EXTRA_CMD, CMD_CONFIG_CHANGED);
                vpnServiceIntent.putExtra(STATE, state);
                PendingIntent pendingIntent = PendingIntent.getService(context, 0, vpnServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                try {
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException ignored) {
                }
            }
        });
    }

    public static void notifyProxyChanged(@NonNull Context context) {
        if (!Preferences.isActive()) {
            return;
        }

        Util.runOnMain(new Runnable() {
            @Override
            public void run() {
                Intent vpnServiceIntent = new Intent(context, FilterVpnService.class);
                vpnServiceIntent.putExtra(EXTRA_CMD, CMD_PROXY_CHANGED);
                PendingIntent pendingIntent = PendingIntent.getService(context, 0, vpnServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                try {
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException ignored) {
                }
            }
        });
    }

    public static void notifyDropConnections(@NonNull Context context) {
        if (!Preferences.isActive()) {
            return;
        }

        Util.runOnMain(new Runnable() {
            @Override
            public void run() {
                Intent vpnServiceIntent = new Intent(context, FilterVpnService.class);
                vpnServiceIntent.putExtra(EXTRA_CMD, CMD_DROP_CONNECTS);
                vpnServiceIntent.putExtra("all", true);
                PendingIntent pendingIntent = PendingIntent.getService(context, 0, vpnServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                try {
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException ignored) {
                }
            }
        });
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate() {
        super.onCreate();

        vpnThread = new HandlerThread("vpnThread");
        vpnThread.start();
        vpnHandler = new Handler(vpnThread.getLooper());

        listener = new VpnListener(this);
    }

    @Override
    public void onRevoke() {

        if (listener != null) {
            listener.onVPNRevoked(this);
        }

        _stopVpnAndServiceAsync();

        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        RecoveryService.unbind(this, recoveryServiceConnection);

        if (vpnThread != null) {
            vpnThread.interrupt();
            vpnThread = null;
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startForeground();

        if (!App.isLibsLoaded() || !Preferences.isActive()) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        int command = -1;
        int state = -1;
        boolean allConnections = false;

        if (intent != null) {
            command = intent.getIntExtra(EXTRA_CMD, -1);
            state = intent.getIntExtra(STATE, -1);
            allConnections = intent.getBooleanExtra("all", false);
        }

        switch (command) {
            case CMD_START_VPN:
                if (Settings.DEBUG_STATE) {
                    L.a(Settings.TAG_FILTERVPNSERVICE, "CMD_STARTVPN " + (new Date()));
                }

                _setupVpnAsync();
                break;

            case CMD_CONFIG_CHANGED:
                if (Settings.DEBUG_STATE) {
                    L.a(Settings.TAG_FILTERVPNSERVICE, "CMD_CONFIG_CHANGED " + (new Date()));
                }

                if (!isVpnProcess.get()) {
                    if (state == STATE_CONNECTED) {
                        _restartVpnAsync();
                    } else if (state == STATE_DISCONNECTED) {
                        _stopVpnAsync();
                    } else if (state == STATE_TETHERING) {
                        _stopVpnAsync();
                    } else if (state == STATE_CHANGED) {
                        _restartVpnAsync();
                    }
                }
                break;

            case CMD_PROXY_CHANGED:
            case CMD_DROP_CONNECTS:
                if (Settings.DEBUG_STATE) {
                    String cmd = (command == CMD_PROXY_CHANGED) ? "CMD_PROXY_CHANGED" : "CMD_DROP_CONNECTS";
                    L.a(Settings.TAG_FILTERVPNSERVICE, cmd + (new Date()));
                }

                // close proxy connections (or all connections) if proxy config changed
                if (allConnections) {
                    ProxyManager.getInstance().closeAllConnections();
                } else {
                    ProxyManager.getInstance().closeProxyConnections();
                }

                ChannelPool.clear(false);
                break;

            default:
                MyAnalyticsSDK.log("TEST Unknown command in FilterVpnService: " + command);
                FirebaseAnalyticsSDK.LogWarning("Unknown command in FilterVpnService: " + command);
                _startVpn();
                break;
        }

        return START_STICKY;

    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////

    private void runOnVpnThread(@NonNull Runnable runnable) {
        vpnHandler.post(runnable);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////

    private void setDnsServers(Builder vpnBuilder, String[] dnsServers) {
        final String fakeDNS = Settings.DNS_FAKE_IP;

        // first set DNS servers from settings (see FilterVpnOptions.setDNSServers)
        UDPClient.setDNSServers(dnsServers);

        // next add DNS servers from provider
        ArrayList<String> servers = NetUtil.getDNSServers();
        if (servers.size() == 1 && servers.get(0).equals(fakeDNS)) {
            servers = Preferences.getDNSServers();
        } else {
            Preferences.putDNSServers(servers);
        }
        UDPClient.addDNSServers(servers);

        // for VPN set our fakeDNS server
        try {
            vpnBuilder.addDnsServer(fakeDNS);
        } catch (IllegalArgumentException e) {
            logAddDnsServerError(fakeDNS);
        }

    }

    // The method configures the list of apps that we do not filter.
    private void setWhitelist(Builder vpnBuilder) {
        try {
            vpnBuilder.addDisallowedApplication(getPackageName());
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        if (Preferences.getBoolean(PREF_EXCLUDE_SYSTEM_APPS_DATA, false)) {
            String[] systemApps = ApplicationDependencies.getAppsUidManager().getAllSystemAppPackageNames();
            for (String pkgName : systemApps) {
                try {
                    vpnBuilder.addDisallowedApplication(pkgName);
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
        }

        for (String pkgName : Whitelist.getAllowed()) {
            try {
                vpnBuilder.addDisallowedApplication(pkgName);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
    }

    private void logAddDnsServerError(String server) {
        if (listener != null) {
            listener.onOtherError("Error setting DNS server: " + server);
        }
    }

    private void _setupVpnSync() {
        if (NetUtil.getStatus() < 0 || !Preferences.isActive()) {
            return;
        }

        if (ProxyManager.isStarted()) {
            return;
        }

        if (listener != null) {
            listener.onBeforeServiceStart(this);
        }

        if (policy == null) {
            policy = new Policy();
        }

        try {
            VpnService.prepare(this);
        } catch (Exception ignored) {
        }

        FilterVpnOptions options = getOptions(this);

        Builder vpnBuilder = new Builder();

        if (options.sessionName != null) {
            vpnBuilder.setSession(options.sessionName);
        }

        if (options.mtu != 0) {
            vpnBuilder.setMtu(options.mtu);
        }

        vpnBuilder.addAddress(options.address, options.maskBits);
        if (options.addDefaultRoute) {
            vpnBuilder.addRoute("0.0.0.0", 0);
        }

        if (options.configureIntent != null) {
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, options.configureIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            vpnBuilder.setConfigureIntent(pendingIntent);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vpnBuilder.setMetered(false);
        }

        setDnsServers(vpnBuilder, options.dnsServers);

        setWhitelist(vpnBuilder);

        try {
            ParcelFileDescriptor pfd = null;

            try {
                pfd = vpnBuilder.establish();
            } catch (NullPointerException ignored) {
            }

            if (pfd == null) {
                if (listener != null) {
                    listener.onVPNEstablishError(this);
                }
                return;
            }

            LibNative.fileSetBlocking(pfd.getFd(), true);

            IHasherListener hasherListener = new IHasherListener() {
                @Override
                public void onFinish(String url, byte[] sha1, byte[] sha256, byte[] md5) {

                }
            };

            // start network processing
            VpnSocketEnumerator.setVpnService(this); // use patched OkHttp and OkIo to bypass TUN on our http request
            ProxyManager.getInstance().start(this, pfd, policy, null, hasherListener, new MyUserNotifier());

            policy.reload();

            if (listener != null) {
                listener.onVPNStarted(this);
            }

            // clear vpn stop time
            Preferences.putLong(Settings.PREF_DISABLE_TIME, 0);

        } catch (IllegalStateException | IllegalArgumentException e) {
            onVPNEstablishException(e);
        }

    }

    private void _stopVpnSync() {
        VpnSocketEnumerator.setVpnService(null); // use patched OkHttp and OkIo to bypass TUN on our http request
        Util.runOnThread(() -> {
            ApplicationDependencies.getStatlogHelper().process(); // save stats before close connections
        });
        ProxyManager.getInstance().closeAllConnections(); // close all connections before vpn shutdown
        ProxyManager.getInstance().stop();
        ProxyBase.notifyServersUp();
        System.gc();
    }

    private void onVPNEstablishException(Exception e) {
        if (listener != null) {
            listener.onVPNEstablishException(this, e);
        }

        Util.runOnMain(() -> {
            stopForeground(true);
            stopSelf();
        });
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////

    public FilterVpnOptions getOptions(Context context) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        FilterVpnOptions options = new FilterVpnOptions(context.getString(R.string.app_name), Settings.TUN_IP, 24, 0, intent);
//        options.setDNSServers(FilterVpnOptions.DEFAULT_DNS_AFTER, Settings.DNS1_IP, Settings.DNS2_IP); // Google DNS disabled; using custom DNS instead.
        options.setDNSServers(FilterVpnOptions.DEFAULT_DNS_AFTER);

        if (NetUtil.isMobile()) {
            options.mtu = 1400;
        } else {
            options.mtu = 4096;
        }

        return options;
    }

    private static class MyUserNotifier implements IUserNotifier {
        Hashtable<String, Long> map = new Hashtable<>();

        // notify from IP
        @Override
        public void notify(PolicyRules rules, byte[] serverIp) {
        }

        @Override
        public void notify(PolicyRules rules, String domain) {
        }

        // notify from HTTP request
        @Override
        public void notify(PolicyRules rules, String domain, String refDomain) {
            if (!isReadyToShow(domain, refDomain)) {
                return;
            }

            map.put(domain + refDomain, System.currentTimeMillis());
        }

        boolean isReadyToShow(String domain, String refDomain) {
            long curTime = System.currentTimeMillis();
            final String key = domain + refDomain;

            if (map.containsKey(key)) {
                Long time = map.get(key);
                long _time = 0;
                if (time != null) {
                    _time = time;
                }
                return curTime - _time >= 5 * 60 * 1000; // 5 min timeout
            }

            return true;
        }

        void clear() {
            map.clear();
        }
    }

}
