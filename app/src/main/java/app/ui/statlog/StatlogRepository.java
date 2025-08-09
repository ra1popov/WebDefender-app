package app.ui.statlog;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.paging.PagingConfig;

import app.dependencies.ApplicationDependencies;

public class StatlogRepository {

    private final Context context;
    private final StatlogRecordLiveData record;
    private final StatlogIconLiveData icon;

    public StatlogRepository(@NonNull Context context) {
        this.context = context;
        this.record = new StatlogRecordLiveData(context, ApplicationDependencies.getDatabaseHelper().getStatlogRecordsDao(), new PagingConfig(20, 5, false));
        this.icon = new StatlogIconLiveData(context);
    }

    public StatlogRecordLiveData getRecords() {
        return record;
    }

    public void requestRecords() {
        record.requestRecords();
    }

    public StatlogIconLiveData getIcons() {
        return icon;
    }

}
