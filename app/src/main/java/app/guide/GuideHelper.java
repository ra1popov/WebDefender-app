package app.guide;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.Locale;

import app.R;
import app.dependencies.ApplicationDependencies;
import app.dialog.BaseDialog;
import app.dialog.DialogGuideInfo;
import app.dialog.DialogGuideMIUI;
import app.dialog.DialogGuideSetup;
import app.internal.Settings;
import app.preferences.Preferences;
import app.ui.BaseActivity;
import app.util.Callback;

public class GuideHelper {

    private final Context context;
    private final Preferences preferences;
    private final BootPermissionHelper boot;
    private BaseDialog<?, ?, ?> dialog;
    private BaseActivity<?> activity;

    public GuideHelper(@NonNull Context context) {
        this.context = context;
        this.preferences = ApplicationDependencies.getPreferences();
        this.boot = ApplicationDependencies.getBootPermissionHelper();
    }

    public void showGuideIfNeeded(@NonNull BaseActivity<?> activity) {
        this.activity = activity;

        if (!app.internal.Preferences.get(Settings.PREF_APP_ADBLOCK)) {
            return;
        }

        if (preferences.isNeedShowSetupDialog()) {

            if (activity.hasQueryAllPackagesPermission()
                    && activity.hasIgnoreBatteryOptimizationsPermission()
                    && !boot.isBootPermissionAvailable(context, true)
                    && !isMIUIDevice()) {
                return; // Do not show the settings dialog if permissions are granted, there are no available screens for configuration, and the device is not running MIUI.
            }

            showMainDialog();

        } else {

            // If the user has previously configured settings, we double-check whether optimization is disabled just in case (since the device might have revoked our permissions) and attempt to enable it. We also request permission to access the visibility of all apps.
            if (preferences.isMadeFirstSetup()) {
                activity.askIgnoreBatteryOptimizationsPermission(new Callback<Boolean>() {
                    @Override
                    public Void call() {
                        activity.askQueryAllPackagesPermission(new Callback<Boolean>() {
                            @Override
                            public Void call() {
                                return null;
                            }
                        });
                        return null;
                    }
                });
            }

        }
    }

    public void showGuide(@NonNull BaseActivity<?> activity) {
        this.activity = activity;

        if (!app.internal.Preferences.get(Settings.PREF_APP_ADBLOCK)) {
            return;
        }

        if (activity.hasQueryAllPackagesPermission()) {
            if (activity.hasIgnoreBatteryOptimizationsPermission()) {
                if (boot.isBootPermissionAvailable(context, true)) {
                    showBootPermissionDialog();
                } else if (isMIUIDevice()) {
                    showMIUIAppLockDialog();
                } else {
                    showFinishDialog();
                }
            } else {
                showIgnoreBatteryOptimizationsPermissionDialog();
            }
        } else {
            showQueryAllPackagesPermissionDialog();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void showMainDialog() {
        dialog = new DialogGuideSetup.ExtendBuilder()
                .setTitle(getString(R.string.DialogGuideMain_title))
                .setContent(preferences.isMadeFirstSetup() ? getString(R.string.DialogGuideMain_content_hide) : getString(R.string.DialogGuideMain_content_later))
                .onSetPositiveButton(getString(R.string.DialogGuideMain_ok), (baseDialog, datas) -> {
                    if (activity.hasQueryAllPackagesPermission()) {
                        if (activity.hasIgnoreBatteryOptimizationsPermission()) {
                            if (boot.isBootPermissionAvailable(context, true)) {
                                showBootPermissionDialog();
                            } else if (isMIUIDevice()) {
                                showMIUIAppLockDialog();
                            } else {
                                showFinishDialog();
                            }
                        } else {
                            showIgnoreBatteryOptimizationsPermissionDialog();
                        }
                    } else {
                        showQueryAllPackagesPermissionDialog();
                    }
                })
                .onSetNegativeButton(preferences.isMadeFirstSetup() ? getString(R.string.DialogGuideMain_hide) : getString(R.string.DialogGuideMain_later), (baseDialog) -> {
                    if (preferences.isMadeFirstSetup()) {
                        preferences.hideSetupDialog();
                    } else {
                        preferences.updateLastTimeSetup();
                    }
                    close();
                })
                .setCancelable(false)
                .setCanOntouchOutside(false)
                .build();

        dialog.show(activity.getSupportFragmentManager(), DialogGuideSetup.class.getName() + ".showMainDialog");
    }

    private void showQueryAllPackagesPermissionDialog() {
        close();

        dialog = new DialogGuideSetup.ExtendBuilder()
                .setTitle(getString(R.string.DialogGuideQueryAllPackagesPermission_title))
                .setContent(getString(R.string.DialogGuideQueryAllPackagesPermission_content))
                .onSetPositiveButton(getString(R.string.DialogGuideQueryAllPackagesPermission_ok), (baseDialog, datas) -> {
                    activity.askQueryAllPackagesPermission(new Callback<Boolean>() {
                        @Override
                        public Void call() {
                            if (activity.hasIgnoreBatteryOptimizationsPermission()) {
                                if (boot.isBootPermissionAvailable(context, true)) {
                                    showBootPermissionDialog();
                                } else if (isMIUIDevice()) {
                                    showMIUIAppLockDialog();
                                } else {
                                    showFinishDialog();
                                }
                            } else {
                                showIgnoreBatteryOptimizationsPermissionDialog();
                            }
                            return null;
                        }
                    });
                })
                .onSetNegativeButton(getString(R.string.DialogGuideQueryAllPackagesPermission_cancel), (baseDialog) -> {
                    preferences.updateLastTimeSetup();
                    close();
                })
                .setCancelable(false)
                .setCanOntouchOutside(false)
                .build();

        dialog.show(activity.getSupportFragmentManager(), DialogGuideSetup.class.getName() + ".showQueryAllPackagesPermissionDialog");
    }

    private void showIgnoreBatteryOptimizationsPermissionDialog() {
        close();

        dialog = new DialogGuideSetup.ExtendBuilder()
                .setTitle(getString(R.string.DialogGuideIgnoreBatteryOptimizationsPermission_title))
                .setContent(getString(R.string.DialogGuideIgnoreBatteryOptimizationsPermission_content))
                .onSetPositiveButton(getString(R.string.DialogGuideIgnoreBatteryOptimizationsPermission_ok), (baseDialog, datas) -> {
                    activity.askIgnoreBatteryOptimizationsPermission(new Callback<Boolean>() {
                        @Override
                        public Void call() {
                            if (boot.isBootPermissionAvailable(context, true)) {
                                showBootPermissionDialog();
                            } else if (isMIUIDevice()) {
                                showMIUIAppLockDialog();
                            } else {
                                showFinishDialog();
                            }
                            return null;
                        }
                    });
                })
                .onSetNegativeButton(getString(R.string.DialogGuideIgnoreBatteryOptimizationsPermission_cancel), (baseDialog) -> {
                    preferences.updateLastTimeSetup();
                    close();
                })
                .setCancelable(false)
                .setCanOntouchOutside(false)
                .build();

        dialog.show(activity.getSupportFragmentManager(), DialogGuideSetup.class.getName() + ".showIgnoreBatteryOptimizationsPermissionDialog");
    }

    private void showBootPermissionDialog() {
        close();

        dialog = new DialogGuideSetup.ExtendBuilder()
                .setTitle(getString(R.string.DialogGuideBootPermission_title))
                .setContent(getString(R.string.DialogGuideBootPermission_content))
                .onSetPositiveButton(getString(R.string.DialogGuideBootPermission_ok), (baseDialog, datas) -> {
                    boot.forceBootPermission(context, true, true);

                    if (isMIUIDevice()) {
                        showMIUIAppLockDialog();
                    } else {
                        showFinishDialog();
                    }
                })
                .onSetNegativeButton(getString(R.string.DialogGuideBootPermission_cancel), (baseDialog) -> {
                    preferences.updateLastTimeSetup();
                    close();
                })
                .setCancelable(false)
                .setCanOntouchOutside(false)
                .build();

        dialog.show(activity.getSupportFragmentManager(), DialogGuideSetup.class.getName() + ".showBootPermissionDialog");
    }

    private void showMIUIAppLockDialog() {
        close();

        dialog = new DialogGuideMIUI.ExtendBuilder()
                .onSetPositiveButton(getString(R.string.DialogGuideMIUI_ok), (baseDialog, datas) -> {
                    showFinishDialog();
                })
                .setCancelable(false)
                .setCanOntouchOutside(false)
                .build();

        dialog.show(activity.getSupportFragmentManager(), DialogGuideMIUI.class.getName() + ".showMIUIAppLockInstructionsDialog");
    }

    private void showFinishDialog() {
        close();

        dialog = new DialogGuideInfo.ExtendBuilder()
                .setTitle(getString(R.string.DialogGuideFinish_title))
                .setContent(getString(R.string.DialogGuideFinish_content))
                .onSetPositiveButton(getString(R.string.DialogGuideFinish_ok), (baseDialog, datas) -> {
                    preferences.updateLastTimeSetup();
                    preferences.forceFirstSetup(); // The user has completed all the settings.
                    close();
                })
                .setCancelable(false)
                .setCanOntouchOutside(false)
                .build();

        dialog.show(activity.getSupportFragmentManager(), DialogGuideInfo.class.getName() + ".showFinishDialog");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void close() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private String getString(@StringRes int resId) {
        return context.getString(resId);
    }

    private boolean isMIUIDevice() {
        String brand = Build.BRAND.toLowerCase(Locale.ROOT);
        switch (brand) {
            case "xiaomi":
            case "poco":
            case "redmi":
                return true;
        }
        return false;
    }

}
