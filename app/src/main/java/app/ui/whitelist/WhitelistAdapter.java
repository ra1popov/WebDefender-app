package app.ui.whitelist;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.Collections;

import app.R;
import app.adapter.BaseRecyclerFilterableAdapter;
import app.application.ApplicationInfo;
import app.databinding.ItemWhitelistBinding;
import app.security.Whitelist;
import app.util.Toolbox;


public class WhitelistAdapter extends BaseRecyclerFilterableAdapter<ApplicationInfo, RecyclerView.ViewHolder> {

    public interface OnAppClickListener {
        void onClick(ApplicationInfo applicationInfo);
    }

    private OnAppClickListener onAppClickListener;

    public void setOnAppClickListener(OnAppClickListener onAppClickListener) {
        this.onAppClickListener = onAppClickListener;
    }

    public WhitelistAdapter(Context context) {
        super(context, Collections.emptyList());
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder) holder).bind(list.get(position), position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWhitelistBinding binding = ItemWhitelistBinding.bind(LayoutInflater.from(context).inflate(R.layout.item_whitelist, parent, false));
        return new ViewHolder(binding);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemWhitelistBinding binding;

        public ViewHolder(ItemWhitelistBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ApplicationInfo applicationInfo, int position) {
            try {
                Glide.with(context)
                        .load(Toolbox.getIconApplication(context, applicationInfo.pkgName))
                        .apply(new RequestOptions().override(100, 100))
                        .into(binding.ivIcon);
            } catch (PackageManager.NameNotFoundException ignored) {
            }

            binding.cbAllow.setOnCheckedChangeListener(null);
            binding.cbAllow.setChecked(Whitelist.isAllowed(applicationInfo.pkgName));
            binding.cbAllow.setOnCheckedChangeListener((compoundButton, checked) -> {
                Whitelist.setAllowed(applicationInfo.pkgName, checked);
                notifyItemChanged(position);
            });

            binding.tvTitle.setText(applicationInfo.appName);

            binding.container.setOnClickListener(v -> {
                if (onAppClickListener != null) {
                    onAppClickListener.onClick(applicationInfo);
                }
            });
        }

    }

}
