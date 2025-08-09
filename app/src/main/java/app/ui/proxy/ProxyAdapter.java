package app.ui.proxy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.Objects;

import app.R;
import app.adapter.BaseRecyclerAdapter;
import app.databinding.ItemProxyBinding;

public class ProxyAdapter extends BaseRecyclerAdapter<Proxy, RecyclerView.ViewHolder> {

    public interface OnProxyListener {
        void onClickProxy(@NonNull Proxy proxy);
    }

    public final OnProxyListener onProxyListener;

    public ProxyAdapter(Context context, @NonNull OnProxyListener listener) {
        super(context, Collections.emptyList());
        this.onProxyListener = listener;

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder) holder).bind(list.get(position));
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProxyBinding binding = ItemProxyBinding.bind(LayoutInflater.from(context).inflate(R.layout.item_proxy, parent, false));
        return new ViewHolder(binding);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemProxyBinding binding;

        public ViewHolder(ItemProxyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(@NonNull Proxy proxy) {

            binding.ivIcon.setImageResource(proxy.icon);
            binding.tvTitle.setText(proxy.title);
            binding.cbSelected.setChecked(proxy.selected);

            binding.container.setOnClickListener(v -> {
                setChecked(proxy);
                if (onProxyListener != null) {
                    onProxyListener.onClickProxy(proxy);
                }
            });

        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private void setChecked(Proxy proxy) {
        for (Proxy _proxy : list) {
            _proxy.selected = Objects.equals(_proxy, proxy);
        }
        notifyDataSetChanged();
    }

}
