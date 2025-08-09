package app.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

public abstract class BroadcastLiveData<T> extends MutableLiveData<T> {

    protected final Context context;
    protected final IntentFilter intent;
    private BroadcastReceiver observer;

    public BroadcastLiveData(@NonNull Context context, @NonNull IntentFilter intent) {
        this.context = context;
        this.intent = intent;
    }

    @Override
    protected void onActive() {
        super.onActive();

        observer = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                new Thread(() -> postValue(getContentProviderValue())).start();
            }
        };
        context.registerReceiver(observer, intent);

        new Thread(() -> postValue(getContentProviderValue())).start();
    }

    @Override
    protected void onInactive() {
        super.onInactive();

        context.unregisterReceiver(observer);
    }

    protected abstract T getContentProviderValue();

}