package app.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

import app.App;
import app.dependencies.ApplicationDependencies;
import app.internal.Preferences;
import app.internal.ProxyBase;
import app.internal.Settings;
import app.security.Policy;
import app.updater.UpdaterService;

public class TimerWorker extends Worker {

    private static final String TAG = TimerWorker.class.getName();

    public TimerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void startTimers() {
        updateSettingsStartTimer(!App.isLibsLoaded() || !Preferences.isActive());
    }

    public static void updateSettingsStartTimer(boolean cancel) {
        WorkManager workManager = WorkManager.getInstance(ApplicationDependencies.getApplication());

        if (cancel) {
            workManager.cancelUniqueWork(TAG);
        } else {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TimerWorker.class)
                    .setConstraints(constraints)
                    .setInitialDelay(updateSettingsGetNextTime(), TimeUnit.MILLISECONDS)
                    .build();

            workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, workRequest);
        }
    }

    private static long updateSettingsGetNextTime() {
        long curTime = System.currentTimeMillis();
        long intervalDay = 86400000L;
        long curDayStart = intervalDay * (curTime / intervalDay);
        long startTime = curDayStart + Settings.SETTINGS_RELOAD_INTERVAL;

        if (startTime > curTime) {
            return startTime;
        } else {
            return (startTime + intervalDay);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        if (Preferences.isActive()) {
            Policy.refreshToken(false);
            ProxyBase.updateServers(true);
            App.cleanCaches(true, false, false, false);
            UpdaterService.startUpdate(UpdaterService.START_DELAYED);
        }
        return Result.success();
    }

}
