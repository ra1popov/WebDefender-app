package app.ui.statlog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.paging.CombinedLoadStates;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import app.R;
import app.adapter.BasePagingRecyclerAdapter;
import app.databinding.ItemStatlogBinding;
import app.ui.Toasts;

public class StatlogAdapter extends BasePagingRecyclerAdapter<Statlog, RecyclerView.ViewHolder> {

    public interface DataUpdateListener {
        void onDataUpdated(CombinedLoadStates combinedLoadStates);
    }

    private static final DiffUtil.ItemCallback<Statlog> diffCallback = new DiffUtil.ItemCallback<Statlog>() {
        @Override
        public boolean areItemsTheSame(@NonNull Statlog oldItem, @NonNull Statlog newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Statlog oldItem, @NonNull Statlog newItem) {
            return oldItem.equals(newItem);
        }
    };

    private final HashMap<String, StatlogIcon> icons = new HashMap<>();

    public StatlogAdapter(Context context, @NonNull DataUpdateListener listener) {
        super(context, diffCallback);
        addLoadStateListener(combinedLoadStates -> {
            listener.onDataUpdated(combinedLoadStates);
            return null;
        });
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStatlogBinding binding = ItemStatlogBinding.bind(LayoutInflater.from(context).inflate(R.layout.item_statlog, parent, false));
        return new ViewHolder(binding, icons);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Statlog statlog = getItem(position);
        if (statlog != null) {
            ((ViewHolder) holder).bind(statlog);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setIcons(HashMap<String, StatlogIcon> icons) {
        this.icons.clear();
        this.icons.putAll(icons);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        private final DateFormat TIME_FORMAT = DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.getDefault());
        private final ItemStatlogBinding binding;
        private final HashMap<String, StatlogIcon> icons;

        public ViewHolder(ItemStatlogBinding binding, HashMap<String, StatlogIcon> icons) {
            super(binding.getRoot());
            this.binding = binding;
            this.icons = icons;
        }

        public void bind(@NonNull Statlog statlog) {

            StatlogIcon _statlogIcon = icons.get(statlog.pkgName);
            if (_statlogIcon != null) {
                binding.ivIcon.setImageDrawable(_statlogIcon.icon);
            } else {
                binding.ivIcon.setImageResource(R.mipmap.ic_launcher);
            }

            binding.tvDomain.setText(statlog.domain);

            if (_statlogIcon == null) {
                binding.ivIcon.setOnClickListener(null);
            } else {
                String info;
                if (Objects.equals(_statlogIcon.appName, _statlogIcon.pkgName)) {
                    info = _statlogIcon.pkgName;
                } else {
                    if (TextUtils.isEmpty(_statlogIcon.appName)) {
                        info = _statlogIcon.pkgName;
                    } else {
                        info = _statlogIcon.appName + " (" + _statlogIcon.pkgName + ")";
                    }
                }
                binding.ivIcon.setOnClickListener(view -> Toasts.showToast(info));
            }

            binding.llInfo.setOnClickListener(view -> Toasts.showToast(statlog.domain));

            switch (statlog.type) {
                case Statlog.TYPE_AD:
                    binding.tvType.setText(R.string.statlog_type_ad);
                    break;

                case Statlog.TYPE_APK:
                    binding.tvType.setText(R.string.statlog_type_apk);
                    break;

                case Statlog.TYPE_MALWARE:
                    binding.tvType.setText(R.string.statlog_type_malware);
                    break;

                case Statlog.TYPE_PAID:
                    binding.tvType.setText(R.string.statlog_type_paid);
                    break;

                default:
                    binding.tvType.setText("");
                    break;
            }

            binding.tvDate.setText(DATE_FORMAT.format(statlog.time));
            binding.tvTime.setText(TIME_FORMAT.format(statlog.time));

            binding.container.setOnClickListener(v -> {

            });
        }

    }

}
