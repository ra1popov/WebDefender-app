package app.ui.firewall;

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
import app.databinding.ItemFirewallBinding;
import app.security.Firewall;
import app.util.Toolbox;

public class FirewallAdapter extends BaseRecyclerFilterableAdapter<ApplicationInfo, RecyclerView.ViewHolder> {

    public interface OnAppClickListener {
        void onClick(ApplicationInfo applicationInfo);
    }

    private OnAppClickListener onAppClickListener;

    public void setOnAppClickListener(OnAppClickListener onAppClickListener) {
        this.onAppClickListener = onAppClickListener;
    }

    public FirewallAdapter(Context context) {
        super(context, Collections.emptyList());
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder) holder).bind(list.get(position), position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFirewallBinding binding = ItemFirewallBinding.bind(LayoutInflater.from(context).inflate(R.layout.item_firewall, parent, false));
        return new ViewHolder(binding);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemFirewallBinding binding;

        public ViewHolder(ItemFirewallBinding binding) {
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

            binding.cbAllowMobile.setOnCheckedChangeListener(null);
            binding.cbAllowMobile.setChecked(Firewall.mobileAppIsAllowed(applicationInfo.pkgName));
            binding.cbAllowMobile.setOnCheckedChangeListener((compoundButton, checked) -> {
                Firewall.mobileAppState(applicationInfo.pkgName, checked);
                notifyItemChanged(position);
            });

            binding.cbAllowWifi.setOnCheckedChangeListener(null);
            binding.cbAllowWifi.setChecked(Firewall.wifiAppIsAllowed(applicationInfo.pkgName));
            binding.cbAllowWifi.setOnCheckedChangeListener((compoundButton, checked) -> {
                Firewall.wifiAppState(applicationInfo.pkgName, checked);
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
