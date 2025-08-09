package app.jobs;

import androidx.annotation.NonNull;

import app.dependencies.ApplicationDependencies;
import app.jobmanager.Data;
import app.jobmanager.Job;

public class LogJob extends BaseJob {

    public static final String QUEUE = "Log";
    public static final String KEY = "LogJob";
    private static final String KEY_MSG = "msg";

    private final String msg;

    public LogJob(String msg) {
        this(new Parameters.Builder()
                        .setQueue(QUEUE)
//                        .addConstraint(NetworkConstraint.KEY)
                        .setMaxAttempts(1)
//                        .setLifespan(TimeUnit.SECONDS.toMillis(5))
                        .build(),
                msg);
    }

    private LogJob(@NonNull Parameters parameters, String msg) {
        super(parameters);
        this.msg = msg;
    }

    @Override
    @NonNull
    public Data serialize() {
        return new Data.Builder()
                .putString(KEY_MSG, msg)
                .build();
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
        ApplicationDependencies.getApiClient().log(msg);
    }

    @Override
    protected boolean onShouldRetry(@NonNull Exception exception) {
        return false;
    }

    public static final class Factory implements Job.Factory<LogJob> {
        @Override
        @NonNull
        public LogJob create(@NonNull Parameters parameters, @NonNull Data data) {
            return new LogJob(parameters, data.getString(KEY_MSG));
        }
    }

}
