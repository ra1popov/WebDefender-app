package app.security;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import app.App;
import app.common.LibNative;
import app.common.Utils;
import app.common.WiFi;
import app.common.debug.L;
import app.common.memdata.MemoryBuffer;
import app.common.memdata.MemoryCache;
import app.dependencies.ApplicationDependencies;
import app.internal.Preferences;
import app.internal.ProxyBase;
import app.internal.Settings;
import app.netfilter.IFilterVpnPolicy;
import app.netfilter.dns.DNSAnswer;
import app.netfilter.dns.DNSRequest;
import app.netfilter.dns.DNSResponse;
import app.netfilter.http.RequestHeader;
import app.netfilter.http.ResponseHeader;
import app.netfilter.proxy.BlockIPCache;
import app.netfilter.proxy.Packet;
import app.netfilter.proxy.TCPStateMachine;
import app.scanner.LibScan;
import app.scanner.ScanResult;
import app.scanner.Scanner;
import app.ui.Toasts;

public class Policy implements IFilterVpnPolicy {

    private static final boolean CHECK_ON_DNS_REQUEST = false;
    private static final boolean DROP_BROWSERPROXY_CONNECTS = true;
    private static boolean DROP_BROWSERADS_ONLY = true;

    private static Scanner scanner;

    private static boolean changeUserAgentAndIp;
    private static boolean desktopUserAgent;
    private static boolean allowSomeAds;
    private static boolean blockMalicious;
    private static boolean internetOnlyForBrowsers;
    private static boolean internetOnlyForUserApps;
    private static boolean excludeSystemApps;
    private static boolean blockThirdPartyData;
    private static boolean blockAPKDownload;
    private static boolean blockSocialOther, blockSocialGPlus, blockSocialVK, blockSocialFB, blockSocialTwi, blockSocialOdn, blockSocialMailRu, blockSocialLinkedIn, blockSocialMoiKrug;

    private static boolean proxyUsed = true;           // changes on gui settings
    private static boolean proxyCompression = false;   // on gui
    private static boolean proxyAnonymize = false;       // on gui
    private static boolean proxyAnonymizeApps = false; // on gui
    private static int proxyFlags = 0;


    public Policy() {
        load();
    }

    private static void load() {

        // scanner
        if (scanner == null) {
            final boolean inited = updateScanner();

            if (Settings.DEBUG)
                L.d(Settings.TAG_POLICY, "Scanner inited = ", Boolean.toString(inited));
        }

        // update preferences
        reloadPrefs();

    }

    public static boolean updateScanner() {
        return updateScanner(Database.getCurrentVersion());
    }

    public static boolean updateScanner(String version) {
        if (Policy.scanner != null)
            Policy.scanner.clean();

        String dbPath;
        if (Settings.DEBUG_NO_SCAN)
            dbPath = "/";
        else
            dbPath = App.getContext().getFilesDir().getAbsolutePath() + "/" + version + "/";

        Policy.scanner = new Scanner(dbPath, Preferences.getAppVersion());

        final boolean inited = Settings.DEBUG_NO_SCAN || scanner.isInited();

        if (Settings.DEBUG) L.d(Settings.TAG_POLICY, "db is pro: " + scanner.isProVersion());

        // other actions

        // TODO XXX and if user use app with ssl ads? we miss!
        BlockIPCache.clear(); // domain to block may be in clean list

        return inited;
    }

    public static boolean updateScannerFast() {
        boolean result = false;

        if (Policy.scanner != null)
            result = Policy.scanner.reloadFast();

        BlockIPCache.clear(); // see updateScanner

        return result;
    }

    public static void reloadPrefs() {
        blockMalicious = Preferences.get(Settings.PREF_BLOCK_MALICIOUS);
        allowSomeAds = false; //Preferences.get(Settings.PREF_ALLOW_SOME_ADS);

        blockSocialOther = Preferences.get(Settings.PREF_SOCIAL_OTHER);
        blockSocialGPlus = Preferences.get(Settings.PREF_SOCIAL_GPLUS);
        blockSocialVK = Preferences.get(Settings.PREF_SOCIAL_VK);
        blockSocialFB = Preferences.get(Settings.PREF_SOCIAL_FB);
        blockSocialTwi = Preferences.get(Settings.PREF_SOCIAL_TWITTER);
        blockSocialOdn = Preferences.get(Settings.PREF_SOCIAL_OK);
        blockSocialMailRu = Preferences.get(Settings.PREF_SOCIAL_MAILRU);
        blockSocialLinkedIn = Preferences.get(Settings.PREF_SOCIAL_LINKEDIN);
        blockSocialMoiKrug = Preferences.get(Settings.PREF_SOCIAL_MOIKRUG);

        blockThirdPartyData = Preferences.get(Settings.PREF_BLOCK_TP_CONTENT);
        blockAPKDownload = Preferences.get(Settings.PREF_BLOCK_APKS);
        changeUserAgentAndIp = Preferences.get(Settings.PREF_CHANGE_USERAGENT);
        desktopUserAgent = Preferences.get(Settings.PREF_DESKTOP_USERAGENT);

        proxyCompression = Preferences.get(Settings.PREF_USE_COMPRESSION);
        proxyAnonymize = Preferences.get(Settings.PREF_ANONYMIZE);
        proxyAnonymizeApps = !Preferences.get(Settings.PREF_ANONYMIZE_ONLY_BRW);
        internetOnlyForBrowsers = Preferences.get(Settings.PREF_BLOCK_APPS_DATA);
        internetOnlyForUserApps = Preferences.get(Settings.PREF_BLOCK_SYSTEM_APPS_DATA);
        excludeSystemApps = Preferences.get(Settings.PREF_EXCLUDE_SYSTEM_APPS_DATA);

        proxyFlags = 0;
        if (proxyCompression) proxyFlags |= 1;
        if (proxyAnonymize) proxyFlags |= 2;

        proxyUsed = (proxyCompression || proxyAnonymize);
        if (proxyUsed) {
            String country = Preferences.get_s(Settings.PREF_PROXY_COUNTRY);
            ProxyBase.setCurrentCountry(country);
        }

        String token = getUserToken(true);
        if (token != null) {
            DROP_BROWSERADS_ONLY = !Preferences.get(Settings.PREF_APP_ADBLOCK);
        }
    }

    public static void refreshToken(boolean force) {

    }

    public static void clearToken() {
        Preferences.clearToken();
    }

    // see getScanDataBufferSize
    //
    @Override
    public PolicyRules getPolicyForData(RequestHeader request, ByteBuffer buf) {
        return new PolicyRules();
    }

    // check if need to use proxy on connection with such params
    public boolean isProxyUse(byte[] serverIp, int serverPort, int uid, boolean isBrowser) {
        if (!(proxyUsed && (isBrowser || proxyAnonymizeApps))) // proxy disabled, not browser and proxify apps disabled
            return false;

        if (uid == App.getMyUid()) // skip webdefender connects
            return false;

        if (serverIp != null) {
            // check for local connects (not use proxy)

            if (Utils.ip4Cmp(serverIp, Settings.LOOPBACK_IP_AR)) // 127.0.0.1
                return false;

            int b0 = Utils.unsignedByte(serverIp[0]);
            int b1 = Utils.unsignedByte(serverIp[1]);

            // 10.0.0.0 — 10.255.255.255
            // 192.168.0.0 — 192.168.255.255
            // 172.16.0.0 — 172.31.255.255
            if (b0 == 10 || (b0 == 192 && b1 == 168) || (b0 == 172 && b1 >= 16 && b1 <= 31))
                return false;
        }

        // check token and active proxy server
        String token = getUserToken(true);
        return (token != null && ProxyBase.getCurrentServer() != null);
    }

    public ProxyBase.ProxyServer getProxyHost() {
        if (Settings.DEBUG_LOCAL_WGPROXY)
            return ProxyBase.getLocalServer();
        else
            return ProxyBase.getCurrentServer();
    }

    public int getProxyPort() {
        return Settings.WGPROXY_PORT;
    }

    public boolean isProxyCryptUse() {
        return Settings.WGPROXY_CRYPT_USE;
    }

    public static int getProxyFlags() {
        return proxyFlags;
    }

    /*
     * TODO XXX The Content-Type header can come as "text/html; charset=utf-8", this method will only extract the first part
     *
     * @param type - the incoming type from the header
     * @return - the cleaned type
     *
     */
    private static String getContentType(String type, boolean addSemicolon) {
        if (type == null)
            return type;

        int pos = type.indexOf(';');
        if (pos > 0)
            return type.substring(0, ((addSemicolon) ? pos + 1 : pos)).toLowerCase();
        else if (pos == 0)
            return null;
        else
            return ((addSemicolon) ? type + ';' : type);
    }

    // use getContentType(type, true) before
    private static boolean isScannableContentType(String type) {
        if (type == null) {
            return false;
        }

        return ("text/plain;text/html;text/xml;text/css;application/xml;application/xhtml+xml;application/rss+xml;" +
                "text/javascript;text/x-javascript;application/javascript;application/x-javascript;" +
                "text/ecmascript;text/x-ecmascript;application/ecmascript;application/x-ecmascript;" +
                "text/javascript1.0;text/javascript1.1;text/javascript1.2;text/javascript1.3;" +
                "text/javascript1.4;text/javascript1.5;text/jscript;text/livescript;").contains(type);
    }

    // use getContentType(type, true) before
    private static boolean isRunnableContentType(String type) {
        if (type == null) {
            return false;
        }

        return "application/vnd.android.package-archive;".contains(type);
    }

    // use getContentType(type, true) before
    private static boolean isImageContentType(String type, boolean proxySupport) {
        if (type == null)
            return false;

        if (proxySupport) {
            return "image/jpeg;image/png;".contains(type);
        } else {
            return "image/jpeg;image/pjpeg;image/png;image/gif;image/bmp;image/svg+xml;image/tiff;".contains(type);
        }
    }

    // use getContentType(type, true) before
    private static boolean isCompressedEncoding(String encoding) {
        if (encoding == null) {
            return false;
        }

        return "gzip;bzip2;deflate;".contains(encoding);
    }

    public void reload() {
        L.a(Settings.TAG_POLICY, "Reloading...");
        load();
    }

    public void save() {

    }

    /**
     * Check policy for Packet before other work in ProxyWorker
     *
     * @param packet - Packet that is coming from TUN
     * @return PolicyRules for that packet
     */
    public PolicyRules getPolicy(Packet packet) {
        return (new PolicyRules());
    }

    /**
     * Check policy for packet before other work in ProxyWorker (before create TCPClient)
     *
     * @param packet - Packet that is coming from TUN
     * @param uid    - found uid for a process with that packet (read from /proc or kernel)
     * @return PolicyRules for that packet
     */
    public PolicyRules getPolicy(Packet packet, int uid) {
        boolean proxyTest = false;
        boolean tunTest = false;

        if (packet.dstPort == 80) {
            proxyTest = Utils.ip4Cmp(Settings.TEST_LOCAL_PROXY_IP_AR, packet.dstIp);
            tunTest = Utils.ip4Cmp(Settings.TEST_TUN_WORK_IP_AR, packet.dstIp);
        }

        if (!proxyTest && !tunTest) {
            // normal connection

            boolean isBrowser = Browsers.isBrowser(uid);

            // internet only for browsers?
            // didn't block APPS connects if no token or we block google play subscriptions
            if (internetOnlyForBrowsers && !isBrowser && uid != App.getMyUid() && getUserToken(false) != null) {
                return (new PolicyRules(PolicyRules.DROP));
            }

            // internet only for user apps?
            if (internetOnlyForUserApps && !Firewall.appIsUser(uid) && uid != App.getMyUid() && getUserToken(false) != null) {
                return (new PolicyRules(PolicyRules.DROP));
            }

            // firewall block?
            // TODO XXX connections to localhost?!!!
            if (!internetOnlyForBrowsers && !Firewall.appIsAllowed(uid)) {
                return (new PolicyRules(PolicyRules.DROP));
            }

            final PolicyRules res = getPolicy(packet.dstIp, packet.dstPort, uid);
            return res;
        }

        // WG internal test connections

        if (proxyTest) {
            // proxy detect packet (android bug with proxy app use before VPN)

            if (App.getMyUid() == uid) {
                Preferences.putBoolean(Settings.PREF_PROXY_DEL_TRY, false);
            } else {
                boolean disabledAlready = Preferences.get(Settings.PREF_PROXY_DEL_TRY); // try to del wifi proxy?
                if (Settings.DEBUG)
                    L.e(Settings.TAG_POLICY, "<PXY> ", "Found uid = ", Integer.toString(uid), " my uid = ", Integer.toString(App.getMyUid()));

                if (!disabledAlready) {
                    Preferences.putBoolean(Settings.PREF_PROXY_DEL_TRY, true);

                    WiFi.unsetWifiProxySettings(App.getContext()); // network will be reconnected
                } else {
                    //Preferences.putBoolean(Settings.PREF_PROXY_DEL_TRY, false);

                    App.disable();

                    Context c = App.getContext();
                    PackageManager pm = c.getPackageManager();

                    String[] packages = Processes.getNamesFromUid(uid);
                    String pkgName = null;
                    if (packages != null && packages.length > 0) {
                        try {
                            ApplicationInfo info = pm.getApplicationInfo(packages[0], 0);
                            //pkgName = pm.getApplicationLabel(info);
                            pkgName = info.name;

                            if (Settings.EVENTS_LOG) {

                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();

                            //message = "";
                            pkgName = packages[0];
                        }

                    }

                    Toasts.showProxyDeleteError(pkgName);
                }
            }
        } else {
            // tun work test

            int[] netClientsCounts = new int[2];
            netClientsCounts[0] = 1; // tcp
            netClientsCounts[1] = 0; // udp

        }

        return (new PolicyRules(PolicyRules.DROP));
    }

    public PolicyRules getPolicy(byte[] servIp, int servPort, int uid) {
        boolean isBrowser = Browsers.isBrowser(uid);

        // TODO XXX check only first name now
        String[] packageNames = Processes.getNamesFromUid(uid);
        String pkgname = null;
        if (packageNames != null && packageNames.length > 0) {
            pkgname = packageNames[0];
        }


        //
        if (Settings.DEBUG_ALLOW_APP) {
            if (!Settings.DEBUG_ALLOW_APP_NAME.equals(pkgname) && App.getMyUid() != uid)
                return (new PolicyRules(PolicyRules.DROP));
        }
        if (Settings.DEBUG_DROP_WG) {
            if (App.getMyUid() == uid)
                return (new PolicyRules(PolicyRules.DROP));
        }

        return getPolicy(servIp, servPort, isBrowser, pkgname);
    }

    /*
     * TODO XXX use one pkgname but for one uid can be several packages (browser plugins)
     */
    public PolicyRules getPolicy(byte[] servIp, int servPort, boolean isBrowser, String pkgname) {
        if (!Settings.APPS_EXCLUDE_BROWSERS) {
            // don't block by ip if no separate ad blocking and apps blocking disabled
            // TODO XXX check ip verdict and drop only ad block
            if (DROP_BROWSERADS_ONLY)
                return (new PolicyRules());
        }

//		  if (proxyUsed && !ProxyBase.isReady() && servPort != 53)
//			  return new PolicyRules(PolicyRules.DROP);

        // block ads only in browsers? (with this check browsers may show ssl ads!)
        if (!isBrowser && DROP_BROWSERADS_ONLY)
            return (new PolicyRules());
//		  else if ("adbd".equals(pkgname)) return (new PolicyRules()); // PROFILE

        // android browser bug workaround (on some sites back command not work if block ip connects)
        // also need to disable DNS_NO_ADS_IP
//		  else if (isBrowser && "com.android.browser".equals(pkgname)) // && isSamsung
//			  return (new PolicyRules());

        // This is for ads blocking with hosts, to drop connection before trying
        if (Settings.LOOPBACK_DROP_CONNECTS) {
            if (Utils.ip4Cmp(servIp, Settings.LOOPBACK_IP_AR)) {
                return (new PolicyRules(PolicyRules.DROP));
            }
        }

        PolicyRules res = new PolicyRules();

        PolicyRules tmp = BlockIPCache.getPolicy(servIp);

        if (tmp != null) {
            //boolean compressed = tmp.hasPolicy(PolicyRules.COMPRESSED);
            int recordType = tmp.getVerdict();

            if (DROP_BROWSERPROXY_CONNECTS && servPort == 443 && tmp.hasPolicy(PolicyRules.COMPRESSED)) {
                tmp.addPolicy(PolicyRules.DROP);
            }

            if (isBrowser) {
                // do not block browser access to ip with normal or third party ads
                if (!((allowSomeAds &&
                        recordType == LibScan.RECORD_TYPE_ADS_OK) || recordType == LibScan.RECORD_TYPE_ADS_TPARTY)) {
                    res = tmp;
                }
            } else {
                // block not browsers access to any ip
                // TODO XXX if ADS_OK or NOTIFY???
                res = tmp;
            }

            // stats
            if (res.hasPolicy(PolicyRules.DROP)) {
                String domain = BlockIPCache.getDomain(servIp);
                if (domain == null) {
                    domain = Utils.ipToString(servIp, 0).replace(":0", "");
                }

                if (LibScan.recordTypeIsAds(recordType)) {
                    ApplicationDependencies.getStatlogHelper().incrementBlockedAdsIpCount(pkgname, domain);
                } else if (LibScan.recordTypeIsDangerous(recordType)) {
                    ApplicationDependencies.getStatlogHelper().incrementBlockedMalwareSiteCount(pkgname, domain);
                }
            }
        }

        if (Settings.DEBUG_POLICY)
            L.a(Settings.TAG_POLICY, "<IPB> ", Utils.ipToString(servIp, servPort) + " -> " + res.toString());

        return res;
    }

    public PolicyRules getPolicy(int uid) {
        return new PolicyRules();
    }

    /*
     * check policy for request
     *
     * order:
     * - get policy for url + referer and if have policy (!= NORMAL) return it
     * - process 'paranoid' private mode
     * - check for .apk request
     *
     * TODO XXX use one pkgname but for one uid can be several packages (browser plugins)
     * TODO XXX add cache for last request
     */
    public PolicyRules getPolicy(RequestHeader request, int uid, String pkgname, boolean isBrowser, byte[] servIp) {
        PolicyRules rules = new PolicyRules();

        if (!request.isHttp()) {
            return rules;
        } else if (request.isPartial()) {
            rules.addPolicy(PolicyRules.WAIT);
            return rules;
        }

        String url = request.getUrl();

        if (Settings.DEBUG_YOUTUBE) {
            if ((pkgname != null && LibNative.asciiToLower(pkgname).contains("youtube")) || url.contains(".googlevideo.com/")) {
                L.a(Settings.TAG_POLICY, "<YouTube> ", url);
            }
        }

        do {
            // check properties
            if (url == null)
                break;

            // for applications domain and referer always not equal
            boolean isSameDomain = false;
            final String referer_http = request.referer;
            String referer = null;
            if (isBrowser) {
                if (referer_http == null || referer_http.isEmpty()) {
                    isSameDomain = true;
                    // some records need referrer! see 1700001
                    // TODO XXX if no host also?
                    if (request.host != null && !request.host.isEmpty())
                        referer = Utils.getMainDomain(request.host);
                } else {
                    referer = Utils.getMainDomain(Utils.getDomain(referer_http));
                    isSameDomain = request.isSameDomain(referer);
                }
            } else {
                referer = pkgname;
            }

            String name = request.getFilename();
            boolean isApk = (name != null && LibNative.asciiEndsWith(".apk", LibNative.asciiToLower(name))); // TODO XXX test with russian apk name
            boolean urlBlocked = false;

            //L.d(Settings.TAG_POLICY, "referrer: ", request.referer, " isSameDomain: ", Boolean.toString(isSameDomain) + " uid: " + uid);

            // scan if browser or block any apps ads
            if (isBrowser || !DROP_BROWSERADS_ONLY) {
                //
                PolicyRules rules0 = getPolicy(url, referer, isSameDomain, referer_http);
                int recordType = rules0.getVerdict();

                if (!Settings.APPS_EXCLUDE_BROWSERS) {
                    // reset ad block in browsers if no separate ad blocking and apps blocking disabled
                    if (DROP_BROWSERADS_ONLY && LibScan.recordTypeIsAds(recordType))
                        rules0 = new PolicyRules();
                }

                if (rules0.hasPolicy(PolicyRules.DROP))
                    urlBlocked = true;

                //L.d(Settings.TAG_POLICY, "rules: ", rules.toString());

                if (rules0.hasPolicy(PolicyRules.NOTIFY_SERVER)) {

                }

                if (!isBrowser && urlBlocked && LibScan.recordTypeIsSocial(recordType))
                    // don't block social networks in applications
                    // TODO XXX exclude official applications in db instead of this
                    rules0 = null;

                if (isBrowser && ((blockThirdPartyData && !isSameDomain) || (isApk && blockAPKDownload))) {
                    // block all requests to other sites + block apk from browsers
                    rules0 = new PolicyRules(PolicyRules.DROP);
                }

                if (rules0 != null) {
                    rules = rules0;

                    String domain = Utils.getDomain(url);
                    if (TextUtils.isEmpty(domain)) {
                        domain = Utils.ipToString(servIp, 0).replace(":0", "");
                    }

                    // stats
                    if (rules.hasPolicy(PolicyRules.DROP)) {
                        if (isApk) {
                            ApplicationDependencies.getStatlogHelper().incrementBlockedApkCount(pkgname, domain);
                        } else if (LibScan.recordTypeIsAds(recordType)) {
                            ApplicationDependencies.getStatlogHelper().incrementBlockedAdsUrlCount(pkgname, domain);
                        } else if (LibScan.recordTypeIsDangerous(recordType)) {
                            ApplicationDependencies.getStatlogHelper().incrementBlockedMalwareSiteCount(pkgname, domain);
                        }
                    }
                }
            }

            // apk statistics
            // statistics on AV blocked downloads or from exceptions not needed
            if (isApk && !urlBlocked && !Exceptions.exceptFromStats(url)) {

            }
        }
        while (false);

        if (Settings.DEBUG_POLICY)
            L.a(Settings.TAG_POLICY, "<REQ> ", "'" + url + "' -> " + rules.toString());

        return rules;
    }

    // return minimal buffer data size before send data to client
    @Override
    public int getScanDataBufferSize(RequestHeader requestHeader, ResponseHeader responseHeader) {
        //if (responseHeader.responseCode == 200 && LibNative.asciiIndexOf(".googlevideo.com/videoplayback?", requestHeader.getUrl()) >= 0)
        //	  return 17000;
        //else
        return 0;
    }

    @Override
    public boolean isBrowser(String[] packs) {
        return Browsers.isBrowser(packs);
    }

    @Override
    public boolean isBrowser(int uid) {
        return Browsers.isBrowser(uid);
    }

    @Override
    public void addRequestHeaders(RequestHeader header) {
    }

    @Override
    public boolean changeUserAgent() {
        return (changeUserAgentAndIp || desktopUserAgent);
    }

    @Override
    public boolean needToAddHeaders() {
        return (changeUserAgentAndIp || desktopUserAgent);
    }

    @Override
    public boolean changeReferer() {
        //return changeUserAgentAndIp;
        return false;
    }

    @Override
    public void scan(MemoryBuffer buffer, TCPStateMachine tcpStateMachine) {
        //Scanner.scan(buffer, tcpStateMachine);
        MemoryCache.release(buffer); // TODO XXX move from here
    }

    @Override
    public String getUserAgent() {
        String ua = null;

        if (desktopUserAgent && !changeUserAgentAndIp)
            ua = UserAgents.getAgentDesktop();
        else if (changeUserAgentAndIp)
            ua = UserAgents.getAgent(desktopUserAgent);

        return ua;
    }

    /*
     * Analyzes the request and response upon receiving a server reply
     *
     * @param request   - the server request, parsed into necessary fields
     * @param response  - the server response, parsed by headers
     * @param data      - the full server response, which may contain only headers or even a part of a file
     * @param isBrowser - whether the client is a browser or not
     * @return          - returns the policy for further actions with this connection
     */
    public PolicyRules getPolicy(RequestHeader request, ResponseHeader response, byte[] data,
                                 int uid, boolean isBrowser, boolean isProxyUsed, String pkgName) {
        PolicyRules res = new PolicyRules();

        if (request == null || response == null)
            return res;

        if (response.responseCode != 200)
            return res;

        // check for android application
        final String type = getContentType(response.contentType, true);
        final String name = response.fileName;
        final String encoding = getContentType(response.contentEncoding, true);

        // check for archive
        boolean isArch = false;
        boolean isExe = false;
        boolean isApk = (isRunnableContentType(type) ||
                (name != null && LibNative.asciiEndsWith(".apk", LibNative.asciiToLower(name))));

        if (!isApk && !isCompressedEncoding(encoding) && !isScannableContentType(type)) {
            int binaryType = LibScan.binaryDataSearchType(data, Math.min(1024, data.length));
            if (LibScan.binaryTypeIsArchive(binaryType) || LibScan.binaryTypeIsExecutable(binaryType)) {
                //L.i(Settings.TAG_POLICY, "<RES> ", LibScan.binaryTypeToString(binaryType) + " " + request.getUrl() + " " + name);

                //if (binaryType != LibScan.BINARY_TYPE_GZIP && binaryType != LibScan.BINARY_TYPE_ZIP)
                //if (binaryType != LibScan.BINARY_TYPE_GZIP && binaryType != LibScan.BINARY_TYPE_ZIP &&
                //	  binaryType != LibScan.BINARY_TYPE_BZIP2)
                isArch = true;

                if (LibScan.binaryTypeIsExecutable(binaryType))
                    isExe = true;
            }

            // fake counter of traffic saved by proxy
            if (isProxyUsed && proxyCompression && isImageContentType(type, true)) {
                ApplicationDependencies.getStatlogHelper().incrementProxyCompressionSave(response.contentLength);
            }
        }

        // statistics + block apk
        final String url = request.getUrl();
        if (url != null) {
            if (isApk || isArch) {
                if (!Exceptions.exceptFromStats(url)) {

                }

                if (isBrowser && isApk && blockAPKDownload) {
                    res = new PolicyRules(PolicyRules.DROP);
                    ApplicationDependencies.getStatlogHelper().incrementBlockedApkCount(pkgName, url);
                }
            }
        }

        if (Settings.DEBUG_POLICY)
            L.a(Settings.TAG_POLICY, "<RES> ", "'" + url + "' -> " + res.toString());

        return res;
    }

    /*
     * get domain policy on DNS response, return policy and verdict
     *
     * check this function with resolving domains:
     *
     * gld.push.samsungosp.com -> alias to many lb-gld-777274664.eu-west-1.elb.amazonaws.com ip
     * cdn-tags.brainient.com -> alias to cdn-tags-brainient.global.ssl.fastly.net -> alias to global-ssl.fastly.net ->
     *	   alias to fallback.global-ssl.fastly.net ip
     *
     * TODO XXX add ip scan
     * TODO XXX incorrect work with several domains mixed with aliases
     * TODO XXX add cache for last request
     */
    public PolicyRules getPolicy(DNSResponse resp) {
        ArrayList<DNSAnswer> answers = resp.getAnswers();
        if (answers == null || answers.isEmpty()) {
            return null;
        }

        String last = "";
        String main = null;
        String alias = null;
        PolicyRules rules = null;

        //L.a(Settings.TAG_POLICY, "domain answers");
        boolean isBlocked = false;

        for (DNSAnswer answer : answers) {
            if (answer == null || answer.domain == null) {
                continue;
            }

            final String domain = answer.domain;
            final String cname = answer.cname;
            final byte[] ip = answer.ip;

            if (Settings.DEBUG_POLICY) {
                L.a(Settings.TAG_POLICY, "<DIP> ", "'" + answer.domain + "' (" + main + ") " + Utils.ipToString(answer.ip, 0) + " " + Integer.toString(answer.ttl));
            }

            if (domain.equals(last)) {
                // skip same domain scan

                if (ip == null) {
                    continue;
                } else if (rules != null && rules.verdict != LibScan.RECORD_TYPE_WHITE) {
                    BlockIPCache.addIp(main, ip, answer.ttl, rules);
                    isBlocked = true;
                } else {
                    BlockIPCache.addCleanIp(ip);
                }

                continue;
            }

            last = domain;
            if (!domain.equals(alias)) {
                // domain switch (new domain, not alias to previous domains)
                main = domain;
                rules = null;
                alias = null;
                //L.a(Settings.TAG_POLICY, "domain switch");
            }
            if (cname != null) {
                alias = cname;
            }

            // scan

            PolicyRules this_rules = null;
            if (rules != null) {
                this_rules = rules; // skip scan if upper domain detected (even if TPARTY ads and not scan for WHITE)
            } else {
                this_rules = getPolicy(domain);
            }

            if (this_rules.hasPolicy(PolicyRules.DROP) || this_rules.hasPolicy(PolicyRules.COMPRESSED)) {
                rules = this_rules;
                if (ip != null) {
                    BlockIPCache.addIp(main, ip, answer.ttl, rules);
                    isBlocked = true;
                }
            } else {
                if (this_rules.verdict == LibScan.RECORD_TYPE_WHITE) {
                    rules = this_rules;
                }
                if (ip != null) {
                    BlockIPCache.addCleanIp(ip);
                }
            }

            //L.i(Settings.TAG_POLICY, "<DnsRes> ", answer.domain);
        }

        if (isBlocked) {
            if (rules != null) {
                if (LibScan.recordTypeIsAds(rules.verdict) && rules.verdict != LibScan.RECORD_TYPE_ADS_TPARTY) {
                    ApplicationDependencies.getStatlogHelper().incrementBlockedAdsIpCount(Settings.APP_PACKAGE, last);
                } else if (LibScan.recordTypeIsDangerous(rules.verdict)) {
                    ApplicationDependencies.getStatlogHelper().incrementBlockedMalwareSiteCount(Settings.APP_PACKAGE, last);
                }
            }
        }

        if (Settings.DNS_NO_ADS_IP) {

            if (!DROP_BROWSERADS_ONLY && rules != null &&
                    (rules.verdict == LibScan.RECORD_TYPE_ADS ||
                            rules.verdict == LibScan.RECORD_TYPE_FRAUD ||
                            rules.verdict == LibScan.RECORD_TYPE_MALWARE)) {

                // return rules to drop answers from dns response
                return rules;
            }
        }

        return null;
    }

    /*
     * get domain policy on DNS request, return policy and verdict
     * on dns request block only ads (without third party and ok domains) or
     *	   dangerous (fraud/malware) domains
     *
     * return policy NORMAL and type CLEAN on normal domains
     *
     * TODO XXX what to do on different domains request ?!
     * TODO XXX request to compression proxy
     */
    public PolicyRules getPolicy(DNSRequest req) {
        if (!CHECK_ON_DNS_REQUEST)
            return (new PolicyRules());

        ScanResult sc = new ScanResult();

        if (req.domains != null && req.flags == 0x0100) {
            int i = -1;
            for (String domain : req.domains) {
                i++;
                if (domain == null)
                    continue;

                if (i > 0 && req.domains[i - 1] != null && req.domains[i - 1].equals(domain))
                    continue;

                int recordType = Policy.scanner.scanDomain(domain);
                sc.addType(recordType);
            }
        }

        // convert scan result + preferences to policy
        int policy = PolicyRules.NORMAL;
        int recordType = LibScan.RECORD_TYPE_CLEAN;

        if (!sc.hasType(LibScan.RECORD_TYPE_WHITE)) {
            if (sc.hasAds()) {
                if (sc.hasType(LibScan.RECORD_TYPE_ADS_OK) && !allowSomeAds) {
                    policy = PolicyRules.DROP;
                    recordType = LibScan.RECORD_TYPE_ADS_OK;
                } else {
                    if (sc.hasType(LibScan.RECORD_TYPE_ADS)) {
                        policy = PolicyRules.DROP;
                        recordType = LibScan.RECORD_TYPE_ADS;
                    }
                }
            }

            if (blockMalicious && policy == PolicyRules.NORMAL && sc.hasDangerous()) {
                policy = PolicyRules.DROP | PolicyRules.NOTIFY_USER;
                recordType = sc.getMajorType(); // XXX
            }
        }

        PolicyRules rules = new PolicyRules(policy, recordType);

        if (Settings.DEBUG_POLICY) {
            String tmp = (req.domains != null) ? Arrays.toString(req.domains) : "";
            L.a(Settings.TAG_POLICY, "<DNS> ", tmp + " -> " + sc + " " + rules);
        }

        return rules;
    }

    /*
     * get domain policy, return policy and verdict
     *
     * copy/paste from getPolicy(DNSRequest req)
     * see getPolicy(DNSResponse resp)
     */
    public PolicyRules getPolicy(String domain) {
        ScanResult sc = new ScanResult();
        int recordType = Policy.scanner.scanDomain(domain);
        sc.addType(recordType);

        // convert scan result + preferences to policy
        int policy = PolicyRules.NORMAL;
        recordType = LibScan.RECORD_TYPE_CLEAN;

        if (!sc.hasType(LibScan.RECORD_TYPE_WHITE)) {
            if (sc.hasAds()) {
                policy = PolicyRules.DROP;
                recordType = sc.getMajorType();
            } else if (blockMalicious && sc.hasDangerous()) {
                policy = PolicyRules.DROP | PolicyRules.NOTIFY_USER;
                recordType = sc.getMajorType();
            }
        }

        PolicyRules rules = new PolicyRules(policy, recordType);

        if (Exceptions.isCompressed(domain)) { /*"proxy.googlezip.net".equals(domain) || "compress.googlezip.net".equals(domain)*/
            rules.addPolicy(PolicyRules.COMPRESSED);
        }

        if (Settings.DEBUG_POLICY)
            L.a(Settings.TAG_POLICY, "<DNM> ", domain + " -> " + sc + " " + rules);

        return rules;
    }

    /*
     * get url policy on HTTP request
     *
     * return policy NORMAL and type CLEAN on normal urls
     *
     * NOTE pass as referer main domain (if request from browser) or app package name
     *
     * TODO add social networks settings check
     * TODO add server notify on 'regexp' records detect
     */
    public PolicyRules getPolicy(String url, String referer, boolean isSameDomain, String referer_http) {
        //L.e(Settings.TAG_POLICY, "<URL> ", "Url: " + url + "\t" + isSameDomain);

        ScanResult sc = Policy.scanner.scanUrl(url, referer);

        // convert scan result + preferences to policy

        int policy = PolicyRules.NORMAL;
        int recordType = LibScan.RECORD_TYPE_CLEAN;
        String redirect = null;

        boolean haveTparty = sc.hasType(LibScan.RECORD_TYPE_ADS_TPARTY);
        boolean tparty = (isSameDomain && haveTparty);

        while (!sc.hasType(LibScan.RECORD_TYPE_WHITE)) {
            // first check for silent verdicts (ADS, SOCIAL) then for verdicts with notification

            if (!tparty && sc.hasAds() &&
                    (!allowSomeAds || (allowSomeAds && !sc.hasType(LibScan.RECORD_TYPE_ADS_OK)))) {
                // ads (but allow to open urls with ADS_TPARTY detect in new tab)

                policy = PolicyRules.DROP;
                recordType = LibScan.RECORD_TYPE_ADS;
                if (haveTparty)
                    redirect = url; // see PolicyRules

                break;
            }

            if (!isSameDomain && sc.hasSocial()) {
                // social (but allow to open urls in new tab)

                if (blockSocialOther && sc.hasType(LibScan.RECORD_TYPE_SOCIAL_OTHER))
                    recordType = LibScan.RECORD_TYPE_SOCIAL_OTHER;
                else if (blockSocialGPlus && sc.hasType(LibScan.RECORD_TYPE_SOCIAL_GPLUS))
                    recordType = LibScan.RECORD_TYPE_SOCIAL_GPLUS;
                else if (blockSocialVK && sc.hasType(LibScan.RECORD_TYPE_SOCIAL_VK))
                    recordType = LibScan.RECORD_TYPE_SOCIAL_VK;
                else if (blockSocialFB && sc.hasType(LibScan.RECORD_TYPE_SOCIAL_FB))
                    recordType = LibScan.RECORD_TYPE_SOCIAL_FB;
                else if (blockSocialTwi && sc.hasType(LibScan.RECORD_TYPE_SOCIAL_TWI))
                    recordType = LibScan.RECORD_TYPE_SOCIAL_TWI;
                else if (blockSocialOdn && sc.hasType(LibScan.RECORD_TYPE_SOCIAL_ODNKLASS))
                    recordType = LibScan.RECORD_TYPE_SOCIAL_ODNKLASS;
                else if (blockSocialMailRu && sc.hasType(LibScan.RECORD_TYPE_SOCIAL_MAILRU))
                    recordType = LibScan.RECORD_TYPE_SOCIAL_MAILRU;
                else if (blockSocialLinkedIn && sc.hasType(LibScan.RECORD_TYPE_SOCIAL_LINKEDIN))
                    recordType = LibScan.RECORD_TYPE_SOCIAL_LINKEDIN;
                else if (blockSocialMoiKrug && sc.hasType(LibScan.RECORD_TYPE_SOCIAL_MOIKRUG))
                    recordType = LibScan.RECORD_TYPE_SOCIAL_MOIKRUG;

                if (LibScan.recordTypeIsSocial(recordType)) {
                    policy = PolicyRules.DROP;
                    redirect = url; // see PolicyRules
                }

                break;
            }

            if (blockMalicious && sc.hasDangerous()) {
                // malware

                policy = PolicyRules.DROP | PolicyRules.NOTIFY_USER;
                recordType = sc.getMajorType(); // WHITE > MALWARE > FRAUD > all other

                break;
            }

            if (sc.hasType(LibScan.RECORD_TYPE_CHARGEABLE) &&
                    (!isSameDomain || referer_http == null || referer_http.isEmpty())) {
                // chargeable (skip notification if user already on site)

                policy = PolicyRules.NOTIFY_USER;
                recordType = LibScan.RECORD_TYPE_CHARGEABLE;

                break;
            }

            break;
        } // while

        if (sc.hasType(LibScan.RECORD_TYPE_TEST))
            policy |= PolicyRules.NOTIFY_SERVER;

        // +signature

        PolicyRules rules = new PolicyRules(policy, recordType, sc.getIds(), redirect);

        if (Settings.DEBUG_POLICY) {
            // TODO XXX check proguard cutoff (false || false)
            String tmp = (referer != null) ? "'" + referer + "'" : "''";
            tmp += (referer_http != null) ? " '" + referer_http + "'" : " ''";
            tmp += " (" + isSameDomain + ", " + tparty + ")";

            L.a(Settings.TAG_POLICY, "<URL> ", "'" + url + "' " + tmp + " -> " + sc.toString() + " " + rules.toString());
        }

        return rules;
    }

    public static String getUserToken(boolean check) {
        if (Settings.DEBUG_NOTOKEN)
            return null;

        String id;
        if (Settings.LIC_DISABLE)
            id = "id______________________________";
        else
            id = Preferences.get_s(Settings.PREF_USER_TOKEN);

        // 32 - GP token, 36 - our free token
        if (check && id != null && !(id.length() == 32 || id.length() == 36)) {
            L.w(Settings.TAG_POLICY, "invalid token");
            return null;
        }

        return id;
    }

}
