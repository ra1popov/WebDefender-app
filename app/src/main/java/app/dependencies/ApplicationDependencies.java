package app.dependencies;

import android.annotation.SuppressLint;
import android.app.Application;

import androidx.annotation.NonNull;

import app.ad.AdRepository;
import app.api.ApiClient;
import app.application.ApplicationRepository;
import app.application.ApplicationUidManager;
import app.database.DatabaseHelper;
import app.guide.BootPermissionHelper;
import app.guide.GuideHelper;
import app.guide.PowerPermissionHelper;
import app.jobmanager.JobManager;
import app.netfilter.VpnHelper;
import app.preferences.Preferences;
import app.ui.main.MainRepository;
import app.ui.proxy.ProxyRepository;
import app.ui.statlog.StatlogHelper;
import app.ui.statlog.StatlogRepository;

public class ApplicationDependencies {

    private static Application application;
    private static Provider provider;
    private static JobManager jobManager;

    @SuppressLint("StaticFieldLeak")
    private static Preferences preferences;

    @SuppressLint("StaticFieldLeak")
    private static ApiClient apiClient;

    @SuppressLint("StaticFieldLeak")
    private static AdRepository adRepository;

    @SuppressLint("StaticFieldLeak")
    private static DatabaseHelper databaseHelper;

    @SuppressLint("StaticFieldLeak")
    private static StatlogHelper statlogHelper;

    @SuppressLint("StaticFieldLeak")
    private static StatlogRepository statlogRepository;

    @SuppressLint("StaticFieldLeak")
    private static ProxyRepository proxyRepository;

    @SuppressLint("StaticFieldLeak")
    private static ApplicationRepository applicationRepository;

    @SuppressLint("StaticFieldLeak")
    private static ApplicationUidManager applicationUidManager;

    @SuppressLint("StaticFieldLeak")
    private static VpnHelper vpnHelper;

    @SuppressLint("StaticFieldLeak")
    private static MainRepository mainRepository;

    @SuppressLint("StaticFieldLeak")
    private static GuideHelper guideHelper;

    @SuppressLint("StaticFieldLeak")
    private static BootPermissionHelper bootPermissionHelper;

    @SuppressLint("StaticFieldLeak")
    private static PowerPermissionHelper powerPermissionHelper;

    public static synchronized void init(@NonNull Application application, @NonNull Provider provider) {
        if (ApplicationDependencies.application != null || ApplicationDependencies.provider != null) {
            throw new IllegalStateException("Already initialized!");
        }

        ApplicationDependencies.application = application;
        ApplicationDependencies.provider = provider;
    }

    @NonNull
    public static Application getApplication() {
        assertInitialization();
        return application;
    }

    @NonNull
    public static synchronized JobManager getJobManager() {
        assertInitialization();

        if (jobManager == null) {
            jobManager = provider.provideJobManager();
        }

        return jobManager;
    }

    @NonNull
    public static synchronized Preferences getPreferences() {
        assertInitialization();

        if (preferences == null) {
            preferences = provider.providePreferences();
        }

        return preferences;
    }

    @NonNull
    public static synchronized ApiClient getApiClient() {
        assertInitialization();

        if (apiClient == null) {
            apiClient = provider.provideApiClient();
        }

        return apiClient;
    }

    @NonNull
    public static synchronized AdRepository getAdRepository() {
        assertInitialization();

        if (adRepository == null) {
            adRepository = provider.provideAdRepository();
        }

        return adRepository;
    }

    @NonNull
    public static synchronized DatabaseHelper getDatabaseHelper() {
        assertInitialization();

        if (databaseHelper == null) {
            databaseHelper = provider.provideDatabaseHelper();
        }

        return databaseHelper;
    }

    @NonNull
    public static synchronized StatlogHelper getStatlogHelper() {
        assertInitialization();

        if (statlogHelper == null) {
            statlogHelper = provider.provideStatlogHelper();
        }

        return statlogHelper;
    }

    @NonNull
    public static synchronized StatlogRepository getStatlogRepository() {
        assertInitialization();

        if (statlogRepository == null) {
            statlogRepository = provider.provideStatlogRepository();
        }

        return statlogRepository;
    }

    @NonNull
    public static synchronized ProxyRepository getProxyRepository() {
        assertInitialization();

        if (proxyRepository == null) {
            proxyRepository = provider.provideProxyRepository();
        }

        return proxyRepository;
    }

    @NonNull
    public static synchronized ApplicationRepository getApplicationRepository() {
        assertInitialization();

        if (applicationRepository == null) {
            applicationRepository = provider.provideApplicationRepository();
        }

        return applicationRepository;
    }

    @NonNull
    public static synchronized ApplicationUidManager getAppsUidManager() {
        assertInitialization();

        if (applicationUidManager == null) {
            applicationUidManager = provider.provideAppsUidManager();
        }

        return applicationUidManager;
    }

    @NonNull
    public static synchronized VpnHelper getVpnHelper() {
        assertInitialization();

        if (vpnHelper == null) {
            vpnHelper = provider.provideVpnHelper();
        }

        return vpnHelper;
    }

    @NonNull
    public static synchronized MainRepository getMainRepository() {
        assertInitialization();

        if (mainRepository == null) {
            mainRepository = provider.provideMainRepository();
        }

        return mainRepository;
    }

    @NonNull
    public static synchronized GuideHelper getGuideHelper() {
        assertInitialization();

        if (guideHelper == null) {
            guideHelper = provider.provideGuideHelper();
        }

        return guideHelper;
    }

    @NonNull
    public static synchronized BootPermissionHelper getBootPermissionHelper() {
        assertInitialization();

        if (bootPermissionHelper == null) {
            bootPermissionHelper = provider.provideBootPermissionHelper();
        }

        return bootPermissionHelper;
    }

    @NonNull
    public static synchronized PowerPermissionHelper getPowerPermissionHelper() {
        assertInitialization();

        if (powerPermissionHelper == null) {
            powerPermissionHelper = provider.providePowerPermissionHelper();
        }

        return powerPermissionHelper;
    }

    private static void assertInitialization() {
        if (application == null || provider == null) {
            throw new UninitializedException();
        }
    }

    public interface Provider {
        @NonNull
        JobManager provideJobManager();

        @NonNull
        Preferences providePreferences();

        @NonNull
        ApiClient provideApiClient();

        @NonNull
        AdRepository provideAdRepository();

        @NonNull
        DatabaseHelper provideDatabaseHelper();

        @NonNull
        StatlogHelper provideStatlogHelper();

        @NonNull
        StatlogRepository provideStatlogRepository();

        @NonNull
        ProxyRepository provideProxyRepository();

        @NonNull
        ApplicationRepository provideApplicationRepository();

        @NonNull
        ApplicationUidManager provideAppsUidManager();

        @NonNull
        VpnHelper provideVpnHelper();

        @NonNull
        MainRepository provideMainRepository();

        @NonNull
        GuideHelper provideGuideHelper();

        @NonNull
        BootPermissionHelper provideBootPermissionHelper();

        @NonNull
        PowerPermissionHelper providePowerPermissionHelper();
    }

    private static class UninitializedException extends IllegalStateException {
        private UninitializedException() {
            super("You must call init() first!");
        }
    }

}
