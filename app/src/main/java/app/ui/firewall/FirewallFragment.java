package app.ui.firewall;

import static app.internal.Settings.PREF_BLOCK_APPS_DATA;
import static app.internal.Settings.PREF_BLOCK_SYSTEM_APPS_DATA;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Objects;

import app.R;
import app.application.ApplicationInfo;
import app.application.ApplicationRepository;
import app.databinding.FragmentFirewallBinding;
import app.dependencies.ApplicationDependencies;
import app.internal.Preferences;
import app.netfilter.FilterVpnService;
import app.security.Firewall;
import app.security.Policy;
import app.ui.BaseFragment;
import app.util.Toolbox;

public class FirewallFragment extends BaseFragment<FragmentFirewallBinding> implements FirewallHandler {

    private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = (prefs, key) -> {
        if (key.equals(PREF_BLOCK_APPS_DATA)) {
            Policy.reloadPrefs();
            if (Preferences.getBoolean(PREF_BLOCK_APPS_DATA, false)) {
                FilterVpnService.notifyDropConnections(requireContext()); // block apps data except browsers
            }
        } else if (key.equals(PREF_BLOCK_SYSTEM_APPS_DATA)) {
            Policy.reloadPrefs();
            if (Preferences.getBoolean(PREF_BLOCK_SYSTEM_APPS_DATA, false)) {
                FilterVpnService.notifyDropConnections(requireContext()); // block system apps data except user apps
            }
        }
    };

    private ApplicationRepository applicationRepository;
    private FirewallViewModel firewallViewModel;
    private FirewallAdapter firewallAdapter;

    private SearchView.SearchAutoComplete searchAutoComplete;
    private ImageView searchButton;
    private ImageView searchCloseButton;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applicationRepository = ApplicationDependencies.getApplicationRepository();
        firewallViewModel = new ViewModelProvider(requireActivity()).get(FirewallViewModel.class);
        firewallAdapter = new FirewallAdapter(requireContext());
    }

    @Override
    protected void initView() {
        getToolbar().setTitle(R.string.menu_firewall);

        firewallViewModel.setInternetForBrowsersOnly(Preferences.getBoolean(PREF_BLOCK_APPS_DATA, false));

        Context context = requireContext();

        // Change the text color of the SearchView widget.
        searchAutoComplete = binding.svSearch.findViewById(androidx.appcompat.R.id.search_src_text);
        searchAutoComplete.setHintTextColor(context.getColor(R.color.white));
        searchAutoComplete.setTextColor(context.getColor(R.color.white));
        searchAutoComplete.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimensionPixelSize(com.intuit.sdp.R.dimen._12sdp));

        // Change the cursor.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            searchAutoComplete.setTextCursorDrawable(R.drawable.bg_cursor);
        } else {
            Toolbox.setCursorDrawable(context, searchAutoComplete, R.color.white);
        }

        // Change the search icon color of the SearchView widget.
        searchButton = binding.svSearch.findViewById(androidx.appcompat.R.id.search_button);
        searchButton.setColorFilter(context.getColor(R.color.white), PorterDuff.Mode.SRC_IN);

        // Change the clear icon color of the SearchView widget.
        searchCloseButton = binding.svSearch.findViewById(androidx.appcompat.R.id.search_close_btn);
        searchCloseButton.setColorFilter(context.getColor(R.color.white), PorterDuff.Mode.SRC_IN);
    }

    @Override
    protected void initControl() {
        getToolbar().setOnRightClickListener(this::showOptionsMenu);

        binding.rvFirewall.setEmptyView(binding.ivFirewallEmpty);

        applicationRepository.getList().observe(getViewLifecycleOwner(), firewallViewModel::setList);

        firewallAdapter.setOnAppClickListener(appInfo -> {
            String info;
            if (Objects.equals(appInfo.appName, appInfo.pkgName)) {
                info = appInfo.pkgName;
            } else {
                if (TextUtils.isEmpty(appInfo.appName)) {
                    info = appInfo.pkgName;
                } else {
                    info = appInfo.appName + " (" + appInfo.pkgName + ")";
                }
            }
            toast(info);
        });

        searchButton.setClickable(false); // Disable clicks on the search icon

        // Open the search when clicking anywhere on the SearchView widget, not just on its search icon.
        binding.svSearch.setOnClickListener(v -> {
            binding.tvTooltip.setVisibility(View.GONE);
            searchButton.performClick();
        });

        // Show a hint when the search is completed.
        binding.svSearch.setOnCloseListener(() -> {
            binding.tvTooltip.setVisibility(View.VISIBLE);
            return false;
        });

        // Bind the contact list filtering to the adapter.
        binding.svSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String text) {
                firewallAdapter.getFilter().filter(text);
                return true;
            }
        });

    }


    @Override
    public void onResume() {
        super.onResume();

        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        Firewall.save(); // save rules
        ApplicationDependencies.getVpnHelper().restartVpnIfRunning(); // restart vpn if running

        Preferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    private void showOptionsMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), view);
        Menu menu = popupMenu.getMenu();
        popupMenu.getMenuInflater().inflate(R.menu.firewall_menu, menu);

        if (Preferences.getBoolean(PREF_BLOCK_APPS_DATA, false)) {
            menu.findItem(R.id.menu_firewall_browsers_only_enabled).setTitle(R.string.menu_firewall_browsers_only_off);
        } else {
            menu.findItem(R.id.menu_firewall_browsers_only_enabled).setTitle(R.string.menu_firewall_browsers_only_on);
        }

        if (Preferences.getBoolean(PREF_BLOCK_SYSTEM_APPS_DATA, false)) {
            menu.findItem(R.id.menu_firewall_system_apps_blocked_enabled).setTitle(R.string.menu_firewall_system_apps_blocked_off);
        } else {
            menu.findItem(R.id.menu_firewall_system_apps_blocked_enabled).setTitle(R.string.menu_firewall_system_apps_blocked_on);
        }

        // Handling menu item selection.
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_firewall_browsers_only_enabled) {
                boolean _enabled = !Preferences.getBoolean(PREF_BLOCK_APPS_DATA, false);
                if (_enabled) {
                    item.setTitle(getString(R.string.menu_firewall_browsers_only_off));
                } else {
                    item.setTitle(getString(R.string.menu_firewall_browsers_only_on));
                }
                firewallViewModel.setInternetForBrowsersOnly(_enabled);
                Preferences.putBoolean(PREF_BLOCK_APPS_DATA, _enabled);
            } else if (item.getItemId() == R.id.menu_firewall_system_apps_blocked_enabled) {
                boolean _enabled = !Preferences.getBoolean(PREF_BLOCK_SYSTEM_APPS_DATA, false);
                if (_enabled) {
                    item.setTitle(getString(R.string.menu_firewall_system_apps_blocked_off));
                } else {
                    item.setTitle(getString(R.string.menu_firewall_system_apps_blocked_on));
                }
                Preferences.putBoolean(PREF_BLOCK_SYSTEM_APPS_DATA, _enabled);
            }
            return true;
        });

        popupMenu.show();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @BindingAdapter("firewall")
    public static void setFirewall(@NonNull RecyclerView recyclerView, List<ApplicationInfo> list) {
        FirewallAdapter adapter = (FirewallAdapter) recyclerView.getAdapter();
        if (adapter != null && list != null) {
            adapter.setData(list);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected FragmentFirewallBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        FragmentFirewallBinding binding = FragmentFirewallBinding.inflate(inflater, container, false);
        binding.setViewModel(firewallViewModel);
        binding.setHandler(this);
        binding.rvFirewall.setAdapter(firewallAdapter);
        binding.setLifecycleOwner(requireActivity()); // call after setAdapter
        return binding;
    }

}
