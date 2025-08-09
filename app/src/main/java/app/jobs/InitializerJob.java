package app.jobs;

import android.content.Context;

import androidx.annotation.NonNull;

import app.App;
import app.common.NetUtil;
import app.dependencies.ApplicationDependencies;
import app.internal.Preferences;
import app.internal.ProxyBase;
import app.jobmanager.Data;
import app.jobmanager.Job;
import app.security.Browsers;
import app.security.Database;
import app.security.Exceptions;
import app.security.Firewall;
import app.security.Policy;
import app.security.UserAgents;
import app.security.Whitelist;
import app.workers.TimerWorker;

public class InitializerJob extends BaseJob {

    public static final String QUEUE = "Main";
    public static final String KEY = "InitializerJob";

    public InitializerJob() {
        this(new Parameters.Builder()
                        .setQueue(QUEUE)
//                        .addConstraint(NetworkConstraint.KEY)
                        .setMaxAttempts(1)
//                        .setLifespan(TimeUnit.SECONDS.toMillis(5))
                        .build()
        );
    }

    private InitializerJob(@NonNull Parameters parameters) {
        super(parameters);
    }

    @Override
    @NonNull
    public Data serialize() {
        return new Data.Builder().build();
    }

    @Override
    @NonNull
    public String getFactoryKey() {
        return KEY;
    }

    @Override
    public void onCanceled() {
    }

    @Override
    public void onRun() {
        Context context = ApplicationDependencies.getApplication();

        Preferences.load(context);
        NetUtil.init(context);

        Firewall.init();
        Whitelist.init();

        UserAgents.load(context);

        // check db versions
        boolean newer = Database.distribHaveNewer();
        if (newer) {
            // distrib version > current version (may be update? may be call MyUpdateReceiver?)
            Database.deleteDatabases();
            Database.setCurrentVersion(null); // force db reset
            Database.setCurrentFastVersion(0);
        }

        // init db
        final boolean changed = Database.init(); // must be first, before Exceptions.load and Browsers.load

        Exceptions.load();
        Browsers.load();

        if (changed) {
            // database reinstalled
            final boolean inited = Policy.updateScanner();

            if (inited) {
                App.cleanCaches(true, false, false, false); // clean+kill on db update
            }
        }

        TimerWorker.startTimers();

        ProxyBase.load();
        ProxyBase.updateServers(true);

        ApplicationDependencies.getAppsUidManager().hashAllUid(); // We start a service that collects UID information for all applications (to determine the packageName by UID for Android 7 and above, since Google has restricted access to /proc).

        ApplicationDependencies.getVpnHelper().notifyInitializationReady();
    }

    @Override
    protected boolean onShouldRetry(@NonNull Exception exception) {
        return false;
    }

    public static final class Factory implements Job.Factory<InitializerJob> {
        @Override
        @NonNull
        public InitializerJob create(@NonNull Parameters parameters, @NonNull Data data) {
            return new InitializerJob(parameters);
        }
    }

}
