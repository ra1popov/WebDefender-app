package app.application;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ApplicationUidManager {

    private final Context context;
    private final ConcurrentHashMap<Integer, String[]> allNames = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String[]> userNames = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    public ApplicationUidManager(Context context) {
        this.context = context;
    }

    public void hashAllUid() {

        ConcurrentHashMap<Integer, String[]> _allNames = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, String[]> _userNames = new ConcurrentHashMap<>();

        try {
            PackageManager packageManager = context.getPackageManager();
            final List<PackageInfo> packages = packageManager.getInstalledPackages(0); // get list of installed apps without meta
            String myPackageName = context.getPackageName();

            for (PackageInfo packageInfo : packages) {
                final String pkgName = packageInfo.packageName;
                if (Objects.equals(myPackageName, pkgName)) { // there's no need to check pkgName.indexOf('.') < 0 here, because it should be possible to exclude such system packages from filtering
                    continue;
                }

                int uid = packageManager.getApplicationInfo(pkgName, 0).uid;

                Intent intent = packageManager.getLaunchIntentForPackage(pkgName);
                if (intent != null) {
                    _userNames.compute(uid, (k, existingArray) -> {
                        if (existingArray == null) {
                            return new String[]{pkgName};
                        } else {
                            String[] updatedArray = Arrays.copyOf(existingArray, existingArray.length + 1);
                            updatedArray[existingArray.length] = pkgName;
                            return updatedArray;
                        }
                    });
                }

                _allNames.compute(uid, (k, existingArray) -> {
                    if (existingArray == null) {
                        return new String[]{pkgName};
                    } else {
                        String[] updatedArray = Arrays.copyOf(existingArray, existingArray.length + 1);
                        updatedArray[existingArray.length] = pkgName;
                        return updatedArray;
                    }
                });
            }
        } catch (Exception ignored) {
        }

        synchronized (lock) {
            this.allNames.clear();
            this.allNames.putAll(_allNames);

            this.userNames.clear();
            this.userNames.putAll(_userNames);
        }

    }

    @Nullable
    public String[] getNamesFromUid(int uid) {
        synchronized (lock) {
            return allNames.get(uid);
        }
    }

    @Nullable
    public String[] getUserNamesFromUid(int uid) {
        synchronized (lock) {
            return userNames.get(uid);
        }
    }

    @WorkerThread
    public String[] getAllSystemAppPackageNames() {
        Set<String> uniqueAllNames;
        Set<String> uniqueUserNames;

        synchronized (lock) {
            uniqueAllNames = allNames.values().stream()
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toSet());

            uniqueUserNames = userNames.values().stream()
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toSet());
        }

        return uniqueAllNames.stream().filter(str -> !uniqueUserNames.contains(str)).distinct().toArray(String[]::new);
    }

}
