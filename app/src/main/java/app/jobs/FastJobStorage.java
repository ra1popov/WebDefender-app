package app.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import app.jobmanager.persistence.ConstraintSpec;
import app.jobmanager.persistence.DependencySpec;
import app.jobmanager.persistence.FullSpec;
import app.jobmanager.persistence.JobSpec;
import app.jobmanager.persistence.JobStorage;

public class FastJobStorage implements JobStorage {

    private final List<JobSpec> jobs;
    private final Map<String, List<ConstraintSpec>> constraintsByJobId;
    private final Map<String, List<DependencySpec>> dependenciesByJobId;

    public FastJobStorage() {
        this.jobs = new ArrayList<>();
        this.constraintsByJobId = new HashMap<>();
        this.dependenciesByJobId = new HashMap<>();
    }

    @Override
    public synchronized void init() {

    }


    @Override
    public synchronized void insertJobs(@NonNull List<FullSpec> fullSpecs) {
        for (FullSpec fullSpec : fullSpecs) {
            jobs.add(fullSpec.getJobSpec());
            constraintsByJobId.put(fullSpec.getJobSpec().getId(), fullSpec.getConstraintSpecs());
            dependenciesByJobId.put(fullSpec.getJobSpec().getId(), fullSpec.getDependencySpecs());
        }
    }

    @Override
    public synchronized @Nullable
    JobSpec getJobSpec(@NonNull String id) {
        for (JobSpec jobSpec : jobs) {
            if (jobSpec.getId().equals(id)) {
                return jobSpec;
            }
        }
        return null;
    }

    @Override
    public synchronized @NonNull
    List<JobSpec> getAllJobSpecs() {
        return new ArrayList<>(jobs);
    }

    @Override
    public synchronized @NonNull
    List<JobSpec> getPendingJobsWithNoDependenciesInCreatedOrder(long currentTime) {
        return Stream.of(jobs)
                .filterNot(JobSpec::isRunning)
                .filter(this::firstInQueue)
                .filter(j -> !dependenciesByJobId.containsKey(j.getId()) || dependenciesByJobId.get(j.getId()).isEmpty())
                .filter(j -> j.getNextRunAttemptTime() <= currentTime)
                .sorted(Comparator.comparingLong(JobSpec::getCreateTime))
                .toList();
    }

    private boolean firstInQueue(@NonNull JobSpec job) {
        if (job.getQueueKey() == null) {
            return true;
        }

        return Stream.of(jobs)
                .filter(j -> Objects.equals(j.getQueueKey(), job.getQueueKey()))
                .sorted(Comparator.comparingLong(JobSpec::getCreateTime))
                .toList()
                .get(0)
                .equals(job);
    }

    @Override
    public synchronized int getJobInstanceCount(@NonNull String factoryKey) {
        return (int) Stream.of(jobs)
                .filter(j -> j.getFactoryKey().equals(factoryKey))
                .count();
    }

    @Override
    public synchronized void updateJobRunningState(@NonNull String id, boolean isRunning) {
        ListIterator<JobSpec> iter = jobs.listIterator();

        while (iter.hasNext()) {
            JobSpec existing = iter.next();
            if (existing.getId().equals(id)) {
                JobSpec updated = new JobSpec(existing.getId(),
                        existing.getFactoryKey(),
                        existing.getQueueKey(),
                        existing.getCreateTime(),
                        existing.getNextRunAttemptTime(),
                        existing.getRunAttempt(),
                        existing.getMaxAttempts(),
                        existing.getMaxBackoff(),
                        existing.getLifespan(),
                        existing.getMaxInstances(),
                        existing.getSerializedData(),
                        isRunning);
                iter.set(updated);
            }
        }
    }

    @Override
    public synchronized void updateJobAfterRetry(@NonNull String id, boolean isRunning, int runAttempt, long nextRunAttemptTime, @NonNull String serializedData) {
        ListIterator<JobSpec> iter = jobs.listIterator();

        while (iter.hasNext()) {
            JobSpec existing = iter.next();
            if (existing.getId().equals(id)) {
                JobSpec updated = new JobSpec(existing.getId(),
                        existing.getFactoryKey(),
                        existing.getQueueKey(),
                        existing.getCreateTime(),
                        nextRunAttemptTime,
                        runAttempt,
                        existing.getMaxAttempts(),
                        existing.getMaxBackoff(),
                        existing.getLifespan(),
                        existing.getMaxInstances(),
                        serializedData,
                        isRunning);
                iter.set(updated);
            }
        }
    }

    @Override
    public synchronized void updateAllJobsToBePending() {
        ListIterator<JobSpec> iter = jobs.listIterator();

        while (iter.hasNext()) {
            JobSpec existing = iter.next();
            JobSpec updated = new JobSpec(existing.getId(),
                    existing.getFactoryKey(),
                    existing.getQueueKey(),
                    existing.getCreateTime(),
                    existing.getNextRunAttemptTime(),
                    existing.getRunAttempt(),
                    existing.getMaxAttempts(),
                    existing.getMaxBackoff(),
                    existing.getLifespan(),
                    existing.getMaxInstances(),
                    existing.getSerializedData(),
                    false);
            iter.set(updated);
        }
    }

    @Override
    public void updateJobs(@NonNull List<JobSpec> jobSpecs) {
        Map<String, JobSpec> updates = Stream.of(jobSpecs).collect(Collectors.toMap(JobSpec::getId));
        ListIterator<JobSpec> iter = jobs.listIterator();

        while (iter.hasNext()) {
            JobSpec existing = iter.next();
            JobSpec update = updates.get(existing.getId());

            if (update != null) {
                iter.set(update);
            }
        }
    }

    @Override
    public synchronized void deleteJob(@NonNull String jobId) {
        deleteJobs(Collections.singletonList(jobId));
    }

    @Override
    public synchronized void deleteJobs(@NonNull List<String> jobIds) {
        Set<String> deleteIds = new HashSet<>(jobIds);

        Iterator<JobSpec> jobIter = jobs.iterator();
        while (jobIter.hasNext()) {
            if (deleteIds.contains(jobIter.next().getId())) {
                jobIter.remove();
            }
        }

        for (String jobId : jobIds) {
            constraintsByJobId.remove(jobId);
            dependenciesByJobId.remove(jobId);

            for (Map.Entry<String, List<DependencySpec>> entry : dependenciesByJobId.entrySet()) {
                Iterator<DependencySpec> depedencyIter = entry.getValue().iterator();

                while (depedencyIter.hasNext()) {
                    if (depedencyIter.next().getDependsOnJobId().equals(jobId)) {
                        depedencyIter.remove();
                    }
                }
            }
        }
    }

    @Override
    public synchronized @NonNull
    List<ConstraintSpec> getConstraintSpecs(@NonNull String jobId) {
        return Util.getOrDefault(constraintsByJobId, jobId, new LinkedList<>());
    }

    @Override
    public synchronized @NonNull
    List<ConstraintSpec> getAllConstraintSpecs() {
        return Stream.of(constraintsByJobId)
                .map(Map.Entry::getValue)
                .flatMap(Stream::of)
                .toList();
    }

    @Override
    public synchronized @NonNull
    List<DependencySpec> getDependencySpecsThatDependOnJob(@NonNull String jobSpecId) {
        List<DependencySpec> layer = getSingleLayerOfDependencySpecsThatDependOnJob(jobSpecId);
        List<DependencySpec> all = new ArrayList<>(layer);

        Set<String> activeJobIds;

        do {
            activeJobIds = Stream.of(layer).map(DependencySpec::getJobId).collect(Collectors.toSet());
            layer.clear();

            for (String activeJobId : activeJobIds) {
                layer.addAll(getSingleLayerOfDependencySpecsThatDependOnJob(activeJobId));
            }

            all.addAll(layer);
        } while (!layer.isEmpty());

        return all;
    }

    private @NonNull
    List<DependencySpec> getSingleLayerOfDependencySpecsThatDependOnJob(@NonNull String jobSpecId) {
        return Stream.of(dependenciesByJobId.entrySet())
                .map(Map.Entry::getValue)
                .flatMap(Stream::of)
                .filter(j -> j.getDependsOnJobId().equals(jobSpecId))
                .toList();
    }

    @Override
    public @NonNull
    List<DependencySpec> getAllDependencySpecs() {
        return Stream.of(dependenciesByJobId)
                .map(Map.Entry::getValue)
                .flatMap(Stream::of)
                .toList();
    }

}
