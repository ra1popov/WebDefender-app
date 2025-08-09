package app.ui.statlog;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import app.database.DatabaseHelper;
import app.dependencies.ApplicationDependencies;
import app.internal.Preferences;
import app.internal.Settings;
import app.preferences.model.Stat;
import app.util.Util;

public class StatlogHelper {

    private final Context context;
    private final DatabaseHelper databaseHelper;

    private final AtomicInteger blockedAdsIpCount = new AtomicInteger();
    private final AtomicInteger blockedAdsUrlCount = new AtomicInteger();
    private final AtomicInteger blockedApkCount = new AtomicInteger();
    private final AtomicInteger blockedMalwareSiteCount = new AtomicInteger();
    private final AtomicInteger blockedPaidSiteCount = new AtomicInteger();
    private final AtomicLong proxyCompressionSave = new AtomicLong();

    private final ConcurrentLinkedQueue<Statlog> blockedList = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean enabled = new AtomicBoolean(false);

    public StatlogHelper(@NonNull Context context) {
        this.context = context;
        this.databaseHelper = ApplicationDependencies.getDatabaseHelper();
        this.enabled.set(Preferences.getBoolean(Settings.PREF_STATLOG_ENABLED, false));

        Stat stat = ApplicationDependencies.getPreferences().getStat();
        blockedAdsIpCount.set(stat.blockedAdsIpCount);
        blockedAdsUrlCount.set(stat.blockedAdsUrlCount);
        blockedApkCount.set(stat.blockedApkCount);
        blockedMalwareSiteCount.set(stat.blockedMalwareSiteCount);
        blockedPaidSiteCount.set(stat.blockedPaidSiteCount);
        proxyCompressionSave.set(stat.proxyCompressionSave);
    }

    public void incrementBlockedAdsIpCount(String pkgName, String domain) {
        blockedAdsIpCount.incrementAndGet();
        if (enabled.get()) {
            blockedList.add(new Statlog(Statlog.TYPE_AD, pkgName, domain, System.currentTimeMillis()));
        }
    }

    public void incrementBlockedAdsUrlCount(String pkgName, String domain) {
        blockedAdsUrlCount.incrementAndGet();
        if (enabled.get()) {
            blockedList.add(new Statlog(Statlog.TYPE_AD, pkgName, domain, System.currentTimeMillis()));
        }
    }

    public void incrementBlockedApkCount(String pkgName, String domain) {
        blockedApkCount.incrementAndGet();
        if (enabled.get()) {
            blockedList.add(new Statlog(Statlog.TYPE_APK, pkgName, domain, System.currentTimeMillis()));
        }
    }

    public void incrementBlockedMalwareSiteCount(String pkgName, String domain) {
        blockedMalwareSiteCount.incrementAndGet();
        if (enabled.get()) {
            blockedList.add(new Statlog(Statlog.TYPE_MALWARE, pkgName, domain, System.currentTimeMillis()));
        }
    }

    public void incrementProxyCompressionSave(long contentLength) {
        proxyCompressionSave.addAndGet(contentLength);
    }

    public Stat getStat() {
        return ApplicationDependencies.getPreferences().getStat();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    @WorkerThread
    public void process() {
        Stat stat = new Stat(blockedAdsIpCount.get(),
                blockedAdsUrlCount.get(),
                blockedApkCount.get(),
                blockedMalwareSiteCount.get(),
                blockedPaidSiteCount.get(),
                proxyCompressionSave.get());

        ApplicationDependencies.getPreferences().setStat(stat);

        if (enabled.get()) {
            databaseHelper.batchWriteStatlog(Util.getAndClearList(blockedList));
        }
    }

}
