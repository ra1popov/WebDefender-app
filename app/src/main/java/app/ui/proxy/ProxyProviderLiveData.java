package app.ui.proxy;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

public abstract class ProxyProviderLiveData<T> extends MutableLiveData<T> {

    protected final Context context;
    private ProxyObserver observer;

    public ProxyProviderLiveData(@NonNull Context context) {
        this.context = context;
    }

    @Override
    protected void onActive() {
        super.onActive();

        observer = () -> new Thread(() -> postValue(getContentProviderValue())).start();
        registerObserver(observer);

        new Thread(() -> postValue(getContentProviderValue())).start();
    }

    @Override
    protected void onInactive() {
        super.onInactive();

        unregisterObserver(observer);
    }

    protected abstract T getContentProviderValue();

    protected abstract void registerObserver(ProxyObserver observer);

    protected abstract void unregisterObserver(ProxyObserver observer);

}
