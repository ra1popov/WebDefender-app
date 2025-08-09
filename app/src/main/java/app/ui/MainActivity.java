package app.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import app.R;
import app.databinding.ActivityMainBinding;
import app.dependencies.ApplicationDependencies;
import app.dialog.DialogAdShow;
import app.dialog.DialogAskReview;
import app.internal.Preferences;
import app.internal.Settings;
import app.util.Toolbox;
import app.widget.ToolbarView;

public class MainActivity extends BaseActivity<ActivityMainBinding> {

    private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.equals(Settings.PREF_ACTIVE)) {
                setBackground();
            }
        }
    };

    private AppBarConfiguration appBarConfiguration;
    private DialogAskReview dialogReview;
    private DialogAdShow dialogAd;

    @Override
    protected ActivityMainBinding getViewBinding() {
        return ActivityMainBinding.inflate(LayoutInflater.from(this));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void initView() {
        setSupportActionBar(binding.toolbar);
        appBarConfiguration = new AppBarConfiguration.Builder().build();
        NavigationUI.setupActionBarWithNavController(this, getNavController(), appBarConfiguration);
        hideActionBar();

        dialogReview = new DialogAskReview.ExtendBuilder()
                .onSetPositiveButton(getString(R.string.DialogAskReview_ok), (baseDialog, datas) -> {
                    ApplicationDependencies.getPreferences().updateLastTimeReview();
                    Toolbox.reviewApp(this);
                    finish();
                })
                .onSetNegativeButton(getString(R.string.DialogAskReview_cancel), (baseDialog) -> {
                    ApplicationDependencies.getPreferences().updateLastTimeReview();
                    finish();
                })
                .build();

        dialogAd = new DialogAdShow.ExtendBuilder()
                .setCanOntouchOutside(false)
                .build();
    }

    @Override
    protected void initControl() {
        getNavController().addOnDestinationChangedListener((controller, destination, arguments) -> {
            hideActionBar(); // Hide the system navigation buttons in the ActionBar because we use custom buttons.
            if (destination.getId() == R.id.nav_main) {
                getToolbar().setHome(true);
                getToolbar().setOnLeftClickListener(v -> {
                });
            } else {
                getToolbar().setHome(false);
                getToolbar().setOnLeftClickListener(v -> onBackPressed());
            }
        });

        ApplicationDependencies.getAdRepository().getAd().observe(this, ad -> {
            if (ad.size() > 0) {
                dialogAd.show(getSupportFragmentManager(), DialogAdShow.class.getName(), ad.get(0));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        setBackground();
        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Preferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    // The method hides the system navigation buttons in the ActionBar because we use custom buttons.
    private void hideActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0.0f);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    @NonNull
    @Override
    public ToolbarView getToolbar() {
        return binding.toolbar;
    }

    @NonNull
    private NavController getNavController() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        assert navHostFragment != null;
        return navHostFragment.getNavController();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(getNavController(), appBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return NavigationUI.onNavDestinationSelected(item, getNavController()) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (getToolbar().isHome() && ApplicationDependencies.getPreferences().isNeedShowReviewDialog()) {
            dialogReview.show(getSupportFragmentManager(), DialogAskReview.class.getName());
            return;
        }
        super.onBackPressed();
    }

    private void setBackground() {
        if (Preferences.isActive()) {
            binding.llMainActive.setAlpha(0.0f);
            binding.llMainActive.setVisibility(View.VISIBLE);
            binding.llMainActive.animate()
                    .alpha(1.0f)
                    .setDuration(1000)
                    .start();
        } else {
            binding.llMainActive.animate()
                    .alpha(0.0f)
                    .setDuration(1000)
                    .withEndAction(() -> binding.llMainActive.setVisibility(View.INVISIBLE))
                    .start();
        }
    }

}