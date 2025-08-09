package app.application;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import app.util.BroadcastLiveData;

public class ApplicationLiveData extends BroadcastLiveData<List<ApplicationInfo>> {

    public ApplicationLiveData(@NonNull Context context) {
        super(context, getIntentFilter());
    }

    @Override
    protected List<ApplicationInfo> getContentProviderValue() {
        List<ApplicationInfo> list = new ArrayList<>();

        try {
            PackageManager packageManager = context.getPackageManager();
            List<android.content.pm.ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA); // get list of installed apps
            String myPackageName = context.getPackageName();

            for (android.content.pm.ApplicationInfo packageInfo : packages) {
                final String pkgName = packageInfo.packageName;
                if (pkgName == null || pkgName.indexOf('.') < 0 || Objects.equals(myPackageName, pkgName)) {
                    continue;
                }

                if (packageManager.checkPermission(Manifest.permission.INTERNET, pkgName) != PackageManager.PERMISSION_GRANTED) {
                    continue; // apps that don’t require internet access are not needed
                }

                Intent intent = packageManager.getLaunchIntentForPackage(pkgName);
                if (intent == null) {
                    continue; // apps that cannot be launched are not needed
                }

                String appName = null;
                try {
                    appName = packageManager.getApplicationLabel(packageInfo).toString();
                } catch (NullPointerException ignored) {
                }
                if (TextUtils.isEmpty(appName)) {
                    appName = pkgName;
                }

                list.add(new ApplicationInfo(pkgName, appName));
            }
        } catch (Exception ignored) {
        }

        list.sort(Comparator.comparing(appInfo -> appInfo.appName));

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