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

public class BootPermissionHelper {

    /***
     * Asus
     */
    private static final String BRAND_ASUS = "asus";
    private static final String PACKAGE_ASUS_MAIN = "com.asus.mobilemanager";
    private static final String PACKAGE_ASUS_COMPONENT = "com.asus.mobilemanager.autostart.AutoStartActivity";
    private static final String PACKAGE_ASUS_COMPONENT_FOR_ACTION_REQUEST = "com.asus.mobilemanager.entry.FunctionActivity";

    /***
     * Xiaomi
     */
    private static final String BRAND_XIAOMI = "xiaomi";
    private static final String BRAND_XIAOMI_POCO = "poco";
    private static final String BRAND_XIAOMI_REDMI = "redmi";
    private static final String PACKAGE_XIAOMI_MAIN = "com.miui.securitycenter";
    private static final String PACKAGE_XIAOMI_COMPONENT = "com.miui.permcenter.autostart.AutoStartManagementActivity";
    private static final String PACKAGE_XIAOMI_ACTION = "miui.intent.action.OP_AUTO_START";

    /***
     * Letv
     */
    private static final String BRAND_LETV = "letv";
    private static final String PACKAGE_LETV_MAIN = "com.letv.android.letvsafe";
    private static final String PACKAGE_LETV_COMPONENT = "com.letv.android.letvsafe.AutobootManageActivity";
    private static final String PACKAGE_LETV_COMPONENT_FOR_ACTION_REQUEST = "com.letv.android.letvsafe.AutobootManageActivity";
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
    private static final String PACKAGE_HUAWEI_COMPONENT = "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity";
    private static final String PACKAGE_HUAWEI_COMPONENT2 = "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity";
    private static final String PACKAGE_HUAWEI_COMPONENT3 = "com.huawei.systemmanager.optimize.bootstart.BootStartActivity";
    private static final String PACKAGE_HUAWEI_COMPONENT4 = "com.huawei.systemmanager.optimize.process.ProtectActivity";

    /**
     * Oppo
     */
    private static final String BRAND_OPPO = "oppo";
    private static final String PACKAGE_OPPO_MAIN = "com.coloros.safecenter";
    private static final String PACKAGE_OPPO_FALLBACK = "com.coloros.safe";
    private static final String PACKAGE_OPPO_FALLBACKB = "com.oppo.safe";
    private static final String PACKAGE_OPPO_COMPONENT = "com.coloros.safecenter.permission.startup.StartupAppListActivity";
    private static final String PACKAGE_OPPO_COMPONENT2 = "com.coloros.safecenter.startupapp.StartupAppListActivity";
    private static final String PACKAGE_OPPO_COMPONENT3 = "com.coloros.safecenter.permission.startup.FakeActivity";
    private static final String PACKAGE_OPPO_COMPONENT4 = "com.coloros.safecenter.permission.startupapp.StartupAppListActivity";
    private static final String PACKAGE_OPPO_COMPONENT5 = "com.coloros.safecenter.permission.startupmanager.StartupAppListActivity";
    private static final String PACKAGE_OPPO_COMPONENT6 = "com.coloros.safecenter.permission.startsettings";
    private static final String PACKAGE_OPPO_COMPONENT7 = "com.coloros.safecenter.permission.startupapp.startupmanager";
    private static final String PACKAGE_OPPO_COMPONENT8 = "com.coloros.safecenter.permission.startupmanager.startupActivity";
    private static final String PACKAGE_OPPO_COMPONENT9 = "com.coloros.safecenter.permission.startup.startupapp.startupmanager";
    private static final String PACKAGE_OPPO_COMPONENT10 = "com.coloros.privacypermissionsentry.PermissionTopActivity.Startupmanager";
    private static final String PACKAGE_OPPO_COMPONENT11 = "com.coloros.privacypermissionsentry.PermissionTopActivity";
    private static final String PACKAGE_OPPO_COMPONENT12 = "com.coloros.safecenter.FakeActivity";
    private static final String PACKAGE_OPPO_COMPONENT13 = "com.coloros.safecenter.startup.StartupAppListActivity";
    private static final String PACKAGE_OPPO_COMPONENT_FALLBACK = "com.coloros.safe.permission.startup.StartupAppListActivity";
    private static final String PACKAGE_OPPO_COMPONENT_FALLBACK2 = "com.coloros.safe.permission.startupapp.StartupAppListActivity";
    private static final String PACKAGE_OPPO_COMPONENT_FALLBACK3 = "com.coloros.safe.permission.startupmanager.StartupAppListActivity";
    private static final String PACKAGE_OPPO_COMPONENT_FALLBACKB = "com.oppo.safe.permission.startup.StartupAppListActivity";
    private static final String PACKAGE_OPPO_COMPONENT_FOR_ACTION_REQUEST = "com.coloros.safecenter.startupapp.StartupAppListActivity";

    /**
     * Vivo
     */
    private static final String BRAND_VIVO = "vivo";
    private static final String PACKAGE_VIVO_MAIN = "com.iqoo.secure";
    private static final String PACKAGE_VIVO_FALLBACK = "com.vivo.permissionmanager";
    private static final String PACKAGE_VIVO_COMPONENT = "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager";
    private static final String PACKAGE_VIVO_COMPONENT_FALLBACK = "com.vivo.permissionmanager.activity.BgStartUpManagerActivity";

    /***
     * One plus
     */
    private static final String BRAND_ONE_PLUS = "oneplus";
    private static final String PACKAGE_ONE_PLUS_MAIN = "com.oneplus.security";
    private static final String PACKAGE_ONE_PLUS_COMPONENT = "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity";

    /***
     * Nubia
     */
    private static final String BRAND_NUBIA = "nubia";
    private static final String PACKAGE_NUBIA_MAIN = "com.nubia.security2";
    private static final String PACKAGE_NUBIA_COMPONENT = "com.nubia.security2.ui.startmanager.StartAppListActivity";

    /***
     * Infinix
     */
    private static final String BRAND_INFINIX = "infinix";
    private static final String PACKAGE_INFINIX_MAIN = "com.infinix.xlauncher";
    private static final String PACKAGE_INFINIX_COMPONENT = "com.infinix.xlauncher.StartupAppMgrActivity";

    /***
     * TCL
     */
    private static final String BRAND_TCL = "tcl";
    private static final String PACKAGE_TCL_MAIN = "com.tct.security";
    private static final String PACKAGE_TCL_COMPONENT = "com.tct.security.appautostart.AutoStartListActivity";

    /***
     * Lenovo
     */
    private static final String BRAND_LENOVO = "lenovo";
    private static final String PACKAGE_LENOVO_MAIN = "com.lenovo.safecenter";
    private static final String PACKAGE_LENOVO_COMPONENT = "com.lenovo.performancecenter.performance.BootSpeedActivity";

    /***
     * Meizu
     */
    private static final String BRAND_MEIZU = "meizu";
    private static final String PACKAGE_MEIZU_MAIN = "com.meizu.safe";
    private static final String PACKAGE_MEIZU_COMPONENT = "com.meizu.safe.permission.SmartBGActivity";

    /***
     * HTC
     */
    private static final String BRAND_HTC = "htc";
    private static final String PACKAGE_HTC_MAIN = "com.htc.pitroad";
    private static final String PACKAGE_HTC_COMPONENT = "com.htc.pitroad.landingpage.activity.LandingPageActivity";

    /***
     * Transsion
     */
    private static final String BRAND_TRANSSION = "transsion";
    private static final String PACKAGE_TRANSSION_MAIN = "com.transsion.phonemanager";
    private static final String PACKAGE_TRANSSION_COMPONENT = "com.itel.autobootmanager.activity.AutoBootMgrActivity";


    private final List<String> PACKAGES_TO_CHECK_FOR_PERMISSION = List.of(
            PACKAGE_ASUS_MAIN,
            PACKAGE_XIAOMI_MAIN,
            PACKAGE_LETV_MAIN,
            PACKAGE_HONOR_MAIN,
            PACKAGE_HUAWEI_MAIN,
            PACKAGE_OPPO_MAIN,
            PACKAGE_OPPO_FALLBACK,
            PACKAGE_OPPO_FALLBACKB,
            PACKAGE_VIVO_MAIN,
            PACKAGE_VIVO_FALLBACK,
            PACKAGE_ONE_PLUS_MAIN,
            PACKAGE_NUBIA_MAIN,
            PACKAGE_INFINIX_MAIN,
            PACKAGE_TCL_MAIN,
            PACKAGE_LENOVO_MAIN,
            PACKAGE_MEIZU_MAIN,
            PACKAGE_HTC_MAIN,
            PACKAGE_TRANSSION_MAIN
    );

    /**
     * It will attempt to open the specific manufacturer settings screen with the autostart permission
     * If [open] is changed to false it will just check the screen existence
     *
     * @param context  Context
     * @param open,    if true it will attempt to open the activity, otherwise it will just check its existence
     * @param newTask, if true when the activity is attempted to be opened it will add FLAG_ACTIVITY_NEW_TASK to the intent
     * @return true if the activity was opened or is confirmed that it exists (depending on [open]]), false otherwise
     */
    public boolean forceBootPermission(@NonNull Context context, boolean open, boolean newTask) {
        String brand = Build.BRAND.toLowerCase(Locale.ROOT);

        switch (brand) {
            case BRAND_ASUS:
                return bootAsus(context, open, newTask);

            case BRAND_XIAOMI:
            case BRAND_XIAOMI_POCO:
            case BRAND_XIAOMI_REDMI:
                return bootXiaomi(context, open, newTask);

            case BRAND_LETV:
                return bootLetv(context, open, newTask);

            case BRAND_HONOR:
                return bootHonor(context, open, newTask);

            case BRAND_HUAWEI:
                return bootHuawei(context, open, newTask);

            case BRAND_OPPO:
                return bootOppo(context, open, newTask);

            case BRAND_VIVO:
                return bootVivo(context, open, newTask);

            case BRAND_ONE_PLUS:
                return bootOnePlus(context, open, newTask);

            case BRAND_NUBIA:
                return bootNubia(context, open, newTask);

            case BRAND_INFINIX:
                return bootInfinix(context, open, newTask);

            case BRAND_TCL:
                return bootTcl(context, open, newTask);

            case BRAND_LENOVO:
                return bootLenovo(context, open, newTask);

            case BRAND_MEIZU:
                return bootMeizu(context, open, newTask);

            case BRAND_HTC:
                return bootHtc(context, open, newTask);

            case BRAND_TRANSSION:
                return bootTranssion(context, open, newTask);

            default:
                return false;
        }
    }

    /**
     * Checks whether the autostart permission is present in the manufacturer and supported by the library
     *
     * @param context         Context
     * @param onlyIfSupported if true, the method will only return true if the screen is supported by the library.
     *                        If false, the method will return true as long as the permission exist even if the screen is not supported
     *                        by the library.
     * @return true if autostart permission is present in the manufacturer and supported by the library, false otherwise
     */
    @SuppressLint("QueryPermissionsNeeded")
    public boolean isBootPermissionAvailable(@NonNull Context context, boolean onlyIfSupported) {
        List<ApplicationInfo> packages;
        PackageManager pm = context.getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if (PACKAGES_TO_CHECK_FOR_PERMISSION.contains(packageInfo.packageName) && (!onlyIfSupported || forceBootPermission(context, false, false))) {
                return true;
            }
        }
        return false;
    }

    private boolean bootAsus(@NonNull Context context, boolean open, boolean newTask) {
        if (launchAsusAppInfo(context, open, newTask)) {
            return true;
        } else {
            return boot(context, List.of(PACKAGE_ASUS_MAIN), List.of(getIntent(PACKAGE_ASUS_MAIN, PACKAGE_ASUS_COMPONENT, newTask)), open);
        }
    }

    private boolean launchAsusAppInfo(@NonNull Context context, boolean open, boolean newTask) {
        try {
            Intent intent = getIntent(PACKAGE_ASUS_MAIN, PACKAGE_ASUS_COMPONENT_FOR_ACTION_REQUEST, newTask);
            intent.setData(Uri.parse("mobilemanager://function/entry/AutoStart"));

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

    private boolean bootXiaomi(@NonNull Context context, boolean open, boolean newTask) {
        if (launchXiaomiAppInfo(context, open, newTask)) {
            return true;
        } else {
            return boot(context, List.of(PACKAGE_XIAOMI_MAIN), List.of(getIntent(PACKAGE_XIAOMI_MAIN, PACKAGE_XIAOMI_COMPONENT, newTask)), open);
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

    private boolean bootLetv(@NonNull Context context, boolean open, boolean newTask) {
        if (launchLetvAppInfo(context, open, newTask)) {
            return true;
        } else {
            return boot(context, List.of(PACKAGE_LETV_MAIN), List.of(getIntent(PACKAGE_LETV_MAIN, PACKAGE_LETV_COMPONENT, newTask)), open);
        }
    }

    private boolean launchLetvAppInfo(@NonNull Context context, boolean open, boolean newTask) {
        try {
            Intent intent = getIntent(PACKAGE_LETV_MAIN, PACKAGE_LETV_COMPONENT_FOR_ACTION_REQUEST, newTask);
            intent.setData(Uri.parse("mobilemanager://function/entry/AutoStart"));

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

    private boolean bootHonor(@NonNull Context context, boolean open, boolean newTask) {
        return boot(context, List.of(PACKAGE_HONOR_MAIN), List.of(getIntent(PACKAGE_HONOR_MAIN, PACKAGE_HONOR_COMPONENT, newTask)), open);
    }

    private boolean bootHuawei(@NonNull Context context, boolean open, boolean newTask) {
        List<String> packageList = List.of(PACKAGE_HUAWEI_MAIN);
        List<Intent> intentList = List.of(
                getIntent(PACKAGE_HUAWEI_MAIN, PACKAGE_HUAWEI_COMPONENT, newTask),
                getIntent(PACKAGE_HUAWEI_MAIN, PACKAGE_HUAWEI_COMPONENT2, newTask),
                getIntent(PACKAGE_HUAWEI_MAIN, PACKAGE_HUAWEI_COMPONENT3, newTask),
                getIntent(PACKAGE_HUAWEI_MAIN, PACKAGE_HUAWEI_COMPONENT4, newTask)
        );
        return boot(context, packageList, intentList, open);
    }

    @SuppressLint("BatteryLife")
    private boolean bootOppo(@NonNull Context context, boolean open, boolean newTask) {
        List<String> packageList = List.of(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_FALLBACK);
        List<Intent> intentList = List.of(
                getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT, newTask),
                getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT2, newTask),
                getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT3, newTask),
                getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT4, newTask),
                getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT5, newTask),
                getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT6, newTask),
                getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT7, newTask),
                getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT8, newTask),
                getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT9, newTask),
                getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT10, newTask),
                getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT11, newTask),
                getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT12, newTask),
                getIntent(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT13, newTask),
                getIntent(PACKAGE_OPPO_FALLBACK, PACKAGE_OPPO_COMPONENT_FALLBACK, newTask),
                getIntent(PACKAGE_OPPO_FALLBACK, PACKAGE_OPPO_COMPONENT_FALLBACK2, newTask),
                getIntent(PACKAGE_OPPO_FALLBACK, PACKAGE_OPPO_COMPONENT_FALLBACK3, newTask),
                getIntent(PACKAGE_OPPO_FALLBACKB, PACKAGE_OPPO_COMPONENT_FALLBACKB, newTask)
        );
        if (launchOppoIgnoreBatteryOptimizations(context, open, newTask)) {
            return true;
        } else if (boot(context, packageList, intentList, open)) {
            return true;
        } else return launchOppoAppInfo(context, open, newTask);
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

    @SuppressLint("BatteryLife")
    private boolean launchOppoIgnoreBatteryOptimizations(@NonNull Context context, boolean open, boolean newTask) {
        try {
            Intent intent = getIntentFromAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, newTask);
            intent.setComponent(new ComponentName(PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT_FOR_ACTION_REQUEST));
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

    private boolean bootVivo(@NonNull Context context, boolean open, boolean newTask) {
        List<String> packageList = List.of(PACKAGE_VIVO_MAIN, PACKAGE_VIVO_FALLBACK);
        List<Intent> intentList = List.of(
                getIntent(PACKAGE_VIVO_MAIN, PACKAGE_VIVO_COMPONENT, newTask),
                getIntent(PACKAGE_VIVO_FALLBACK, PACKAGE_VIVO_COMPONENT_FALLBACK, newTask)
        );
        return boot(context, packageList, intentList, open);
    }

    private boolean bootOnePlus(@NonNull Context context, boolean open, boolean newTask) {
        return boot(context, List.of(PACKAGE_ONE_PLUS_MAIN), List.of(getIntent(PACKAGE_ONE_PLUS_MAIN, PACKAGE_ONE_PLUS_COMPONENT, newTask)), open);
    }

    private boolean bootNubia(@NonNull Context context, boolean open, boolean newTask) {
        return boot(context, List.of(PACKAGE_NUBIA_MAIN), List.of(getIntent(PACKAGE_NUBIA_MAIN, PACKAGE_NUBIA_COMPONENT, newTask)), open);
    }

    private boolean bootInfinix(@NonNull Context context, boolean open, boolean newTask) {
        return boot(context, List.of(PACKAGE_INFINIX_MAIN), List.of(getIntent(PACKAGE_INFINIX_MAIN, PACKAGE_INFINIX_COMPONENT, newTask)), open);
    }

    private boolean bootTcl(@NonNull Context context, boolean open, boolean newTask) {
        return boot(context, List.of(PACKAGE_TCL_MAIN), List.of(getIntent(PACKAGE_TCL_MAIN, PACKAGE_TCL_COMPONENT, newTask)), open);
    }

    private boolean bootLenovo(@NonNull Context context, boolean open, boolean newTask) {
        return boot(context, List.of(PACKAGE_LENOVO_MAIN), List.of(getIntent(PACKAGE_LENOVO_MAIN, PACKAGE_LENOVO_COMPONENT, newTask)), open);
    }

    private boolean bootMeizu(@NonNull Context context, boolean open, boolean newTask) {
        return boot(context, List.of(PACKAGE_MEIZU_MAIN), List.of(getIntent(PACKAGE_MEIZU_MAIN, PACKAGE_MEIZU_COMPONENT, newTask)), open);
    }

    private boolean bootHtc(@NonNull Context context, boolean open, boolean newTask) {
        return boot(context, List.of(PACKAGE_HTC_MAIN), List.of(getIntent(PACKAGE_HTC_MAIN, PACKAGE_HTC_COMPONENT, newTask)), open);
    }

    private boolean bootTranssion(@NonNull Context context, boolean open, boolean newTask) {
        return boot(context, List.of(PACKAGE_TRANSSION_MAIN), List.of(getIntent(PACKAGE_TRANSSION_MAIN, PACKAGE_TRANSSION_COMPONENT, newTask)), open);
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
     * Will attempt to open the AutoStart settings activity from the passed list of intents in order.
     * The first activity found will be opened.
     *
     * @param context Context
     * @param intents list of intents
     * @return true if an activity was opened, false otherwise
     */
    private boolean openBootScreen(@NonNull Context context, List<Intent> intents) {
        for (Intent intent : intents) {
            if (isActivityFound(context, intent)) {
                return startIntent(context, intent);
            }
        }
        return false;
    }

    /**
     * Will trigger the common autostart permission logic. If [open] is true it will attempt to open the specific
     * manufacturer setting screen, otherwise it will just check for its existence
     *
     * @param context   Context
     * @param packages, list of known package of the corresponding manufacturer
     * @param intents,  list of known intent that open the corresponding manufacturer settings screens
     * @param open,     if true it will attempt to open the settings screen, otherwise it just check its existence
     * @return true if the screen was opened or exists, false if it doesn't exist or could not be opened
     */
    private boolean boot(@NonNull Context context, List<String> packages, List<Intent> intents, boolean open) {
        for (String packageName : packages) {
            if (isPackageExists(context, packageName)) {
                if (open) {
                    return openBootScreen(context, intents);
                } else {
                    return areActivitiesFound(context, intents);
                }
            }
        }
        return false;
    }

    /**
     * Will trigger the common autostart permission logic. If [open] is true it will attempt to open the specific
     * manufacturer setting screen, otherwise it will just check for its existence
     *
     * @param context        Context
     * @param intentActions, list of known intent actions that open the corresponding manufacturer settings screens
     * @param open,          if true it will attempt to open the settings screen, otherwise it just check its existence
     * @return true if the screen was opened or exists, false if it doesn't exist or could not be opened
     */
    private boolean bootFromAction(@NonNull Context context, List<Intent> intentActions, boolean open) {
        if (open) {
            return openBootScreen(context, intentActions);
        } else {
            return areActivitiesFound(context, intentActions);
        }
    }

}
