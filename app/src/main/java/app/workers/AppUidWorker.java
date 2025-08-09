package app.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import app.dependencies.ApplicationDependencies;

/**
 * Created 27.08.17
 * <p>
 * The task generates information about the UID of all applications (to determine the packageName by UID for Android 7 and above, since Google has restricted access to /proc).
 */
public class AppUidWorker extends Worker {

    public AppUidWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void hashAllUid(Context context) {
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(AppUidWorker.class).build();
        WorkManager.getInstance(context).enqueue(workRequest);
    }

    @NonNull
    @Override
    public Result doWork() {
        ApplicationDependencies.getAppsUidManager().hashAllUid();
        return Result.success();
    }

}
