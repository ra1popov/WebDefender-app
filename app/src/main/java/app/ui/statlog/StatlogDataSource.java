package app.ui.statlog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingSource;
import androidx.paging.PagingState;

import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import kotlin.coroutines.Continuation;

public class StatlogDataSource extends PagingSource<Long, Statlog> {

    private final RuntimeExceptionDao<Statlog, Long> dao;

    public StatlogDataSource(@NonNull RuntimeExceptionDao<Statlog, Long> dao) {
        this.dao = dao;
    }

    @Nullable
    @Override
    public Long getRefreshKey(@NonNull PagingState<Long, Statlog> pagingState) {
        return null;
    }

    @Nullable
    @Override
    public LoadResult<Long, Statlog> load(@NonNull LoadParams<Long> loadParams, @NonNull Continuation<? super LoadResult<Long, Statlog>> continuation) {
        long offset = loadParams.getKey() != null ? loadParams.getKey() : 0;
        try {

            List<Statlog> data = dao.queryBuilder()
                    .orderBy(Statlog.FIELD_ID, false)
                    .offset(offset)
                    .limit((long) loadParams.getLoadSize())
                    .query();

            boolean reachedEndOfList = data.size() < loadParams.getLoadSize();
            Long nextKey = reachedEndOfList ? null : offset + data.size();
            Long prevKey = offset >= loadParams.getLoadSize() ? offset - loadParams.getLoadSize() : null;

            return new LoadResult.Page<>(data, prevKey, nextKey);
        } catch (SQLException ignored) {
        }
        return new LoadResult.Page<>(Collections.emptyList(), null, null);
    }

}