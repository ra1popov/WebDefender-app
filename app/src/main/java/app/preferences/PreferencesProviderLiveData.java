package app.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

public abstract class PreferencesProviderLiveData<T> extends MutableLiveData<T> {

    protected final Context context;
    protected final Preferences preferences;
    private PreferencesObserver observer;


    public PreferencesProviderLiveData(@NonNull Context context, @NonNull Preferences preferences) {
        this.context = context;
        this.preferences = preferences;
    }

    @Override
    protected void onActive() {
        super.onActive();

        observer = () -> new Thread(() -> postValue(getContentProviderValue())).start();
        preferences.registerObserver(observer);

        new Thread(() -> postValue(getContentProviderValue())).start();
    }

    @Override
    protected void onInactive() {
        super.onInactive();

        preferences.unregisterObserver(observer);
    }

    protected abstract T getContentProviderValue();

}
