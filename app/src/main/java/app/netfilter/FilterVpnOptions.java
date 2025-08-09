package app.netfilter;

import android.content.Intent;

import app.internal.Settings;

public class FilterVpnOptions {

    public static final int DEFAULT_DNS_NO = 0;
    public static final int DEFAULT_DNS_ONLY = 1;
    public static final int DEFAULT_DNS_BEFORE = 2;
    public static final int DEFAULT_DNS_AFTER = 3;

    public String sessionName = null;
    public Intent configureIntent = null;
    public int mtu = 0;
    public String address = Settings.TUN_DEFAULT_IP;
    public int maskBits = 24;
    public boolean addDefaultRoute = true;
    public String[] dnsServers = null;
    public int useDefaultDNSServers = DEFAULT_DNS_AFTER;

    public FilterVpnOptions(String name) {
        sessionName = name;
    }

    public FilterVpnOptions(String name, String address, int maskBits, int mtu, Intent configIntent) {
        this.sessionName = name;
        this.address = address;
        this.maskBits = maskBits;
        this.mtu = mtu;
        this.configureIntent = configIntent;
    }

    public void setDNSServers(int useDefault, String... servers) {
        this.useDefaultDNSServers = useDefault;
        this.dnsServers = servers.clone();
    }

}
