package app.ui.statlog;

import static app.internal.Settings.PREF_STATLOG_ENABLED;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.CombinedLoadStates;
import androidx.paging.LoadState;
import androidx.paging.PagingData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import app.R;
import app.databinding.FragmentStatlogBinding;
import app.dependencies.ApplicationDependencies;
import app.internal.Preferences;
import app.internal.Settings;
import app.ui.BaseFragment;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowCollector;

public class StatlogFragment extends BaseFragment<FragmentStatlogBinding> implements StatlogHandler, StatlogAdapter.DataUpdateListener {

    private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = (prefs, key) -> {
        if (key.equals(Settings.PREF_STATLOG_ENABLED)) {
            ApplicationDependencies.getStatlogHelper().setEnabled(Preferences.getBoolean(Settings.PREF_STATLOG_ENABLED, false));
        }
    };

    private StatlogRepository statlogRepository;
    private StatlogViewModel statlogViewModel;
    private StatlogAdapter statlogAdapter;
    private boolean isProgrammaticScroll = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        statlogRepository = ApplicationDependencies.getStatlogRepository();

        statlogViewModel = new ViewModelProvider(requireActivity()).get(StatlogViewModel.class);
        statlogAdapter = new StatlogAdapter(requireContext(), this);
    }

    @Override
    protected void initView() {
        getToolbar().setTitle(R.string.menu_statlog);
    }

    @Override
    protected void initControl() {
        getToolbar().setOnRightClickListener(this::showOptionsMenu);

        binding.rvStatlog.setHasFixedSize(true);

        statlogRepository.getRecords().observe(getViewLifecycleOwner(), new Observer<Flow<PagingData<Statlog>>>() {
            @Override
            public void onChanged(Flow<PagingData<Statlog>> pagingDataFlow) {
                pagingDataFlow.collect(new FlowCollector<PagingData<Statlog>>() {
                    @NonNull
                    @Override
                    public Object emit(PagingData<Statlog> data, @NonNull Continuation<? super Unit> continuation) {
                        if (statlogAdapter.getItemCount() == 0) {
                            statlogAdapter.submitData(getLifecycle(), data);
                        } else {
                            if (isUserAtTheBeginningOfTheList()) {
                                statlogAdapter.submitData(getLifecycle(), data);
                            }
                        }
                        return Unit.INSTANCE;
                    }
                }, new Continuation<Unit>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object o) {

                    }
                });
            }
        });

        binding.rvStatlog.addOnScrollListener(new RecyclerView.OnScrollListener() {

            private boolean isScrollingUp = false;

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // Updating data when the user scrolls to the top of the list.
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!isProgrammaticScroll && isScrollingUp && isUserAtTheBeginningOfTheList()) {
                        ApplicationDependencies.getStatlogRepository().requestRecords();
                    }
                    isProgrammaticScroll = false;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                isScrollingUp = dy < 0; // Checking the scroll direction.
            }

        });

        statlogRepository.getIcons().observe(getViewLifecycleOwner(), icons -> {
            statlogAdapter.setIcons(icons);
        });

    }

    private boolean isUserAtTheBeginningOfTheList() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.rvStatlog.getLayoutManager();
        return layoutManager != null && layoutManager.findFirstVisibleItemPosition() == 0;
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
        popupMenu.getMenuInflater().inflate(R.menu.statlog_menu, menu);

        if (Preferences.getBoolean(PREF_STATLOG_ENABLED, false)) {
            menu.findItem(R.id.menu_statlog_enabled).setTitle(R.string.menu_statlog_log_off);
        } else {
            menu.findItem(R.id.menu_statlog_enabled).setTitle(R.string.menu_statlog_log_on);
        }

        // Menu item selection handling
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_statlog_clear) {
                ApplicationDependencies.getDatabaseHelper().clearStatlogTable();
            } else if (item.getItemId() == R.id.menu_statlog_enabled) {
                boolean enabled = !Preferences.getBoolean(PREF_STATLOG_ENABLED, false);
                if (enabled) {
                    item.setTitle(getString(R.string.menu_statlog_log_off));
                } else {
                    item.setTitle(getString(R.string.menu_statlog_log_on));
                }
                Preferences.putBoolean(PREF_STATLOG_ENABLED, enabled);
            }
            return true;
        });

        popupMenu.show();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected FragmentStatlogBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        FragmentStatlogBinding binding = FragmentStatlogBinding.inflate(inflater, container, false);
        binding.setViewModel(statlogViewModel);
        binding.setHandler(this);
        binding.rvStatlog.setAdapter(statlogAdapter);
        binding.setLifecycleOwner(requireActivity()); // call after setAdapter
        return binding;
    }

    @Override
    public void onDataUpdated(CombinedLoadStates combinedLoadStates) {
        if (statlogAdapter.getItemCount() == 0 && combinedLoadStates.getRefresh() instanceof LoadState.NotLoading) {
            binding.tvStatlogEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.tvStatlogEmpty.setVisibility(View.GONE);
        }

        if (statlogAdapter.getItemCount() > 0 && isUserAtTheBeginningOfTheList()) {
            isProgrammaticScroll = true; // Marking that scrolling to the top of the list is performed programmatically.
            binding.rvStatlog.smoothScrollToPosition(0);
        }
    }

}
