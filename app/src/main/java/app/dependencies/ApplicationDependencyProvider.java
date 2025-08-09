package app.dependencies;

import android.app.Application;

import androidx.annotation.NonNull;

import app.Config;
import app.ad.AdRepository;
import app.api.ApiClient;
import app.application.ApplicationRepository;
import app.application.ApplicationUidManager;
import app.database.DatabaseHelper;
import app.guide.BootPermissionHelper;
import app.guide.GuideHelper;
import app.guide.PowerPermissionHelper;

import app.jobmanager.JobManager;
import app.jobmanager.impl.JsonDataSerializer;
import app.jobs.FastJobStorage;
import app.jobs.JobManagerFactories;
import app.netfilter.VpnHelper;
import app.preferences.Preferences;
import app.ui.main.MainRepository;
import app.ui.proxy.ProxyRepository;
import app.ui.statlog.StatlogHelper;
import app.ui.statlog.StatlogRepository;

public class ApplicationDependencyProvider implements ApplicationDependencies.Provider {

    private final Application context;

    public ApplicationDependencyProvider(@NonNull Application context) {
        this.context = context;
    }

    @NonNull
    @Override
    public JobManager provideJobManager() {
        return new JobManager(context, new JobManager.Configuration.Builder()
                .setDataSerializer(new JsonDataSerializer())
                .setJobFactories(JobManagerFactories.getJobFactories(context))
                .setConstraintFactories(JobManagerFactories.getConstraintFactories(context))
                .setConstraintObservers(JobManagerFactories.getConstraintObservers(context))
                .setJobStorage(new FastJobStorage())
                .setJobThreadCount(10) // fix to quickly work
                .build());
    }

    @NonNull
    @Override
    public Preferences providePreferences() {
        return new Preferences(context);
    }

    @NonNull
    @Override
    public ApiClient provideApiClient() {
        return new ApiClient(context, Config.DOMAIN);
    }

    @NonNull
    @Override
    public AdRepository provideAdRepository() {
        return new AdRepository(context);
    }

    @NonNull
    @Override
    public DatabaseHelper provideDatabaseHelper() {
        return new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public StatlogHelper provideStatlogHelper() {
        return new StatlogHelper(context);
    }

    @NonNull
    @Override
    public StatlogRepository provideStatlogRepository() {
        return new StatlogRepository(context);
    }

    @NonNull
    @Override
    public ProxyRepository provideProxyRepository() {
        return new ProxyRepository(context);
    }

    @NonNull
    @Override
    public ApplicationRepository provideApplicationRepository() {
        return new ApplicationRepository(context);
    }

    @NonNull
    @Override
    public ApplicationUidManager provideAppsUidManager() {
        return new ApplicationUidManager(context);
    }

    @NonNull
    @Override
    public VpnHelper provideVpnHelper() {
        return new VpnHelper(context);
    }

    @NonNull
    @Override
    public MainRepository provideMainRepository() {
        return new MainRepository(context);
    }

    @NonNull
    @Override
    public GuideHelper provideGuideHelper() {
        return new GuideHelper(context);
    }

    @NonNull
    @Override
    public BootPermissionHelper provideBootPermissionHelper() {
        return new BootPermissionHelper();
    }

    @NonNull
    @Override
    public PowerPermissionHelper providePowerPermissionHelper() {
        return new PowerPermissionHelper();
    }

}
