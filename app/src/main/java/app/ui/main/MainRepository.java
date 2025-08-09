package app.ui.main;

import android.content.Context;

import androidx.annotation.NonNull;

import app.dependencies.ApplicationDependencies;

public class MainRepository {

    private final Context context;
    private final MainStatLiveData stat;

    public MainRepository(@NonNull Context context) {
        this.context = context;
        this.stat = new MainStatLiveData(context, ApplicationDependencies.getPreferences());
    }

    public MainStatLiveData getStat() {
        return stat;
    }

}
