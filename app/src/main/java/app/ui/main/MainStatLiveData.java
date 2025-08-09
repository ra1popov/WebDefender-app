package app.ui.main;

import android.content.Context;

import androidx.annotation.NonNull;

import app.preferences.Preferences;
import app.preferences.PreferencesProviderLiveData;
import app.preferences.model.Stat;

public class MainStatLiveData extends PreferencesProviderLiveData<Stat> {

    public MainStatLiveData(@NonNull Context context, @NonNull Preferences preferences) {
        super(context, preferences);
    }

    @Override
    protected Stat getContentProviderValue() {
        return preferences.getStat();
    }

}
