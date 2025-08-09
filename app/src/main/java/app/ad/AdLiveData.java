package app.ad;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.Config;
import app.api.ApiClient;
import app.dependencies.ApplicationDependencies;
import app.preferences.Preferences;

public class AdLiveData extends AdProviderLiveData<List<Ad>> {

    private final ApiClient apiClient;
    private final Preferences preferences;
    private final List<AdObserver> observers = new ArrayList<>();

    public AdLiveData(@NonNull Context context) {
        super(context);
        this.apiClient = ApplicationDependencies.getApiClient();
        this.preferences = ApplicationDependencies.getPreferences();
    }

    @Override
    protected List<Ad> getContentProviderValue() {
        if (!preferences.isNeedShowAdDialog()) {
            return new ArrayList<>();
        }

        Ad ad = apiClient.getAd();
        if (ad == null) {
            return new ArrayList<>();
        }

        if (!Config.DEBUG) {
            preferences.updateLastTimeAd();
        }

        return Collections.singletonList(ad);
    }

    @Override
    protected void registerObserver(AdObserver observer) {
        observers.add(observer);
    }

    @Override
    protected void unregisterObserver(AdObserver observer) {
        observers.remove(observer);
    }

    public void requestAd() {
        for (AdObserver observer : observers) {
            observer.onChange();
        }
    }

}
