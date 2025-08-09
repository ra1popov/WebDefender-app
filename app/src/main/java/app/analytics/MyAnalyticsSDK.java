package app.analytics;

import android.app.Application;

import java.util.concurrent.Callable;

import app.dependencies.ApplicationDependencies;
import app.jobs.LogJob;

public class MyAnalyticsSDK {

    public static void init(Application application) {
    }

    public static void log(String msg) {
        ApplicationDependencies.getJobManager().add(new LogJob(msg));
    }

    public static void log(String msg, Callable<Void> callable) {
        LogJob job = new LogJob(msg);
        ApplicationDependencies.getJobManager().addListener(job.getId(), jobState -> {
            if (jobState.isComplete()) {
                try {
                    callable.call();
                } catch (Exception ignored) {
                }
            }
        });
        ApplicationDependencies.getJobManager().add(job);
    }

}
