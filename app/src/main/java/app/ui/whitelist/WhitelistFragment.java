package app.ui.whitelist;

import static app.internal.Settings.PREF_EXCLUDE_SYSTEM_APPS_DATA;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import app.R;
import app.application.ApplicationInfo;
import app.application.ApplicationRepository;
import app.databinding.FragmentWhitelistBinding;
import app.dependencies.ApplicationDependencies;
import app.internal.Preferences;
import app.netfilter.FilterVpnService;
import app.security.Policy;
import app.security.Whitelist;
import app.ui.BaseFragment;
import app.util.Toolbox;

public class WhitelistFragment extends BaseFragment<FragmentWhitelistBinding> implements WhitelistHandler {

    private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = (prefs, key) -> {
        if (key.equals(PREF_EXCLUDE_SYSTEM_APPS_DATA)) {
            Policy.reloadPrefs();
            if (Preferences.getBoolean(PREF_EXCLUDE_SYSTEM_APPS_DATA, false)) {
                FilterVpnService.notifyDropConnections(requireContext()); // exclude system apps from vpn filtering
            }
        }
    };

    private ApplicationRepository applicationRepository;
    private WhitelistViewModel whitelistViewModel;
    private WhitelistAdapter whitelistAdapter;

    private SearchView.SearchAutoComplete searchAutoComplete;
    private ImageView searchButton;
    private ImageView searchCloseButton;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applicationRepository = ApplicationDependencies.getApplicationRepository();
        whitelistViewModel = new ViewModelProvider(requireActivity()).get(WhitelistViewModel.class);
        whitelistAdapter = new WhitelistAdapter(requireContext());
    }

    @Override
    protected void initView() {
        getToolbar().setTitle(R.string.menu_whitelist);

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

        binding.rvWhitelist.setEmptyView(binding.ivWhitelistEmpty);

        applicationRepository.getList().observe(getViewLifecycleOwner(), whitelistViewModel::setList);

        whitelistAdapter.setOnAppClickListener(appInfo -> {
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

        searchButton.setClickable(false); // Disable clicks on the search icon.

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
                whitelistAdapter.getFilter().filter(text);
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

        Whitelist.save(); // save rules
        ApplicationDependencies.getVpnHelper().restartVpnIfRunning(); // restart vpn if running

        Preferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    private void showOptionsMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), view);
        Menu menu = popupMenu.getMenu();
        popupMenu.getMenuInflater().inflate(R.menu.whitelist_menu, menu);

        if (Preferences.getBoolean(PREF_EXCLUDE_SYSTEM_APPS_DATA, false)) {
            menu.findItem(R.id.menu_whitelist_system_apps_exclude_enabled).setTitle(R.string.menu_whitelist_system_apps_exclude_off);
        } else {
            menu.findItem(R.id.menu_whitelist_system_apps_exclude_enabled).setTitle(R.string.menu_whitelist_system_apps_exclude_on);
        }

        // Handling menu item selection.
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_whitelist_system_apps_exclude_enabled) {
                boolean _enabled = !Preferences.getBoolean(PREF_EXCLUDE_SYSTEM_APPS_DATA, false);
                if (_enabled) {
                    item.setTitle(getString(R.string.menu_whitelist_system_apps_exclude_off));
                } else {
                    item.setTitle(getString(R.string.menu_whitelist_system_apps_exclude_on));
                }
                Preferences.putBoolean(PREF_EXCLUDE_SYSTEM_APPS_DATA, _enabled);
            }
            return true;
        });

        popupMenu.show();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @BindingAdapter("whitelist")
    public static void setWhitelist(@NonNull RecyclerView recyclerView, List<ApplicationInfo> list) {
        WhitelistAdapter adapter = (WhitelistAdapter) recyclerView.getAdapter();
        if (adapter != null && list != null) {
            adapter.setData(list);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected FragmentWhitelistBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        FragmentWhitelistBinding binding = FragmentWhitelistBinding.inflate(inflater, container, false);
        binding.setViewModel(whitelistViewModel);
        binding.setHandler(this);
        binding.rvWhitelist.setAdapter(whitelistAdapter);
        binding.setLifecycleOwner(requireActivity()); // call after setAdapter
        return binding;
    }

}
