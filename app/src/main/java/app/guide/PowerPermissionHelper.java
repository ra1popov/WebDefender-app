package app.guide;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Locale;

public class PowerPermissionHelper {

    /***
     * Asus
     */
    private static final String BRAND_ASUS = "asus";
    private static final String PACKAGE_ASUS_MAIN = "com.asus.mobilemanager";
    private static final String PACKAGE_ASUS_COMPONENT = "com.asus.mobilemanager.powersaver.PowerSaverSettings";
    private static final String PACKAGE_ASUS_COMPONENT2 = "com.asus.mobilemanager.powersaver.PowerSaverSettingsActivity";

    /***
     * Xiaomi
     */
    private static final String BRAND_XIAOMI = "xiaomi";
    private static final String BRAND_XIAOMI_POCO = "poco";
    private static final String BRAND_XIAOMI_REDMI = "redmi";
    private static final String PACKAGE_XIAOMI_MAIN = "com.miui.securitycenter";
    private static final String PACKAGE_XIAOMI_COMPONENT = "com.miui.powercenter.PowerSettings";
    private static final String PACKAGE_XIAOMI_ACTION = "miui.intent.action.POWER_HIDE_MODE_APP_LIST";

    /***
     * Honor
     */
    private static final String BRAND_HONOR = "honor";
    private static final String PACKAGE_HONOR_MAIN = "com.huawei.systemmanager";
    private static final String PACKAGE_HONOR_COMPONENT = "com.huawei.systemmanager.optimize.process.ProtectActivity";

    /***
     * Huawei
     */
    private static final String BRAND_HUAWEI = "huawei";
    private static final String PACKAGE_HUAWEI_MAIN = "com.huawei.systemmanager";
    private static final String PACKAGE_HUAWEI_COMPONENT = "com.huawei.systemmanager.optimize.process.ProtectActivity";

    /**
     * Oppo
     */
    private static final String BRAND_OPPO = "oppo";
    private static final String PACKAGE_OPPO_MAIN = "com.coloros.phonemanager";
    private static final String PACKAGE_OPPO_FALLBACK = "com.coloros.oppoguardelf";
    private static final String PACKAGE_OPPO_COMPONENT = "com.coloros.deepsleepui.ui.activity.ProcessWhiteListActivity";
    private static final String PACKAGE_OPPO_COMPONENT_FALLBACK = "com.coloros.powermanager.fuelgaue.PowerSaverModeActivity";
    private static final String PACKAGE_OPPO_COMPONENT_FALLBACK2 = "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity";
    private static final String PACKAGE_OPPO_COMPONENT_FALLBACK3 = "com.coloros.powermanager.fuelgaue.PowerConsumptionActivity";

    /**
     * Vivo
     */
    private static final String BRAND_VIVO = "vivo";
    private static final String PACKAGE_VIVO_MAIN = "com.iqoo.secure";
    private static final String PACKAGE_VIVO_COMPONENT = "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity";

    /**
     * Nokia
     */
    private static final String BRAND_NOKIA = "nokia";
    private static final String PACKAGE_NOKIA_MAIN = "com.evenwell.powersaving.g3";
    private static final String PACKAGE_NOKIA_COMPONENT = "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity";

    /***
     * Samsung
     */
    private static final String BRAND_SAMSUNG = "samsung";
    private static final String PACKAGE_SAMSUNG_MAIN = "com.samsung.android.lool";
    private static final String PACKAGE_SAMSUNG_FALLBACK = "com.samsung.android.sm";
    private static final String PACKAGE_SAMSUNG_COMPONENT = "com.samsung.android.sm.ui.battery.BatteryActivity";
    private static final String PACKAGE_SAMSUNG_COMPONENT2 = "com.samsung.android.sm.battery.ui.BatteryActivity";
    private static final String PACKAGE_SAMSUNG_COMPONENT3 = "com.samsung.android.sm.battery.ui.usage.CheckableAppListActivity";
    private static final String PACKAGE_SAMSUNG_COMPONENT4 = "com.samsung.android.sm.ui.appmanagement.AppManagementActivity";
    private static final String PACKAGE_SAMSUNG_COMPONENT5 = "com.samsung.android.sm.ui.cstyleboard.SmartManagerMainActivity";
    private static final String PACKAGE_SAMSUNG_COMPONENT_FALLBACK = "com.samsung.android.sm.ui.battery.BatteryActivity";
    private static final String PACKAGE_SAMSUNG_COMPONENT_FALLBACK2 = "com.samsung.android.sm.battery.ui.BatteryActivity";
    private static final String PACKAGE_SAMSUNG_COMPONENT_FALLBACK3 = "com.samsung.android.sm.battery.ui.usage.CheckableAppListActivity";
    private static final String PACKAGE_SAMSUNG_COMPONENT_FALLBACK4 = "com.samsung.android.sm.ui.appmanagement.AppManagementActivity";
    private static final String PACKAGE_SAMSUNG_COMPONENT_FALLBACK5 = "com.samsung.android.sm.ui.cstyleboard.SmartManagerMainActivity";

    /***
     * One plus
     */
    private static final String BRAND_ONE_PLUS = "oneplus";
    private static final String PACKAGE_ONE_PLUS_MAIN = "com.oneplus.security";
    private static final String PACKAGE_ONE_PLUS_COMPONENT = "com.oneplus.security.chainlaunch.view.RunningProcessesActivity";
    private static final String PACKAGE_ONE_PLUS_ACTION = "com.android.settings.action.BACKGROUND_OPTIMIZE";

    /***
     * Motorola
     */
    private static final String BRAND_MOTOROLA = "motorola";
    private static final String PACKAGE_MOTOROLA_MAIN = "com.motorola.moto";
    private static final String PACKAGE_MOTOROLA_COMPONENT = "com.motorola.moto.bgoptimize.BgOptimizeMainActivity";

    /***
     * Lenovo
     */
    private static final String BRAND_LENOVO = "lenovo";
    private static final String PACKAGE_LENOVO_MAIN = "com.lenovo.security";
    private static final String PACKAGE_LENOVO_COMPONENT = "com.lenovo.security.purebackground.PureBackgroundActivity";
    private static final String PACKAGE_LENOVO_COMPONENT2 = "com.lenovo.security.purebackground.PureBackgroundSetting";
    private static final String PACKAGE_LENOVO_COMPONENT3 = "com.lenovo.security.purebackground.PureBackgroundSettings";

    /***
     * LG
     */
    private static final String BRAND_LG = "lg";
    private static final String PACKAGE_LG_MAIN = "com.lge.powersavingmode";
    private static final String PACKAGE_LG_COMPONENT = "com.lge.powersavingmode.PSMLandingActivity";

    /***
     * Meizu
     */
    private static final String BRAND_MEIZU = "meizu";
    private static final String PACKAGE_MEIZU_MAIN = "com.meizu.safe";
    private static final String PACKAGE_MEIZU_COMPONENT = "com.meizu.safe.powerui.PowerAppPermissionActivity";
    private static final String PACKAGE_MEIZU_COMPONENT_FOR_ACTION_REQUEST = "com.meizu.safe.security.AppSecActivity";
    private static final String PACKAGE_MEIZU_ACTION = "com.meizu.safe.security.SHOW_APPSEC";

    /***
     * HTC
     */
    private static final String BRAND_HTC = "htc";
    private static final String PACKAGE_HTC_MAIN = "com.htc.pitroad";
    private static final String PACKAGE_HTC_COMPONENT = "com.htc.pitroad.landingpage.activity.LandingPageActivity";

    private final List<String> PACKAGES_TO_CHECK_FOR_PERMISSION = List.of(
            PACKAGE_ASUS_MAIN,
            PACKAGE_XIAOMI_MAIN,
            PACKAGE_HONOR_MAIN,
            PACKAGE_HUAWEI_MAIN,
            PACKAGE_OPPO_MAIN,
            PACKAGE_OPPO_FALLBACK,
            PACKAGE_VIVO_MAIN,
            PACKAGE_NOKIA_MAIN,
            PACKAGE_SAMSUNG_MAIN,
            PACKAGE_SAMSUNG_FALLBACK,
            PACKAGE_ONE_PLUS_MAIN,
            PACKAGE_MOTOROLA_MAIN,
            PACKAGE_LENOVO_MAIN,
            PACKAGE_LG_MAIN,
            PACKAGE_MEIZU_MAIN,
            PACKAGE_HTC_MAIN
    );

    /**
     * It will attempt to open the specific manufacturer settings screen with the battery permission
     * If [open] is changed to false it will just check the screen existence
     *
     * @param context  Context
     * @param open,    if true it will attempt to open the activity, otherwise it will just check its existence
     * @param newTask, if true when the activity is attempted to be opened it will add FLAG_ACTIVITY_NEW_TASK to the intent
     * @return true if the activity was opened or is confirmed that it exists (depending on [open]]), false otherwise
     */
    public boolean forcePowerPermission(@NonNull Context context, boolean open, boolean newTask) {
        String brand = Build.BRAND.toLowerCase(Locale.ROOT);

        switch (brand) {
            case BRAND_ASUS:
                return powerAsus(context, open, newTask);

            case BRAND_XIAOMI:
            case BRAND_XIAOMI_POCO:
            case BRAND_XIAOMI_REDMI:
                return powerXiaomi(context, open, newTask);

            case BRAND_HONOR:
                return powerHonor(context, open, newTask);

            case BRAND_HUAWEI:
                return powerHuawei(context, open, newTask);

            case BRAND_OPPO:
                return powerOppo(context, open, newTask);

            case BRAND_VIVO:
                return powerVivo(context, open, newTask);

            case BRAND_NOKIA:
                return powerNokia(context, open, newTask);

            case BRAND_SAMSUNG:
                return powerSamsung(context, open, newTask);

            case BRAND_ONE_PLUS:
                return powerOnePlus(context, open, newTask);

            case BRAND_MOTOROLA:
                return powerMotorola(context, open, newTask);

            case BRAND_LENOVO:
                return powerLenovo(context, open, newTask);

            case BRAND_LG:
                return powerLg(context, open, newTask);

            case BRAND_MEIZU:
                return bootMeizu(context, open, newTask);

            case BRAND_HTC:
                return bootHtc(context, open, newTask);

            default:
                return false;
        }
    }

    /**
     * Checks whether the battery permission is present in the manufacturer and supported by the library
     *
     * @param context         Context
     * @param onlyIfSupported if true, the method will only return true if the screen is supported by the library.
     *                        If false, the method will return true as long as the permission exist even if the screen is not supported
     *                        by the library.
     * @return true if battery permission is present in the manufacturer and supported by the library, false otherwise
     */
    @SuppressLint("QueryPermissionsNeeded")
    public boolean isPowerPermissionAvailable(@NonNull Context context, boolean onlyIfSupported) {
        List<ApplicationInfo> packages;
        PackageManager pm = context.getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if (PACKAGES_TO_CHECK_FOR_PERMISSION.contains(packageInfo.packageName) && (!onlyIfSupported || forcePowerPermission(context, false, false))) {
                return true;
            }
        }
        return false;
    }

    private boolean powerAsus(@NonNull Context context, boolean open, boolean newTask) {
        List<String> packageList = List.of(PACKAGE_ASUS_MAIN);
        List<Intent> intentList = List.of(
                getIntent(PACKAGE_ASUS_MAIN, PACKAGE_ASUS_COMPONENT, newTask),
                getIntent(PACKAGE_ASUS_MAIN, PACKAGE_ASUS_COMPONENT2, newTask)
        );
        return power(context, packageList, intentList, open);
    }

    private boolean powerXiaomi(@NonNull Context context, boolean open, boolean newTask) {
        if (launchXiaomiAppInfo(context, open, newTask)) {
            return true;
        } else {
            return power(context, List.of(PACKAGE_XIAOMI_MAIN), List.of(getIntent(PACKAGE_XIAOMI_MAIN, PACKAGE_XIAOMI_COMPONENT, newTask)), open);
        }
    }

    private boolean launchXiaomiAppInfo(@NonNull Context context, boolean open, boolean newTask) {
        try {
            Intent intent = getIntentFromAction(PACKAGE_XIAOMI_ACTION, newTask);
            intent.addCategory(Intent.CATEGORY_DEFAULT);

            if (open) {
                context.startActivity(intent);
                return true;
            } else {
                return isActivityFound(context, intent);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean powerHonor(@NonNull Context context, boolean open, boolean newTask) {
        return power(context, List.of(PACKAGE_HONOR_MAIN), List.of(getIntent(PACKAGE_HONOR_MAIN, PACKAGE_HONOR_COMPONENT, newTask)), open);
    }

    private boolean powerHuawei(@NonNull Context context, boolean open, boolean newTask) {
        return power(context, List.of(PACKAGE_HUAWEI_MAIN), List.of(getIntent(PACKAGE_HUAWEI_MAIN, PACKAGE_HUAWEI_COMPONENT, newTask)), open);
    }

    private boolean powerOppo(@NonNull Context context, boolean open, boolean newTask) {
        List<String> packageList = List.of(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_FALLBACK);
        List<Intent> intentList = List.of(
                getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT, newTask),
                getIntent(PACKAGE_OPPO_FALLBACK, PACKAGE_OPPO_COMPONENT_FALLBACK, newTask),
                getIntent(PACKAGE_OPPO_FALLBACK, PACKAGE_OPPO_COMPONENT_FALLBACK2, newTask),
                getIntent(PACKAGE_OPPO_FALLBACK, PACKAGE_OPPO_COMPONENT_FALLBACK3, newTask)
        );

        if (power(context, packageList, intentList, open)) {
            return true;
        } else {
            return launchOppoAppInfo(context, open, newTask);
        }
    }

    private boolean launchOppoAppInfo(@NonNull Context context, boolean open, boolean newTask) {
        try {
            Intent intent = getIntentFromAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, newTask);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + context.getPackageName()));

            if (open) {
                context.startActivity(intent);
                return true;
            } else {
                return isActivityFound(context, intent);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean powerVivo(@NonNull Context context, boolean open, boolean newTask) {
        return power(context, List.of(PACKAGE_VIVO_MAIN), List.of(getIntent(PACKAGE_VIVO_MAIN, PACKAGE_VIVO_COMPONENT, newTask)), open);
    }

    private boolean powerNokia(@NonNull Context context, boolean open, boolean newTask) {
        return power(context, List.of(PACKAGE_NOKIA_MAIN), List.of(getIntent(PACKAGE_NOKIA_MAIN, PACKAGE_NOKIA_COMPONENT, newTask)), open);
    }

    private boolean powerSamsung(@NonNull Context context, boolean open, boolean newTask) {
        List<String> packageList = List.of(PACKAGE_SAMSUNG_MAIN);
        List<Intent> intentList = List.of(
                getIntent(PACKAGE_SAMSUNG_MAIN, PACKAGE_SAMSUNG_COMPONENT, newTask),
                getIntent(PACKAGE_SAMSUNG_MAIN, PACKAGE_SAMSUNG_COMPONENT2, newTask),
                getIntent(PACKAGE_SAMSUNG_MAIN, PACKAGE_SAMSUNG_COMPONENT3, newTask),
                getIntent(PACKAGE_SAMSUNG_MAIN, PACKAGE_SAMSUNG_COMPONENT4, newTask),
                getIntent(PACKAGE_SAMSUNG_MAIN, PACKAGE_SAMSUNG_COMPONENT5, newTask),
                getIntent(PACKAGE_SAMSUNG_FALLBACK, PACKAGE_SAMSUNG_COMPONENT_FALLBACK, newTask),
                getIntent(PACKAGE_SAMSUNG_FALLBACK, PACKAGE_SAMSUNG_COMPONENT_FALLBACK2, newTask),
                getIntent(PACKAGE_SAMSUNG_FALLBACK, PACKAGE_SAMSUNG_COMPONENT_FALLBACK3, newTask),
                getIntent(PACKAGE_SAMSUNG_FALLBACK, PACKAGE_SAMSUNG_COMPONENT_FALLBACK4, newTask),
                getIntent(PACKAGE_SAMSUNG_FALLBACK, PACKAGE_SAMSUNG_COMPONENT_FALLBACK5, newTask)
        );
        return power(context, packageList, intentList, open);
    }

    private boolean powerOnePlus(@NonNull Context context, boolean open, boolean newTask) {
        boolean powerResult = power(context, List.of(PACKAGE_ONE_PLUS_MAIN), List.of(getIntent(PACKAGE_ONE_PLUS_MAIN, PACKAGE_ONE_PLUS_COMPONENT, newTask)), open);
        boolean powerFromActionResult = powerFromAction(
                context,
                List.of(getIntentFromAction(PACKAGE_ONE_PLUS_ACTION, newTask)),
                open
        );
        return powerResult || powerFromActionResult;
    }

    private boolean powerMotorola(@NonNull Context context, boolean open, boolean newTask) {
        return power(context, List.of(PACKAGE_MOTOROLA_MAIN), List.of(getIntent(PACKAGE_MOTOROLA_MAIN, PACKAGE_MOTOROLA_COMPONENT, newTask)), open);
    }

    private boolean powerLenovo(@NonNull Context context, boolean open, boolean newTask) {
        List<String> packageList = List.of(PACKAGE_LENOVO_MAIN);
        List<Intent> intentList = List.of(
                getIntent(PACKAGE_LENOVO_MAIN, PACKAGE_LENOVO_COMPONENT, newTask),
                getIntent(PACKAGE_LENOVO_MAIN, PACKAGE_LENOVO_COMPONENT2, newTask),
                getIntent(PACKAGE_LENOVO_MAIN, PACKAGE_LENOVO_COMPONENT3, newTask)
        );
        return power(context, packageList, intentList, open);
    }

    private boolean powerLg(@NonNull Context context, boolean open, boolean newTask) {
        return power(context, List.of(PACKAGE_LG_MAIN), List.of(getIntent(PACKAGE_LG_MAIN, PACKAGE_LG_COMPONENT, newTask)), open);
    }

    private boolean bootMeizu(@NonNull Context context, boolean open, boolean newTask) {
        if (launchMeizuAppInfo(context, open, newTask)) {
            return true;
        } else {
            return power(context, List.of(PACKAGE_MEIZU_MAIN), List.of(getIntent(PACKAGE_MEIZU_MAIN, PACKAGE_MEIZU_COMPONENT, newTask)), open);
        }
    }

    private boolean launchMeizuAppInfo(@NonNull Context context, boolean open, boolean newTask) {
        try {
            Intent intent = getIntentFromAction(PACKAGE_MEIZU_ACTION, newTask);
            intent.setComponent(new ComponentName(PACKAGE_MEIZU_MAIN, PACKAGE_MEIZU_COMPONENT_FOR_ACTION_REQUEST));
            intent.putExtra("packageName", context.getPackageName());

            if (open) {
                context.startActivity(intent);
                return true;
            } else {
                return isActivityFound(context, intent);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean bootHtc(@NonNull Context context, boolean open, boolean newTask) {
        return power(context, List.of(PACKAGE_HTC_MAIN), List.of(getIntent(PACKAGE_HTC_MAIN, PACKAGE_HTC_COMPONENT, newTask)), open);
    }

    private boolean startIntent(@NonNull Context context, @NonNull Intent intent) {
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    @SuppressLint("QueryPermissionsNeeded")
    private boolean isPackageExists(@NonNull Context context, String targetPackage) {
        List<ApplicationInfo> packages;
        PackageManager pm = context.getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(targetPackage)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates an intent with the passed package and component name
     *
     * @param packageName   The name of package
     * @param componentName The name of component
     * @param newTask       if true when the activity is attempted to be opened it will add FLAG_ACTIVITY_NEW_TASK to the intent
     * @return the intent generated
     */
    private Intent getIntent(String packageName, String componentName, boolean newTask) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, componentName));
        if (newTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    /**
     * Generates an intent with the passed action
     *
     * @param intentAction The name of action
     * @param newTask      if true when the activity is attempted to be opened it will add FLAG_ACTIVITY_NEW_TASK to the intent
     * @return the intent generated
     */
    private Intent getIntentFromAction(@NonNull String intentAction, boolean newTask) {
        Intent intent = new Intent();
        intent.setAction(intentAction);
        if (newTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    /**
     * Will query the passed intent to check whether the Activity really exists
     *
     * @param context Context
     * @param intent, intent to open an activity
     * @return true if activity is found, false otherwise
     */
    private boolean isActivityFound(@NonNull Context context, @NonNull Intent intent) {
        List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return !activities.isEmpty();
    }

    /**
     * Will query the passed list of intents to check whether any of the activities exist
     *
     * @param context  Context
     * @param intents, list of intents to open an activity
     * @return true if activity is found, false otherwise
     */
    private boolean areActivitiesFound(@NonNull Context context, List<Intent> intents) {
        for (Intent intent : intents) {
            if (isActivityFound(context, intent)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Will attempt to open the battery settings activity from the passed list of intents in order.
     * The first activity found will be opened.
     *
     * @param context Context
     * @param intents list of intents
     * @return true if an activity was opened, false otherwise
     */
    private boolean openPowerScreen(@NonNull Context context, List<Intent> intents) {
        for (Intent intent : intents) {
            if (isActivityFound(context, intent)) {
                return startIntent(context, intent);
            }
        }
        return false;
    }

    /**
     * Will trigger the common battery permission logic. If [open] is true it will attempt to open the specific
     * manufacturer setting screen, otherwise it will just check for its existence
     *
     * @param context   Context
     * @param packages, list of known packages of the corresponding manufacturer
     * @param intents,  list of known intents that open the corresponding manufacturer settings screens
     * @param open,     if true it will attempt to open the settings screen, otherwise it just check its existence
     * @return true if the screen was opened or exists, false if it doesn't exist or could not be opened
     */
    private boolean power(@NonNull Context context, List<String> packages, List<Intent> intents, boolean open) {
        for (String packageName : packages) {
            if (isPackageExists(context, packageName)) {
                if (open) {
                    return openPowerScreen(context, intents);
                } else {
                    return areActivitiesFound(context, intents);
                }
            }
        }
        return false;
    }

    /**
     * Will trigger the common battery permission logic. If [open] is true it will attempt to open the specific
     * manufacturer setting screen, otherwise it will just check for its existence
     *
     * @param context        Context
     * @param intentActions, list of known intent actions that open the corresponding manufacturer settings screens
     * @param open,          if true it will attempt to open the settings screen, otherwise it just check its existence
     * @return true if the screen was opened or exists, false if it doesn't exist or could not be opened
     */
    private boolean powerFromAction(@NonNull Context context, List<Intent> intentActions, boolean open) {
        if (open) {
            return openPowerScreen(context, intentActions);
        } else {
            return areActivitiesFound(context, intentActions);
        }
    }

}
