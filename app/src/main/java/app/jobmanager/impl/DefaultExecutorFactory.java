package app.jobmanager.impl;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.jobmanager.ExecutorFactory;


public class DefaultExecutorFactory implements ExecutorFactory {
    @Override
    @NonNull
    public ExecutorService newSingleThreadExecutor(@NonNull String name) {
        return Executors.newSingleThreadExecutor(r -> new Thread(r, name));
    }
}
