package app.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;

public abstract class DaoLiveData<T, V, K> extends MutableLiveData<T> {

    protected final Context context;
    protected final RuntimeExceptionDao<V, K> dao;
    private Dao.DaoObserver observer;

    public DaoLiveData(@NonNull Context context, @NonNull RuntimeExceptionDao<V, K> dao) {
        this.context = context;
        this.dao = dao;
    }

    @Override
    protected void onActive() {
        super.onActive();

        observer = () -> new Thread(() -> postValue(getContentProviderValue())).start();
        dao.registerObserver(observer);

        new Thread(() -> postValue(getContentProviderValue())).start();
    }

    @Override
    protected void onInactive() {
        super.onInactive();

        dao.unregisterObserver(observer);
    }

    protected abstract T getContentProviderValue();

}