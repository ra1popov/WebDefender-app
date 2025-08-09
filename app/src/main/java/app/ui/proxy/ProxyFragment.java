package app.ui.proxy;

import static app.internal.Settings.PREF_ANONYMIZE;
import static app.internal.Settings.PREF_ANONYMIZE_ONLY_BRW;
import static app.internal.Settings.PREF_USE_COMPRESSION;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.R;
import app.databinding.FragmentProxyBinding;
import app.dependencies.ApplicationDependencies;
import app.internal.Preferences;
import app.internal.ProxyBase;
import app.internal.Settings;
import app.netfilter.FilterVpnService;
import app.security.Policy;
import app.ui.BaseFragment;

public class ProxyFragment extends BaseFragment<FragmentProxyBinding> implements ProxyHandler {

    private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = (prefs, key) -> {
        if (key.equals(Settings.PREF_PROXY_COUNTRY) || key.equals(Settings.PREF_USE_COMPRESSION) || key.equals(Settings.PREF_ANONYMIZE)) {

            ProxyBase.notifyServersUp();

            // TODO XXX drop connections here, but update country in Policy.reloadPrefs
            // TODO XXX drop all connections even if use proxy only in browsers

            if (key.equals(Settings.PREF_PROXY_COUNTRY)) {
                FilterVpnService.notifyProxyChanged(requireContext()); // change proxy
            } else {
                FilterVpnService.notifyDropConnections(requireContext()); // enable proxy
            }

            if (key.equals(Settings.PREF_ANONYMIZE)) {
                setShow(Preferences.getBoolean(Settings.PREF_ANONYMIZE, false));
            }

            Policy.reloadPrefs();
        }
    };

    public ProxyViewModel proxyViewModel;
    private ProxyAdapter proxyAdapter;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        proxyViewModel = new ViewModelProvider(requireActivity()).get(ProxyViewModel.class);
        proxyAdapter = new ProxyAdapter(requireContext(), proxy -> Preferences.putString(Settings.PREF_PROXY_COUNTRY, proxy.country));
    }

    private void setShow(boolean show) {
        proxyViewModel.setShow(show);
    }

    @Override
    protected void initView() {
        getToolbar().setTitle(R.string.menu_proxy);
        setShow(Preferences.getBoolean(Settings.PREF_ANONYMIZE, false));
    }

    @Override
    protected void initControl() {
        getToolbar().setOnRightClickListener(this::showOptionsMenu);

        ApplicationDependencies.getProxyRepository().getProxy().observe(getViewLifecycleOwner(), proxy -> proxyViewModel.setList(proxy));
    }


    @Override
    public void onResume() {
        super.onResume();

        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        Preferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    private void showOptionsMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), view);
        Menu menu = popupMenu.getMenu();
        popupMenu.getMenuInflater().inflate(R.menu.proxy_menu, menu);

        if (Preferences.getBoolean(PREF_ANONYMIZE, false)) {
            menu.findItem(R.id.menu_proxy_anonymity_enabled).setTitle(R.string.menu_proxy_anonymity_off);
        } else {
            menu.findItem(R.id.menu_proxy_anonymity_enabled).setTitle(R.string.menu_proxy_anonymity_on);
        }

        if (Preferences.getBoolean(PREF_USE_COMPRESSION, false)) {
            menu.findItem(R.id.menu_proxy_compress_enabled).setTitle(R.string.menu_proxy_compress_off);
        } else {
            menu.findItem(R.id.menu_proxy_compress_enabled).setTitle(R.string.menu_proxy_compress_on);
        }

        if (Preferences.getBoolean(PREF_ANONYMIZE_ONLY_BRW, Preferences.get(PREF_ANONYMIZE_ONLY_BRW))) {
            menu.findItem(R.id.menu_proxy_only_browsers_enabled).setTitle(R.string.menu_proxy_only_browsers_off);
        } else {
            menu.findItem(R.id.menu_proxy_only_browsers_enabled).setTitle(R.string.menu_proxy_only_browsers_on);
        }

        // Processing menu item selection
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_proxy_anonymity_enabled) {
                boolean enabled = !Preferences.getBoolean(PREF_ANONYMIZE, false);
                if (enabled) {
                    item.setTitle(getString(R.string.menu_proxy_anonymity_off));
                } else {
                    item.setTitle(getString(R.string.menu_proxy_anonymity_on));
                }
                Preferences.putBoolean(PREF_ANONYMIZE, enabled);
            } else if (item.getItemId() == R.id.menu_proxy_compress_enabled) {
                boolean enabled = !Preferences.getBoolean(PREF_USE_COMPRESSION, false);
                if (enabled) {
                    item.setTitle(getString(R.string.menu_proxy_compress_off));
                } else {
                    item.setTitle(getString(R.string.menu_proxy_compress_on));
                }
                Preferences.putBoolean(PREF_USE_COMPRESSION, enabled);
            } else if (item.getItemId() == R.id.menu_proxy_only_browsers_enabled) {
                boolean enabled = !Preferences.getBoolean(PREF_ANONYMIZE_ONLY_BRW, Preferences.get(PREF_ANONYMIZE_ONLY_BRW));
                if (enabled) {
                    item.setTitle(getString(R.string.menu_proxy_only_browsers_off));
                } else {
                    item.setTitle(getString(R.string.menu_proxy_only_browsers_on));
                }
                Preferences.putBoolean(PREF_ANONYMIZE_ONLY_BRW, enabled);
            }
            return true;
        });

        popupMenu.show();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @BindingAdapter("proxy")
    public static void setProxy(@NonNull RecyclerView recyclerView, List<Proxy> list) {
        ProxyAdapter adapter = (ProxyAdapter) recyclerView.getAdapter();
        if (adapter != null && list != null) {
            adapter.setData(list);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected FragmentProxyBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        FragmentProxyBinding binding = FragmentProxyBinding.inflate(inflater, container, false);
        binding.setViewModel(proxyViewModel);
        binding.setHandler(this);
        binding.rvProxy.setAdapter(proxyAdapter);
        binding.setLifecycleOwner(requireActivity()); // call after setAdapter
        return binding;
    }

}
