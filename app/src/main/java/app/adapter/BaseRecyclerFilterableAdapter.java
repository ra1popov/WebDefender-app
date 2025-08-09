package app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseRecyclerFilterableAdapter<T extends DataFilterable, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> implements Filterable {

    protected final Context context;
    protected List<T> originalList;
    protected List<T> list;
    public final LayoutInflater layoutInflater;
    public OnItemClickListener onItemClickListener;
    private CharSequence lastSearchText;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public BaseRecyclerFilterableAdapter(Context context, List<T> list) {
        this.context = context;
        this.originalList = list;
        this.list = list;
        this.layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<T> list) {
        this.originalList = list;
        this.list = list;

        notifyDataSetChanged();

        if (filter != null && !TextUtils.isEmpty(lastSearchText)) {
            filter.filter(lastSearchText);
        }
    }

    public List<T> getList() {
        return list;
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

    //////////////////////////////////////////////////////////////////////////////////////////

    protected Filter filter;

    @Override
    public Filter getFilter() {
        filter = new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                lastSearchText = constraint;
                List<T> filteredResults;
                if (constraint.length() == 0) {
                    filteredResults = originalList;
                } else {
                    filteredResults = getFilteredResults(constraint.toString().toLowerCase());
                }

                FilterResults results = new FilterResults();
                results.values = filteredResults;

                return results;
            }

            @SuppressLint("NotifyDataSetChanged")
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                list = (List<T>) results.values;
                notifyDataSetChanged();
            }

        };

        return filter;
    }

    protected List<T> getFilteredResults(String constraint) {
        List<T> results = new ArrayList<>();

        for (T item : originalList) {
            if (item.title.toLowerCase().contains(constraint)) {
                results.add(item);
            }
        }

        return results;
    }
}

