package app.ui.statlog;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;

import com.j256.ormlite.dao.RuntimeExceptionDao;

import app.database.DaoLiveData;
import kotlinx.coroutines.flow.Flow;

public class StatlogRecordLiveData extends DaoLiveData<Flow<PagingData<Statlog>>, Statlog, Long> {

    private final RuntimeExceptionDao<Statlog, Long> dao;
    private final PagingConfig config;

    public StatlogRecordLiveData(@NonNull Context context, @NonNull RuntimeExceptionDao<Statlog, Long> dao, @NonNull PagingConfig config) {
        super(context, dao);
        this.dao = dao;
        this.config = config;
    }

    @Override
    protected Flow<PagingData<Statlog>> getContentProviderValue() {
        Pager<Long, Statlog> pager = new Pager<>(config, () -> new StatlogDataSource(dao)); // Create an instance of Pager by specifying the configuration and a function to retrieve the data source.
        return pager.getFlow(); // Get the Flow from the Pager
    }

    public void requestRecords() {
        dao.notifyChanges();
    }

}
