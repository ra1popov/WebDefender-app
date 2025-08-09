package app.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;
import androidx.navigation.Navigation;
import androidx.viewbinding.ViewBinding;

import app.R;
import app.util.Callback;
import app.util.Toolbox;
import app.widget.ToolbarView;

public abstract class BaseActivity<B extends ViewBinding> extends AppCompatActivity {

    private final ActivityResultLauncher<String> askPermissionResult = registerForActivityResult(new ActivityResultContracts.RequestPermission(), this::callBooleanListener);

    private final ActivityResultLauncher<Intent> askIgnoreBatteryOptimizationsPermissionResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        Boolean _result = Boolean.FALSE;
        if (hasIgnoreBatteryOptimizationsPermission()) {
            _result = Boolean.TRUE;
        }
        callBooleanListener(_result);
    });

    protected Callback<Boolean> callbackBoolean;
    protected B binding;

    protected void callBooleanListener(Boolean result) {
        try {
            this.callbackBoolean.setResult(result);
            this.callbackBoolean.call();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0, 0);

        binding = getViewBinding();
        setContentView(binding.getRoot());

        setScreenMode();

        initView();
        initControl();
    }

    @Override
    protected void onPause() {
        super.onPause();

        overridePendingTransition(0, 0);
    }

    protected abstract void initView();

    protected abstract B getViewBinding();

    protected abstract void initControl();

    @NonNull
    public abstract ToolbarView getToolbar();

    private void setScreenMode() {
        Toolbox.setStatusBarAndHomeTransparent(this, true);
    }

    public void toast(String content) {
        if (!TextUtils.isEmpty(content)) {
            Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
        }
    }

    public void navigate(int id) {
        navigate(id, null);
    }

    public void navigate(int id, Bundle bundle) {
        try {
            Navigation.findNavController(this, R.id.nav_host_fragment).navigate(
                    id,
                    bundle,
                    null,
                    null);
        } catch (Exception ignored) {
            // try-catch for fix Fatal Exception: java.lang.IllegalArgumentException
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    /////////////////////////////////////////////////////////////////////////////////////////

    public void askQueryAllPackagesPermission(Callback<Boolean> callback) {
        this.callbackBoolean = callback;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasQueryAllPackagesPermission()) {
            try {
                askPermissionResult.launch(Manifest.permission.QUERY_ALL_PACKAGES);
            } catch (Exception ignored) {
            }
        } else {
            callBooleanListener(true);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasQueryAllPackagesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return PermissionChecker.checkSelfPermission(this, Manifest.permission.QUERY_ALL_PACKAGES) == PermissionChecker.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    @SuppressLint("BatteryLife")
    public void askIgnoreBatteryOptimizationsPermission(Callback<Boolean> callback) {
        this.callbackBoolean = callback;
        if (!hasIgnoreBatteryOptimizationsPermission()) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                askIgnoreBatteryOptimizationsPermissionResult.launch(intent);
            } catch (Exception ignored) {
            }
        } else {
            callBooleanListener(true);
        }
    }

    public boolean hasIgnoreBatteryOptimizationsPermission() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return powerManager.isIgnoringBatteryOptimizations(getPackageName());
    }

}
