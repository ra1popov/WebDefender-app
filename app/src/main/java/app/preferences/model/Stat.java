package app.preferences.model;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class Stat implements Serializable {

    public final int blockedAdsIpCount;
    public final int blockedAdsUrlCount;
    public final int blockedApkCount;
    public final int blockedMalwareSiteCount;
    public final int blockedPaidSiteCount;
    public final long proxyCompressionSave;

    public Stat() {
        this(0, 0, 0, 0, 0, 0);
    }

    public Stat(int blockedAdsIpCount, int blockedAdsUrlCount, int blockedApkCount, int blockedMalwareSiteCount, int blockedPaidSiteCount, long proxyCompressionSave) {
        this.blockedAdsIpCount = blockedAdsIpCount;
        this.blockedAdsUrlCount = blockedAdsUrlCount;
        this.blockedApkCount = blockedApkCount;
        this.blockedMalwareSiteCount = blockedMalwareSiteCount;
        this.blockedPaidSiteCount = blockedPaidSiteCount;
        this.proxyCompressionSave = proxyCompressionSave;
    }

}
