package app.netfilter;

public interface IFilterVpnServiceListener {

    void onServiceStarted(FilterVpnService service);

    void onServiceStopped(FilterVpnService service);

    void onBeforeServiceStart(FilterVpnService service);

    void onVPNStarted(FilterVpnService service);

    void onVPNStopped(FilterVpnService service);

    void onVPNRevoked(FilterVpnService service);

    void onVPNEstablishError(FilterVpnService service);

    void onVPNEstablishException(FilterVpnService service, Exception e);

    void onOtherError(String error);

    void onProxyIsSet(FilterVpnService service);

    void saveStats(int[] clientsCounts, int[] netinfo, long[] policy);

}
