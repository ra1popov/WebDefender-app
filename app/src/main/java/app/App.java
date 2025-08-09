package app;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;
import androidx.work.Configuration;

import com.google.firebase.FirebaseApp;

import app.analytics.FirebaseAnalyticsSDK;
import app.analytics.FirebaseCrashlyticsSDK;
import app.analytics.MyAnalyticsSDK;
import app.common.LibNative;
import app.common.Utils;
import app.common.memdata.ByteBufferPool;
import app.dependencies.ApplicationDependencies;
import app.dependencies.ApplicationDependencyProvider;
import app.internal.Preferences;
import app.internal.Settings;
import app.jobs.InitializerJob;
import app.netfilter.proxy.PacketPool;
import app.receivers.AppReceiver;
import app.receivers.MyUpdateReceiver;
import app.receivers.NetworkStateChangeReceiver;
import app.receivers.ScreenStateReceiver;
import app.receivers.TetheringStateReceiver;
import app.util.Toolbox;

public class App extends MultiDexApplication implements DefaultLifecycleObserver, Configuration.Provider {

    private static boolean isLibsLoaded = false;
    private static int myUid = 0;
    private static final Object ccLock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();

        initializeAnalytics();

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        initializeAppDependencies();

        if (!isMainProcess(this)) {
            return;
        }

        initLibs();

        if (!isLibsLoaded()) {
            Preferences.putBoolean(Settings.PREF_ACTIVE, false);
        } else {
            LibNative.signalHandlerSet(1); // for tun threads interrupt (see threadSendSignal)

            ApplicationDependencies.getVpnHelper().bind();
            ApplicationDependencies.getJobManager().add(new InitializerJob());
        }

        initNetworkStateChangeReceiver(); // For Android N and above, you need to explicitly register the broadcast receiver.
        initTetheringStateReceiver();
        initScreenStateReceiver();
        initAppReceiver();
        initMyUpdateReceiver();

        findMyUid();

    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (!isMainProcess(this)) {
            return;
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (!isMainProcess(this)) {
            return;
        }
    }

    private void initNetworkStateChangeReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(new NetworkStateChangeReceiver(), intentFilter);
    }

    private void initTetheringStateReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.TETHER_STATE_CHANGED");
        registerReceiver(new TetheringStateReceiver(), intentFilter);
    }

    private void initScreenStateReceiver() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(new ScreenStateReceiver(), intentFilter);
    }

    private void initAppReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addDataScheme("package");
        registerReceiver(new AppReceiver(), intentFilter);
    }

    private void initMyUpdateReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MY_PACKAGE_REPLACED);
        registerReceiver(new MyUpdateReceiver(), intentFilter);
    }

    private void initializeAnalytics() {
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        FirebaseAnalyticsSDK.init(this);
        FirebaseCrashlyticsSDK.init(this);
        MyAnalyticsSDK.init(this);
    }

    private void initializeAppDependencies() {
        ApplicationDependencies.init(this, new ApplicationDependencyProvider(this));
        ApplicationDependencies.getJobManager().beginJobLoop();
    }

    private static void initLibs() {
        try {
            // try to load libraries
            System.loadLibrary("native");
            System.loadLibrary("bspatch");

            long libsVersion = LibNative.getLibsVersion();
            if (libsVersion == LibNative.LIBS_VERSION) {
                isLibsLoaded = true;
            }
        } catch (UnsatisfiedLinkError | Exception ignored) {
        }
    }

    public static int getMyUid() {
        return myUid;
    }

    private void findMyUid() {
        PackageManager pm = getPackageManager();
        try {
            final ApplicationInfo info = pm.getApplicationInfo(packageName(), PackageManager.GET_META_DATA);
            myUid = info.uid;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Context getContext() {
        return ApplicationDependencies.getApplication();
    }

    public static boolean isLibsLoaded() {
        return isLibsLoaded;
    }

    public static void startVpnService() {
        if (!Preferences.isActive()) {
            Preferences.putBoolean(Settings.PREF_ACTIVE, true);
        }

        ApplicationDependencies.getVpnHelper().startVpn();
    }

    public static void disable() {
        if (Preferences.isActive()) {
            Preferences.putBoolean(Settings.PREF_ACTIVE, false);
        }

        ApplicationDependencies.getVpnHelper().stopVpn();
    }

    public static String packageName() {
        return ApplicationDependencies.getApplication().getPackageName();
    }

    /*
     * force - always run actions
     * onlyKill - only apps kill, no cache clean
     * onlyClean - only cache clean, no apps kill
     * checkTime - check PREF_DISABLE_TIME and interval to select kill+clean or only kill
     *
     * return true if action executed
     *
     * with checkTime call before startVpnService because PREF_DISABLE_TIME will be reset in setupVpn
     */
    public static void cleanCaches(boolean force, boolean onlyKill, boolean onlyClean, boolean checkTime) {
        if (!Settings.CLEAR_CACHES) {
            return;
        }

        synchronized (ccLock) {
            if (!force && Toolbox.isScreenOn(ApplicationDependencies.getApplication())) {
                // not force and screen enabled -> wait for screen off (see ScreenStateReceiver)
                if (!onlyKill) {
                    Preferences.putBoolean(Settings.PREF_CLEARCACHES_NEED, true);
                }
                return;
            }

            if (checkTime) {
                long disableTime = Preferences.get_l(Settings.PREF_DISABLE_TIME);
                long time = System.currentTimeMillis();

                if (disableTime <= 0 || disableTime + Settings.CLEAR_CACHES_INTERVAL > time) {
                    onlyKill = true; // interval not expire, kill only
                }
            }

            if (!onlyKill) {
                Preferences.putLong(Settings.PREF_CLEARCACHES_TIME, System.currentTimeMillis());
                Preferences.putBoolean(Settings.PREF_CLEARCACHES_NEED, false);
                Utils.clearCaches();
            }

            if (!onlyClean) {
                Utils.killBackgroundApps();
            }

        }
    }

    @Override
    public void onTrimMemory(int level) {

        //	TRIM_MEMORY_RUNNING_MODERATE and TRIM_MEMORY_RUNNING_CRITICAL
        if (level <= 15 && level >= 5) {
            ByteBufferPool.clear();
            PacketPool.compact();
        }

        System.gc();

        super.onTrimMemory(level);

    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().build();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean isMainProcess(@NonNull Context context) {
        String mainProcessName = context.getPackageName();
        String currentProcessName = getProcessName(context);
        return mainProcessName.equals(currentProcessName);
    }

    private static String getProcessName(@NonNull Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
                if (processInfo.pid == pid) {
                    return processInfo.processName;
                }
            }
        }
        return "";
    }

}
