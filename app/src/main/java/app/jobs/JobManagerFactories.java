package app.jobs;

import android.app.Application;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.jobmanager.Constraint;
import app.jobmanager.ConstraintObserver;
import app.jobmanager.Job;
import app.jobmanager.impl.NetworkConstraint;
import app.jobmanager.impl.NetworkConstraintObserver;

public final class JobManagerFactories {

    public static Map<String, Job.Factory<?>> getJobFactories(@NonNull Application application) {
        return new HashMap<>() {{
            put(UpdaterJob.KEY, new UpdaterJob.Factory());
            put(InitializerJob.KEY, new InitializerJob.Factory());
            put(LogJob.KEY, new LogJob.Factory());
        }};
    }

    public static Map<String, Constraint.Factory<?>> getConstraintFactories(@NonNull Application application) {
        return new HashMap<>() {{
            put(NetworkConstraint.KEY, new NetworkConstraint.Factory(application));
        }};
    }

    public static List<ConstraintObserver> getConstraintObservers(@NonNull Application application) {
        return Collections.singletonList(new NetworkConstraintObserver(application));
    }

}
