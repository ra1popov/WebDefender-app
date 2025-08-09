package app.jobmanager;

import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Keep
public abstract class Job {

    private final Parameters parameters;

    private int runAttempt;
    private long nextRunAttemptTime;

    protected Context context;

    public Job(@NonNull Parameters parameters) {
        this.parameters = parameters;
    }

    @NonNull
    public final String getId() {
        return parameters.getId();
    }

    @NonNull
    public final Parameters getParameters() {
        return parameters;
    }

    public final int getRunAttempt() {
        return runAttempt;
    }

    public final long getNextRunAttemptTime() {
        return nextRunAttemptTime;
    }

    /**
     * This is already called by {@link JobController} during job submission, but if you ever run a
     * job without submitting it to the {@link JobManager}, then you'll need to invoke this yourself.
     */
    public final void setContext(@NonNull Context context) {
        this.context = context;
    }

    /**
     * Should only be invoked by {@link JobController}
     */
    final void setRunAttempt(int runAttempt) {
        this.runAttempt = runAttempt;
    }

    /**
     * Should only be invoked by {@link JobController}
     */
    final void setNextRunAttemptTime(long nextRunAttemptTime) {
        this.nextRunAttemptTime = nextRunAttemptTime;
    }

    @WorkerThread
    final void onSubmit() {
        onAdded();
    }

    /**
     * Called when the job is first submitted to the {@link JobManager}.
     */
    @WorkerThread
    public void onAdded() {
    }

    /**
     * Called after a job has run and its determined that a retry is required.
     */
    @WorkerThread
    public void onRetry() {
    }

    /**
     * Serialize your job state so that it can be recreated in the future.
     */
    @NonNull
    public abstract Data serialize();

    /**
     * Returns the key that can be used to find the relevant factory needed to create your job.
     */
    @NonNull
    public abstract String getFactoryKey();

    /**
     * Called to do your actual work.
     */
    @WorkerThread
    @NonNull
    public abstract Result run();

    /**
     * Called when your job has completely failed.
     */
    @WorkerThread
    public abstract void onCanceled();

    public interface Factory<T extends Job> {
        @NonNull
        T create(@NonNull Parameters parameters, @NonNull Data data);
    }

    public static final class Result {

        private static final Result SUCCESS = new Result(ResultType.SUCCESS, null);
        private static final Result RETRY = new Result(ResultType.RETRY, null);
        private static final Result FAILURE = new Result(ResultType.FAILURE, null);

        private final ResultType resultType;
        private final RuntimeException runtimeException;

        private Result(@NonNull ResultType resultType, @Nullable RuntimeException runtimeException) {
            this.resultType = resultType;
            this.runtimeException = runtimeException;
        }

        /**
         * Job completed successfully.
         */
        public static Result success() {
            return SUCCESS;
        }

        /**
         * Job did not complete successfully, but it can be retried later.
         */
        public static Result retry() {
            return RETRY;
        }

        /**
         * Job did not complete successfully and should not be tried again. Dependent jobs will also be failed.
         */
        public static Result failure() {
            return FAILURE;
        }

        /**
         * Same as {@link #failure()}, except the app should also crash with the provided exception.
         */
        public static Result fatalFailure(@NonNull RuntimeException runtimeException) {
            return new Result(ResultType.FAILURE, runtimeException);
        }

        boolean isSuccess() {
            return resultType == ResultType.SUCCESS;
        }

        boolean isRetry() {
            return resultType == ResultType.RETRY;
        }

        boolean isFailure() {
            return resultType == ResultType.FAILURE;
        }

        @Nullable
        RuntimeException getException() {
            return runtimeException;
        }

        @Override
        @NonNull
        public String toString() {
            switch (resultType) {
                case SUCCESS:
                case RETRY:
                    return resultType.toString();
                case FAILURE:
                    if (runtimeException == null) {
                        return resultType.toString();
                    } else {
                        return "FATAL_FAILURE";
                    }
            }

            return "UNKNOWN?";
        }

        private enum ResultType {
            SUCCESS, FAILURE, RETRY
        }
    }

    public static final class Parameters {

        public static final String MIGRATION_QUEUE_KEY = "MIGRATION";
        public static final int IMMORTAL = -1;
        public static final int UNLIMITED = -1;

        private final String id;
        private final long createTime;
        private final long lifespan;
        private final int maxAttempts;
        private final long maxBackoff;
        private final int maxInstances;
        private final String queue;
        private final List<String> constraintKeys;

        private Parameters(@NonNull String id,
                           long createTime,
                           long lifespan,
                           int maxAttempts,
                           long maxBackoff,
                           int maxInstances,
                           @Nullable String queue,
                           @NonNull List<String> constraintKeys) {
            this.id = id;
            this.createTime = createTime;
            this.lifespan = lifespan;
            this.maxAttempts = maxAttempts;
            this.maxBackoff = maxBackoff;
            this.maxInstances = maxInstances;
            this.queue = queue;
            this.constraintKeys = constraintKeys;
        }

        @NonNull
        String getId() {
            return id;
        }

        long getCreateTime() {
            return createTime;
        }

        long getLifespan() {
            return lifespan;
        }

        int getMaxAttempts() {
            return maxAttempts;
        }

        long getMaxBackoff() {
            return maxBackoff;
        }

        int getMaxInstances() {
            return maxInstances;
        }

        @Nullable
        public String getQueue() {
            return queue;
        }

        @NonNull
        List<String> getConstraintKeys() {
            return constraintKeys;
        }

        public Builder toBuilder() {
            return new Builder(id, createTime, maxBackoff, lifespan, maxAttempts, maxInstances, queue, constraintKeys);
        }


        public static final class Builder {

            private String id;
            private long createTime;
            private long maxBackoff;
            private long lifespan;
            private int maxAttempts;
            private int maxInstances;
            private String queue;
            private List<String> constraintKeys;

            public Builder() {
                this(UUID.randomUUID().toString());
            }

            Builder(@NonNull String id) {
                this(id, System.currentTimeMillis(), TimeUnit.SECONDS.toMillis(30), IMMORTAL, 1, UNLIMITED, null, new LinkedList<>());
            }

            private Builder(@NonNull String id,
                            long createTime,
                            long maxBackoff,
                            long lifespan,
                            int maxAttempts,
                            int maxInstances,
                            @Nullable String queue,
                            @NonNull List<String> constraintKeys) {
                this.id = id;
                this.createTime = createTime;
                this.maxBackoff = maxBackoff;
                this.lifespan = lifespan;
                this.maxAttempts = maxAttempts;
                this.maxInstances = maxInstances;
                this.queue = queue;
                this.constraintKeys = constraintKeys;
            }

            /**
             * Should only be invoked by {@link JobController}
             */
            Builder setCreateTime(long createTime) {
                this.createTime = createTime;
                return this;
            }

            /**
             * Specify the amount of time this job is allowed to be retried. Defaults to {@link #IMMORTAL}.
             */
            @NonNull
            public Builder setLifespan(long lifespan) {
                this.lifespan = lifespan;
                return this;
            }

            /**
             * Specify the maximum number of times you want to attempt this job. Defaults to 1.
             */
            @NonNull
            public Builder setMaxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
                return this;
            }

            /**
             * Specify the longest amount of time to wait between retries. No guarantees that this will
             * be respected on API >= 26.
             */
            @NonNull
            public Builder setMaxBackoff(long maxBackoff) {
                this.maxBackoff = maxBackoff;
                return this;
            }

            /**
             * Specify the maximum number of instances you'd want of this job at any given time. If
             * enqueueing this job would put it over that limit, it will be ignored.
             * <p>
             * Duplicates are determined by two jobs having the same {@link Job#getFactoryKey()}.
             * <p>
             * This property is ignored if the job is submitted as part of a {@link JobManager.Chain}.
             * <p>
             * Defaults to {@link #UNLIMITED}.
             */
            @NonNull
            public Builder setMaxInstances(int maxInstances) {
                this.maxInstances = maxInstances;
                return this;
            }

            /**
             * Specify a string representing a queue. All jobs within the same queue are run in a
             * serialized fashion -- one after the other, in order of insertion. Failure of a job earlier
             * in the queue has no impact on the execution of jobs later in the queue.
             */
            @NonNull
            public Builder setQueue(@Nullable String queue) {
                this.queue = queue;
                return this;
            }

            /**
             * Add a constraint via the key that was used to register its factory in
             * {@link JobManager.Configuration)};
             */
            @NonNull
            public Builder addConstraint(@NonNull String constraintKey) {
                constraintKeys.add(constraintKey);
                return this;
            }

            /**
             * Set constraints via the key that was used to register its factory in
             * {@link JobManager.Configuration)};
             */
            @NonNull
            public Builder setConstraints(@NonNull List<String> constraintKeys) {
                this.constraintKeys.clear();
                this.constraintKeys.addAll(constraintKeys);
                return this;
            }

            @NonNull
            public Parameters build() {
                return new Parameters(id, createTime, lifespan, maxAttempts, maxBackoff, maxInstances, queue, constraintKeys);
            }
        }
    }

}
