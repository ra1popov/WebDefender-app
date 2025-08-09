package app.ui.statlog;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import app.util.BroadcastLiveData;

public class StatlogIconLiveData extends BroadcastLiveData<HashMap<String, StatlogIcon>> {

    public StatlogIconLiveData(@NonNull Context context) {
        super(context, getIntentFilter());
    }

    @Override
    protected HashMap<String, StatlogIcon> getContentProviderValue() {
        HashMap<String, StatlogIcon> list = new HashMap<>();

        try {
            PackageManager packageManager = context.getPackageManager();
            List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA); // get list of installed apps
            String myPackageName = context.getPackageName();

            for (ApplicationInfo packageInfo : packages) {
                final String pkgName = packageInfo.packageName;
                if (pkgName == null || pkgName.indexOf('.') < 0 || Objects.equals(myPackageName, pkgName)) {
                    continue;
                }

                String appName = null;
                try {
                    appName = packageManager.getApplicationLabel(packageInfo).toString();
                } catch (NullPointerException ignored) {
                }
                if (TextUtils.isEmpty(appName)) {
                    appName = pkgName;
                }

                Drawable icon = null;
                try {
                    icon = packageManager.getApplicationIcon(packageInfo);
                } catch (NullPointerException ignored) {
                }

                list.put(pkgName, new StatlogIcon(pkgName, appName, icon));
            }
        } catch (Exception ignored) {
        }

        return list;
    }

    private static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addDataScheme("package");
        return intentFilter;
    }

}
