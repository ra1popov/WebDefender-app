package app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

public abstract class BasePagingRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends PagingDataAdapter<T, VH> {

    protected final Context context;
    protected final LayoutInflater layoutInflater;
    protected OnItemClickListener onItemClickListener;

    public BasePagingRecyclerAdapter(Context context, @NonNull DiffUtil.ItemCallback<T> diffCallback) {
        super(diffCallback);
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public abstract void onBindViewHolder(@NonNull VH holder, final int position);

    @NonNull
    @Override
    public abstract VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType);

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        super.onViewRecycled(holder);
    }

    protected int getColor(@ColorRes int resId) {
        return context.getResources().getColor(resId);
    }

}
