package app.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.jobmanager.Job;

public abstract class BaseJob extends Job {

    public BaseJob(@NonNull Parameters parameters) {
        super(parameters);
    }

    @Override
    @NonNull
    public Result run() {
        try {
            onRun();
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fatalFailure(e);
        } catch (Exception e) {
            if (onShouldRetry(e)) {
                return Result.retry();
            } else {
                return Result.failure();
            }
        }
    }

    protected abstract void onRun() throws Exception;

    protected abstract boolean onShouldRetry(@NonNull Exception e);

    protected void warn(@NonNull String tag, @NonNull String message) {
        warn(tag, message, null);
    }

    protected void warn(@NonNull String tag, @Nullable Throwable t) {
        warn(tag, "", t);
    }

    protected void warn(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {

    }

}
