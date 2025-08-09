package app.ui.main;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;
import androidx.lifecycle.ViewModelProvider;

import app.App;
import app.R;
import app.databinding.FragmentMainBinding;
import app.dependencies.ApplicationDependencies;
import app.internal.Preferences;
import app.internal.Settings;
import app.preferences.model.Stat;
import app.ui.BaseActivity;
import app.ui.BaseFragment;
import app.ui.Toasts;
import app.ui.main.widget.PowerView;
import app.ui.main.widget.StatView;
import app.updater.UpdaterService;
import app.util.Callback;
import app.util.SafeOnMenuItemClickListener;
import app.util.Toolbox;
import app.workers.TimerWorker;

public class MainFragment extends BaseFragment<FragmentMainBinding> implements MainHandler {

    private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.equals(Settings.PREF_ACTIVE)) {
                boolean isActive = Preferences.isActive();
                if (Boolean.TRUE.equals(mainViewModel.power.getValue()) != isActive) {
                    mainViewModel.setPower(Preferences.isActive());
                }
            }
        }
    };

    private final SafeOnMenuItemClickListener safeOnMenuItemClickListener = new SafeOnMenuItemClickListener();
    private MainViewModel mainViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

    }

    @Override
    protected void initView() {
        getToolbar().setTitle("");

        mainViewModel.power.setValue(Preferences.isActive());
    }

    @Override
    protected void initControl() {
        getToolbar().setOnRightClickListener(this::showOptionsMenu);

        mainViewModel.power.observe(getViewLifecycleOwner(), power -> {

            if (!power && !Preferences.isActive()) {
                return;
            }

            if (power) {
                startVPNInternal();
            } else {
                App.disable();
                TimerWorker.updateSettingsStartTimer(true);
            }

        });

        ApplicationDependencies.getMainRepository().getStat().observe(getViewLifecycleOwner(), stat -> mainViewModel.setStat(stat));

    }

    @Override
    public void onResume() {
        super.onResume();

        mainViewModel.updateState();

        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        Preferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    private void startVPNInternal() {
        Context context = App.getContext();

        Intent intent = null;
        try {
            intent = VpnService.prepare(context);
        } catch (Exception ignored) {
            // Check the behavior here — if the app is installed as an update over an existing one, will it still reach this point?
        }

        if (intent == null) {
            // already have rights, emulate return from vpndialogs.ConfirmDialog
            App.cleanCaches(true, false, false, false);
            App.startVpnService();

            TimerWorker.updateSettingsStartTimer(false);
            UpdaterService.startUpdate(UpdaterService.START_FORCE_DELAYED); // switch on

            ApplicationDependencies.getGuideHelper().showGuideIfNeeded((BaseActivity<?>) requireActivity());
            return; // WAIT dialog
        }

        // no rights, start activity
        try {
            askIntent(intent, new Callback<Boolean>() {
                @Override
                public Void call() {
                    if (result) {
                        App.cleanCaches(true, false, false, false);
                        App.startVpnService();

                        TimerWorker.updateSettingsStartTimer(false);
                        UpdaterService.startUpdate(UpdaterService.START_FORCE_DELAYED); // switch on

                        ApplicationDependencies.getGuideHelper().showGuideIfNeeded((BaseActivity<?>) requireActivity());
                    } else {
                        mainViewModel.setPower(false);
                    }
                    return null;
                }
            });
            return;
        } catch (ActivityNotFoundException ignored) {
        }

        mainViewModel.setPower(false);

        Toasts.showFirmwareVpnUnavailable();
    }

    @SuppressLint("NonConstantResourceId")
    private void showOptionsMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), view);
        Menu menu = popupMenu.getMenu();
        popupMenu.getMenuInflater().inflate(R.menu.main_menu, menu);

//        if (Preferences.get(Settings.PREF_APP_ADBLOCK) && ApplicationDependencies.getPreferences().isPro()) {
            menu.findItem(R.id.menu_whitelist).setVisible(true);
            menu.findItem(R.id.menu_statlog).setVisible(true);
//        } else {
//            menu.findItem(R.id.menu_whitelist).setVisible(false);
//            menu.findItem(R.id.menu_statlog).setVisible(false);
//        }

//        if (ApplicationDependencies.getPreferences().isPro()) {
            menu.findItem(R.id.menu_buy).setVisible(false);
//        } else {
//            menu.findItem(R.id.menu_buy).setVisible(true);
//        }

        if (Preferences.get(Settings.PREF_APP_PROXY)) {
            menu.findItem(R.id.menu_proxy).setVisible(true);
        } else {
            menu.findItem(R.id.menu_proxy).setVisible(false);
        }

        // Handling menu item selection
        popupMenu.setOnMenuItemClickListener(item -> {
            boolean result = safeOnMenuItemClickListener.onMenuItemClick(item);

            if (result) {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_proxy) {
                    navigate(R.id.action_nav_home_to_nav_proxy);
                } else if (itemId == R.id.menu_firewall) {
                    navigate(R.id.action_nav_home_to_nav_firewall);
                } else if (itemId == R.id.menu_whitelist) {
                    navigate(R.id.action_nav_home_to_nav_whitelist);
                } else if (itemId == R.id.menu_statlog) {
                    navigate(R.id.action_nav_home_to_nav_statlog);
                } else if (itemId == R.id.menu_help) {
                    navigate(R.id.action_nav_home_to_nav_help);
                } else if (itemId == R.id.menu_privacy) {
                    Toolbox.openURL(requireContext(), getString(R.string.privacy_uri));
                }
            }

            return result;
        });

        popupMenu.show();
    }

    /// //////////////////////////////////////////////////////////////////////////////////////////////////

    @BindingAdapter("power")
    public static void setPower(@NonNull PowerView powerView, boolean power) {
        powerView.setPower(power);
    }

    @InverseBindingAdapter(attribute = "power")
    public static Boolean isPower(@NonNull PowerView powerView) {
        return powerView.isPower();
    }

    @BindingAdapter("powerAttrChanged")
    public static void setOnPowerListener(@NonNull PowerView powerView, final InverseBindingListener attrChange) {
        powerView.setOnPowerListener(on -> attrChange.onChange());
    }

    @BindingAdapter("statShowAd")
    public static void setStatShowAd(@NonNull StatView statView, boolean show) {
        statView.showAdBlock(show);
    }

    @BindingAdapter("statPower")
    public static void setStatPower(@NonNull StatView statView, boolean power) {
        statView.setPower(power);
    }

    @BindingAdapter("statData")
    public static void setStatData(@NonNull StatView statView, Stat stat) {
        if (stat != null) {
            statView.setData(stat);
        }
    }

    /// //////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected FragmentMainBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        FragmentMainBinding binding = FragmentMainBinding.inflate(inflater, container, false);
        binding.setViewModel(mainViewModel);
        binding.setHandler(this);
        binding.setLifecycleOwner(requireActivity()); // call after setAdapter
        return binding;
    }

}
