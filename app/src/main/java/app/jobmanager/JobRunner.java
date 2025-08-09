package app.jobmanager;

import android.app.Application;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.annimon.stream.Stream;

import java.util.List;
import java.util.concurrent.TimeUnit;

class JobRunner extends Thread {

    private static final String TAG = JobRunner.class.getSimpleName();

    private static final long WAKE_LOCK_TIMEOUT = TimeUnit.MINUTES.toMillis(10);

    private final Application application;
    private final int id;
    private final JobController jobController;

    JobRunner(@NonNull Application application, int id, @NonNull JobController jobController) {
        super("JobRunner-" + id);

        this.application = application;
        this.id = id;
        this.jobController = jobController;
    }

    @Override
    public synchronized void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            Job job = jobController.pullNextEligibleJobForExecution();
            Job.Result result = run(job);

            jobController.onJobFinished(job);

            if (result.isSuccess()) {
                jobController.onSuccess(job);
            } else if (result.isRetry()) {
                jobController.onRetry(job);
                job.onRetry();
            } else if (result.isFailure()) {
                List<Job> dependents = jobController.onFailure(job);
                job.onCanceled();
                Stream.of(dependents).forEach(Job::onCanceled);

                if (result.getException() != null) {
                    throw result.getException();
                }
            } else {
                throw new AssertionError("Invalid job result!");
            }
        }
    }

    private Job.Result run(@NonNull Job job) {
        if (isJobExpired(job)) {
            return Job.Result.failure();
        }

        Job.Result result = null;
        PowerManager.WakeLock wakeLock = null;

        try {
            wakeLock = WakeLockUtil.acquire(application, PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TIMEOUT, job.getId());
            result = job.run();
        } catch (Exception e) {
            return Job.Result.failure();
        } finally {
            if (wakeLock != null) {
                WakeLockUtil.release(wakeLock, job.getId());
            }
        }

        printResult(job, result);

        if (result.isRetry() &&
                job.getRunAttempt() + 1 >= job.getParameters().getMaxAttempts() &&
                job.getParameters().getMaxAttempts() != Job.Parameters.UNLIMITED) {
            return Job.Result.failure();
        }

        return result;
    }

    private boolean isJobExpired(@NonNull Job job) {
        long expirationTime = job.getParameters().getCreateTime() + job.getParameters().getLifespan();

        if (expirationTime < 0) {
            expirationTime = Long.MAX_VALUE;
        }

        return job.getParameters().getLifespan() != Job.Parameters.IMMORTAL && expirationTime <= System.currentTimeMillis();
    }

    private void printResult(@NonNull Job job, @NonNull Job.Result result) {
        if (result.getException() != null) {
            Log.e(TAG, "Job failed with a fatal exception. Crash imminent.");
        } else if (result.isFailure()) {
            Log.w(TAG, "Job failed.");
        } else {
            Log.i(TAG, "Job finished with result: " + result + ", key: " + job.getFactoryKey());
        }
    }

}
