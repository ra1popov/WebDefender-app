package app.jobs;

import androidx.annotation.NonNull;

import app.jobmanager.Data;
import app.jobmanager.Job;
import app.updater.UpdaterService;

public class UpdaterJob extends BaseJob {

    public static final String QUEUE = "Main";
    public static final String KEY = "UpdaterJob";
    private static final String KEY_ID = "id";

    private final int id;

    public UpdaterJob(int id) {
        this(new Parameters.Builder()
                        .setQueue(QUEUE)
//                        .addConstraint(NetworkConstraint.KEY)
                        .setMaxAttempts(1)
//                        .setLifespan(TimeUnit.SECONDS.toMillis(5))
                        .build(),
                id);
    }

    private UpdaterJob(@NonNull Parameters parameters, int id) {
        super(parameters);
        this.id = id;
    }

    @Override
    @NonNull
    public Data serialize() {
        return new Data.Builder()
                .putInt(KEY_ID, id)
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
        new UpdaterService(id).run();
    }

    @Override
    protected boolean onShouldRetry(@NonNull Exception exception) {
        return false;
    }

    public static final class Factory implements Job.Factory<UpdaterJob> {
        @Override
        @NonNull
        public UpdaterJob create(@NonNull Parameters parameters, @NonNull Data data) {
            return new UpdaterJob(parameters, data.getInt(KEY_ID));
        }
    }

}
