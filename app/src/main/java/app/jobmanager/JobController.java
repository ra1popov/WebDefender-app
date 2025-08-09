package app.jobmanager;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import app.jobmanager.persistence.ConstraintSpec;
import app.jobmanager.persistence.DependencySpec;
import app.jobmanager.persistence.FullSpec;
import app.jobmanager.persistence.JobSpec;
import app.jobmanager.persistence.JobStorage;

class JobController {

    private final Application application;
    private final JobStorage jobStorage;
    private final JobInstantiator jobInstantiator;
    private final ConstraintInstantiator constraintInstantiator;
    private final Data.Serializer dataSerializer;
    private final JobTracker jobTracker;
    private final Debouncer debouncer;
    private final Callback callback;
    private final Set<String> runningJobs;


    JobController(@NonNull Application application,
                  @NonNull JobStorage jobStorage,
                  @NonNull JobInstantiator jobInstantiator,
                  @NonNull ConstraintInstantiator constraintInstantiator,
                  @NonNull Data.Serializer dataSerializer,
                  @NonNull JobTracker jobTracker,
                  @NonNull Debouncer debouncer,
                  @NonNull Callback callback) {
        this.application = application;
        this.jobStorage = jobStorage;
        this.jobInstantiator = jobInstantiator;
        this.constraintInstantiator = constraintInstantiator;
        this.dataSerializer = dataSerializer;
        this.jobTracker = jobTracker;
        this.debouncer = debouncer;
        this.callback = callback;
        this.runningJobs = new HashSet<>();
    }

    @WorkerThread
    synchronized void init() {
        jobStorage.updateAllJobsToBePending();
        notifyAll();
    }

    synchronized void wakeUp() {
        notifyAll();
    }

    @WorkerThread
    synchronized void submitNewJobChain(@NonNull List<List<Job>> chain) {
        chain = Stream.of(chain).filterNot(List::isEmpty).toList();

        if (chain.isEmpty()) {
            return;
        }

        if (chainExceedsMaximumInstances(chain)) {
            Job solo = chain.get(0).get(0);
            jobTracker.onStateChange(solo.getId(), JobTracker.JobState.IGNORED);
            return;
        }

        insertJobChain(chain);
        scheduleJobs(chain.get(0));
        triggerOnSubmit(chain);
        notifyAll();
    }

    @WorkerThread
    synchronized void onRetry(@NonNull Job job) {
        int nextRunAttempt = job.getRunAttempt() + 1;
        long nextRunAttemptTime = calculateNextRunAttemptTime(System.currentTimeMillis(), nextRunAttempt, job.getParameters().getMaxBackoff());
        String serializedData = dataSerializer.serialize(job.serialize());

        jobStorage.updateJobAfterRetry(job.getId(), false, nextRunAttempt, nextRunAttemptTime, serializedData);
        jobTracker.onStateChange(job.getId(), JobTracker.JobState.PENDING);

        List<Constraint> constraints = Stream.of(jobStorage.getConstraintSpecs(job.getId()))
                .map(ConstraintSpec::getFactoryKey)
                .map(constraintInstantiator::instantiate)
                .toList();


        long delay = Math.max(0, nextRunAttemptTime - System.currentTimeMillis());

        notifyAll();
    }

    synchronized void onJobFinished(@NonNull Job job) {
        runningJobs.remove(job.getId());
    }

    @WorkerThread
    synchronized void onSuccess(@NonNull Job job) {
        jobStorage.deleteJob(job.getId());
        jobTracker.onStateChange(job.getId(), JobTracker.JobState.SUCCESS);
        notifyAll();
    }

    /**
     * @return The list of all dependent jobs that should also be failed.
     */
    @WorkerThread
    @NonNull
    synchronized List<Job> onFailure(@NonNull Job job) {
        List<Job> dependents = Stream.of(jobStorage.getDependencySpecsThatDependOnJob(job.getId()))
                .map(DependencySpec::getJobId)
                .map(jobStorage::getJobSpec)
                .withoutNulls()
                .map(jobSpec -> {
                    List<ConstraintSpec> constraintSpecs = jobStorage.getConstraintSpecs(jobSpec.getId());
                    return createJob(jobSpec, constraintSpecs);
                })
                .toList();

        List<Job> all = new ArrayList<>(dependents.size() + 1);
        all.add(job);
        all.addAll(dependents);

        jobStorage.deleteJobs(Stream.of(all).map(Job::getId).toList());
        Stream.of(all).forEach(j -> jobTracker.onStateChange(j.getId(), JobTracker.JobState.FAILURE));

        return dependents;
    }

    /**
     * Retrieves the next job that is eligible for execution. To be 'eligible' means that the job:
     * - Has no dependencies
     * - Has no unmet constraints
     * <p>
     * This method will block until a job is available.
     * When the job returned from this method has been run, you must call {@link #onJobFinished(Job)}.
     */
    @WorkerThread
    @NonNull
    synchronized Job pullNextEligibleJobForExecution() {
        try {
            Job job;

            while ((job = getNextEligibleJobForExecution()) == null) {
                if (runningJobs.isEmpty()) {
                    debouncer.publish(callback::onEmpty);
                }

                wait();
            }

            jobStorage.updateJobRunningState(job.getId(), true);
            runningJobs.add(job.getId());
            jobTracker.onStateChange(job.getId(), JobTracker.JobState.RUNNING);

            return job;
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Retrieves a string representing the state of the job queue. Intended for debugging.
     */
    @WorkerThread
    @NonNull
    synchronized String getDebugInfo() {
        List<JobSpec> jobs = jobStorage.getAllJobSpecs();
        List<ConstraintSpec> constraints = jobStorage.getAllConstraintSpecs();
        List<DependencySpec> dependencies = jobStorage.getAllDependencySpecs();

        StringBuilder info = new StringBuilder();

        info.append("-- Jobs\n");
        if (!jobs.isEmpty()) {
            Stream.of(jobs).forEach(j -> info.append(j.toString()).append('\n'));
        } else {
            info.append("None\n");
        }

        info.append("\n-- Constraints\n");
        if (!constraints.isEmpty()) {
            Stream.of(constraints).forEach(c -> info.append(c.toString()).append('\n'));
        } else {
            info.append("None\n");
        }

        info.append("\n-- Dependencies\n");
        if (!dependencies.isEmpty()) {
            Stream.of(dependencies).forEach(d -> info.append(d.toString()).append('\n'));
        } else {
            info.append("None\n");
        }

        return info.toString();
    }

    @WorkerThread
    private boolean chainExceedsMaximumInstances(@NonNull List<List<Job>> chain) {
        if (chain.size() == 1 && chain.get(0).size() == 1) {
            Job solo = chain.get(0).get(0);

            if (solo.getParameters().getMaxInstances() != Job.Parameters.UNLIMITED &&
                    jobStorage.getJobInstanceCount(solo.getFactoryKey()) >= solo.getParameters().getMaxInstances()) {
                return true;
            }
        }
        return false;
    }

    @WorkerThread
    private void triggerOnSubmit(@NonNull List<List<Job>> chain) {
        Stream.of(chain)
                .forEach(list -> Stream.of(list).forEach(job -> {
                    job.setContext(application);
                    job.onSubmit();
                }));
    }

    @WorkerThread
    private void insertJobChain(@NonNull List<List<Job>> chain) {
        List<FullSpec> fullSpecs = new LinkedList<>();
        List<Job> dependsOn = Collections.emptyList();

        for (List<Job> jobList : chain) {
            for (Job job : jobList) {
                fullSpecs.add(buildFullSpec(job, dependsOn));
            }
            dependsOn = jobList;
        }

        jobStorage.insertJobs(fullSpecs);
    }

    @WorkerThread
    @NonNull
    private FullSpec buildFullSpec(@NonNull Job job, @NonNull List<Job> dependsOn) {
        job.setRunAttempt(0);

        JobSpec jobSpec = new JobSpec(job.getId(),
                job.getFactoryKey(),
                job.getParameters().getQueue(),
                System.currentTimeMillis(),
                job.getNextRunAttemptTime(),
                job.getRunAttempt(),
                job.getParameters().getMaxAttempts(),
                job.getParameters().getMaxBackoff(),
                job.getParameters().getLifespan(),
                job.getParameters().getMaxInstances(),
                dataSerializer.serialize(job.serialize()),
                false);

        List<ConstraintSpec> constraintSpecs = Stream.of(job.getParameters().getConstraintKeys())
                .map(key -> new ConstraintSpec(jobSpec.getId(), key))
                .toList();

        List<DependencySpec> dependencySpecs = Stream.of(dependsOn)
                .map(depends -> new DependencySpec(job.getId(), depends.getId()))
                .toList();

        return new FullSpec(jobSpec, constraintSpecs, dependencySpecs);
    }

    @WorkerThread
    private void scheduleJobs(@NonNull List<Job> jobs) {
        for (Job job : jobs) {
            List<Constraint> constraints = Stream.of(job.getParameters().getConstraintKeys())
                    .map(key -> new ConstraintSpec(job.getId(), key))
                    .map(ConstraintSpec::getFactoryKey)
                    .map(constraintInstantiator::instantiate)
                    .toList();
        }
    }

    @WorkerThread
    @Nullable
    private Job getNextEligibleJobForExecution() {
        List<JobSpec> jobSpecs = jobStorage.getPendingJobsWithNoDependenciesInCreatedOrder(System.currentTimeMillis());

        for (JobSpec jobSpec : jobSpecs) {
            List<ConstraintSpec> constraintSpecs = jobStorage.getConstraintSpecs(jobSpec.getId());
            List<Constraint> constraints = Stream.of(constraintSpecs)
                    .map(ConstraintSpec::getFactoryKey)
                    .map(constraintInstantiator::instantiate)
                    .toList();

            if (Stream.of(constraints).allMatch(Constraint::isMet)) {
                return createJob(jobSpec, constraintSpecs);
            }
        }

        return null;
    }

    @NonNull
    private Job createJob(@NonNull JobSpec jobSpec, @NonNull List<ConstraintSpec> constraintSpecs) {
        Job.Parameters parameters = buildJobParameters(jobSpec, constraintSpecs);

        try {
            Data data = dataSerializer.deserialize(jobSpec.getSerializedData());
            Job job = jobInstantiator.instantiate(jobSpec.getFactoryKey(), parameters, data);

            job.setRunAttempt(jobSpec.getRunAttempt());
            job.setNextRunAttemptTime(jobSpec.getNextRunAttemptTime());
            job.setContext(application);

            return job;
        } catch (RuntimeException e) {

            List<String> failIds = Stream.of(jobStorage.getDependencySpecsThatDependOnJob(jobSpec.getId()))
                    .map(DependencySpec::getJobId)
                    .toList();

            jobStorage.deleteJob(jobSpec.getId());
            jobStorage.deleteJobs(failIds);

            throw e;
        }
    }

    @NonNull
    private Job.Parameters buildJobParameters(@NonNull JobSpec jobSpec, @NonNull List<ConstraintSpec> constraintSpecs) {
        return new Job.Parameters.Builder(jobSpec.getId())
                .setCreateTime(jobSpec.getCreateTime())
                .setLifespan(jobSpec.getLifespan())
                .setMaxAttempts(jobSpec.getMaxAttempts())
                .setQueue(jobSpec.getQueueKey())
                .setConstraints(Stream.of(constraintSpecs).map(ConstraintSpec::getFactoryKey).toList())
                .setMaxBackoff(jobSpec.getMaxBackoff())
                .build();
    }

    private long calculateNextRunAttemptTime(long currentTime, int nextAttempt, long maxBackoff) {
        int boundedAttempt = Math.min(nextAttempt, 30);
        long exponentialBackoff = (long) Math.pow(2, boundedAttempt) * 1000;
        long actualBackoff = Math.min(exponentialBackoff, maxBackoff);

        return currentTime + actualBackoff;
    }

    interface Callback {
        void onEmpty();
    }

}

