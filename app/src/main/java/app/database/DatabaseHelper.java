package app.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.List;

import app.ui.statlog.Statlog;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private RuntimeExceptionDao<Statlog, Long> statlogRecordsDao = null;

    private static final String DATABASE_NAME = "webdefender.db";
    private static final int DATABASE_VERSION = 2;

    private OnDatabaseUpdateListener onDatabaseUpdateListener;

    public DatabaseHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void setOnDatabaseUpdateListener(OnDatabaseUpdateListener onDatabaseUpdateListener) {
        this.onDatabaseUpdateListener = onDatabaseUpdateListener;
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTableIfNotExists(connectionSource, Statlog.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        if (onDatabaseUpdateListener != null) {
            onDatabaseUpdateListener.onDatabaseUpdate();
        }

        if (oldVersion <= 1 && newVersion == DATABASE_VERSION) {
            try {
                TableUtils.dropTable(connectionSource, Statlog.class, true);
                TableUtils.createTableIfNotExists(connectionSource, Statlog.class);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (onDatabaseUpdateListener != null) {
            onDatabaseUpdateListener.onDatabaseUpdated(oldVersion, newVersion);
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    public RuntimeExceptionDao<Statlog, Long> getStatlogRecordsDao() {
        if (statlogRecordsDao == null) {
            statlogRecordsDao = getRuntimeExceptionDao(Statlog.class);
            statlogRecordsDao.setObjectCache(false);
        }
        return statlogRecordsDao;
    }

    public void clearStatlogTable() {
        try {
            TableUtils.clearTable(connectionSource, Statlog.class);
            if (statlogRecordsDao != null) {
                statlogRecordsDao.notifyChanges();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void batchWriteStatlog(List<Statlog> list) {
        try {
            RuntimeExceptionDao<Statlog, Long> statlogRecordsDao = getStatlogRecordsDao();
            TransactionManager.callInTransaction(statlogRecordsDao.getConnectionSource(), () -> {
                for (Statlog statlog : list) {
                    statlogRecordsDao.create(statlog);
                }
                return null;
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        super.close();

        statlogRecordsDao = null;
    }

}
