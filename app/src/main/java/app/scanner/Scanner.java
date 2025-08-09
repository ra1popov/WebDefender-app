
/*
 * class to create loaded copy of scanner databases and scan data with it
 *
 * last modified: 2015.09.18
 */

package app.scanner;

import java.nio.ByteBuffer;

import app.common.LibNative;
import app.common.debug.L;
import app.internal.Settings;

/*
 * TODO XXX fix scan:
 *
 * I/WG_===3===(21351): '/'
 * I/WG_Scanner(21351): dom1 '''
 * I/WG_Scanner(21351): url1 ''/''
 * I/WG_Scanner(21351): url3 '/''
 */

public class Scanner {
    public static final String domainsDBName = "domains.db";
    public static final String urlsDBName = "urls.db";
    public static final String fastDBName0 = "fast0.db";
    public static final String debugDBName = "debug.db";
    public static final String proDBDomain = "full." + Settings.APP_SITE; // "full.webdefender.app"

    protected boolean databasesLoaded;
    protected boolean databasesPro;
    protected String databasesPath = "";
    protected int databasesVersion;
    protected int programVersion;

    protected ByteBuffer domainsDB = null;
    protected ByteBuffer urlsDB = null;
    protected ByteBuffer fastDB0 = null;
    protected ByteBuffer debugDB = null;

    /* ----------------------------------------------------------- */

    /*
     * init with full path to databases folder
     * check status with isInited function
     */
    public Scanner(String databasesPath, int programVersion) {
        this.databasesPath = (databasesPath.endsWith("/")) ? databasesPath : databasesPath + "/";
        this.programVersion = programVersion;

        // domains
        this.domainsDB = LibScan.loadDB(LibScan.SCAN_TYPE_DOMAIN, this.databasesPath + Scanner.domainsDBName, this.programVersion);

        if (!LibScan.dbIsLoaded(this.domainsDB))
            this.domainsDB = null;

//		// urls
        this.urlsDB = LibScan.loadDB(LibScan.SCAN_TYPE_URL, this.databasesPath + Scanner.urlsDBName, this.programVersion);

        if (!LibScan.dbIsLoaded(this.urlsDB))
            this.urlsDB = null;

        if (this.domainsDB == null || this.urlsDB == null) {
            // load failed
            this.clean();
            return;
        }


        // get databases version from domains database
        this.databasesVersion = LibScan.dbGetVersion(this.domainsDB);
        this.databasesLoaded = true;

        reloadFast(); // check databasesLoaded

//		reloadDebug();

        // check if use pro db
        int recordType = LibScan.dbScanData(this.domainsDB, proDBDomain.getBytes(), proDBDomain.length());

        if (recordType != LibScan.RECORD_TYPE_CLEAN) // ok, domain detected
        {
            recordType = LibScan.dbScanData(this.urlsDB, proDBDomain.getBytes(), proDBDomain.length());

            if (recordType != LibScan.RECORD_TYPE_CLEAN)
                databasesPro = true;
        }

    }

    // unload loaded databases and clear allocated data (TODO XXX finalize sync)
    public void clean() {
        this.databasesLoaded = false;

        if (this.domainsDB != null) {
            LibScan.unloadDB(this.domainsDB);
            this.domainsDB = null;
        }
        if (this.urlsDB != null) {
            LibScan.unloadDB(this.urlsDB);
            this.urlsDB = null;
        }

        if (this.fastDB0 != null) {
            LibScan.unloadDB(this.fastDB0);
            this.fastDB0 = null;
        }

        if (this.debugDB != null) {
            LibScan.unloadDB(this.debugDB);
            this.debugDB = null;
        }
    }

    // reload fast db (TODO XXX sync)
    public boolean reloadFast() {
        if (!this.databasesLoaded)
            return true;

        if (this.fastDB0 != null)
            LibScan.unloadDB(this.fastDB0);

        // fast0
        this.fastDB0 = LibScan.loadDB(LibScan.SCAN_TYPE_URL,
                this.databasesPath + "../" + Scanner.fastDBName0, this.programVersion);
        if (!LibScan.dbIsLoaded(this.fastDB0)) {
            this.fastDB0 = null;
            return false;
        }

        return true;
    }

    // was disabled 28.07.2023
    // reload debug db (TODO XXX sync)
    public boolean reloadDebug() {
        if (!this.databasesLoaded)
            return true;

        if (this.debugDB != null)
            LibScan.unloadDB(this.debugDB);

        // debug
        this.debugDB = LibScan.loadDB(LibScan.SCAN_TYPE_URL, this.databasesPath + "../" + Scanner.debugDBName, this.programVersion);

        if (!LibScan.dbIsLoaded(this.debugDB)) {
            this.debugDB = null;
            return false;
        }

        return true;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.clean();
    }

    public boolean isInited() {
        return this.databasesLoaded;
    }

    public boolean isProVersion() {
        return this.databasesPro;
    }

    public int getDBVersion() {
        return this.databasesVersion;
    }

    public long getDBRecordsNum() {
        if (!this.databasesLoaded)
            return 0;

        long num = LibScan.dbGetRecordsNumber(this.domainsDB) + LibScan.dbGetRecordsNumber(this.urlsDB);
        return num;
    }

    /*
     * check full domain name from url
     * and levels 2, 3 name in whitelist if full name is clear
     * i.e. 't.cool.test.com' and 'test.com'
     *
     * return verdict value from TEST to CLEAN (RECORD_TYPE_* values)
     *
     * useSigs used to skip signature scan (when calling from scanUrl)
     *
     * XXX maybe return and signature number i.e. return ScanResult?
     */
    public int scanDomain(String url) {
        return scanDomainInternal(url, null, true, true);
    }

    public int scanDomain(String url, String referer) {
        return scanDomainInternal(url, referer, true, true);
    }

    public int scanDomainInternal(String url, String referer, boolean useSigs, boolean prepareData) {
        if (!this.databasesLoaded)
            return LibScan.RECORD_TYPE_CLEAN;

        // check full domain

        String domain1 = LibScan.prepareDomain(LibNative.asciiToLower(url));

        if (domain1.isEmpty())
            return LibScan.RECORD_TYPE_CLEAN;

        if (Settings.DEBUG_SCANNER_URLS)
            L.a(Settings.TAG_SCANNER, "dom1 '" + domain1 + "'");

        int recordType = LibScan.dbScanData(this.domainsDB, domain1.getBytes(), domain1.length());
        if (recordType == LibScan.RECORD_TYPE_WHITE)
            return recordType;

        // clear, check level 2 and 3 domain. maybe white?
        // TODO XXX ip?

        String[] domain1_s = domain1.split("\\.");
        int l = domain1_s.length;
        boolean d2Ok = false;
        boolean d3Ok = false;
        String domain2 = null;
        int recordTypeD = LibScan.RECORD_TYPE_CLEAN;

        if (l > 2 && !domain1_s[l - 1].isEmpty() && !domain1_s[l - 2].isEmpty()) {
            d2Ok = true;
            domain2 = domain1_s[l - 2] + "." + domain1_s[l - 1];

            if (l > 3 && !domain1_s[l - 3].isEmpty())
                d3Ok = true;
        }

        if (d3Ok) {
            String domain3 = domain1_s[l - 3] + "." + domain2;

            if (Settings.DEBUG_SCANNER_URLS)
                L.a(Settings.TAG_SCANNER, "dom2 '" + domain3 + "'");

            recordTypeD = LibScan.dbScanData(this.domainsDB, domain3.getBytes(), domain3.length());
        }

        if (d2Ok && recordTypeD != LibScan.RECORD_TYPE_WHITE) {
            if (Settings.DEBUG_SCANNER_URLS)
                L.a(Settings.TAG_SCANNER, "dom3 '" + domain2 + "'");

            int recordTypeD2 = LibScan.dbScanData(this.domainsDB, domain2.getBytes(), domain2.length());
            if (recordTypeD2 == LibScan.RECORD_TYPE_WHITE || recordTypeD == LibScan.RECORD_TYPE_CLEAN)
                recordTypeD = recordTypeD2;
        }

        if (recordTypeD == LibScan.RECORD_TYPE_WHITE || recordType == LibScan.RECORD_TYPE_CLEAN)
            recordType = recordTypeD; // new verdict replace original: VVV->WHITE or CLEAN->VVV

        // scan domains by signature

        if (useSigs && recordType == LibScan.RECORD_TYPE_CLEAN) {
            if (referer != null) {
                referer = LibNative.asciiToLower(referer);
                domain1 = LibScan.urlAddReferer(domain1, referer); // add referer to exclude some detects
            }

            recordType = LibScan.dbScanData(this.urlsDB, domain1.getBytes(), domain1.length());
            if (this.fastDB0 != null && recordType == LibScan.RECORD_TYPE_CLEAN)
                recordType = LibScan.dbScanData(this.fastDB0, domain1.getBytes(), domain1.length());
        }

        return recordType;
    }

    /*
     * check full url (with domain and params) and it parts
     *
     * checking process:
     * - check domain name (see scanDomain)
     * - full url with args and without args (i.e. 'test.com//sdsd?df' and 'test.com/sdsd')
     *	 and (if CLEAN) url without domain and without args (i.e. '//sdsd?df' and '/sdsd')
     * - select most dangerous verdict between domain and url, return WHITE if domain == WHITE
     *
     * also remove port 80 from url, i.e. url 'test.com:80//sdsd?df'
     *	   will be scanned as 'test.com:80//sdsd?df' and 'test.com/sdsd'
     *
     * return ScanResult with types (verdicts) values from TEST to CLEAN (RECORD_TYPE_* values)
     *
     * TODO XXX, optimize
     */
    public ScanResult scanUrl(String url) {
        return scanUrl(url, null);
    }

    public ScanResult scanUrl(String url, String referer) {
        ScanResult scan_result = new ScanResult(LibScan.RECORD_TYPE_CLEAN);

        if (!this.databasesLoaded)
            return scan_result;

        // parse

        url = LibNative.asciiToLower(url);
        if (referer != null)
            referer = LibNative.asciiToLower(referer);

        String url_f = LibScan.prepareUrlFull(url);
        String domain = LibScan.prepareDomain(url_f);
        if (domain.isEmpty())
            return scan_result;

        int url_f_len = url_f.length();
        int domain_len = domain.length();
        if (domain_len > url_f_len) {
            // url parse error
            L.e(Settings.TAG_SCANNER, "url prepare: '", domain, "', '", url_f, "'");
            return scan_result;
        }

        // check domain

        //if (Settings.DEBUG_SCANNER_URLS)
        //	  L.a(Settings.TAG_SCANNER, "url0 '" + domain + "'");

        int recordType = this.scanDomainInternal(domain, referer, false, false);
        scan_result.addType(recordType);

        // check url (if have -> url > domain + '/' or '?')

        //if (type != LibScan.RECORD_TYPE_WHITE && url_f.length() > domain.length() + 1)
        //if (type != LibScan.RECORD_TYPE_WHITE && url_f.length() > domain.length())
        if (recordType != LibScan.RECORD_TYPE_WHITE) {
            do {
                /*
                 * prepareUrl possible V2 == V1 or V2 == V0:
                 *	  test.com?1 -> test.com
                 *	  test.com/sdsd -> test.com/sdsd
                 *
                 * TODO XXX fix 'http://test6.com:80/ddd'
                 */
                String url_p = LibScan.prepareUrl(url_f);
                int url_p_len = url_p.length();

                String domain_f = LibScan.prepareDomainFull(url_f);
                int domain_f_len = domain_f.length();

                boolean is_same = (!url_p.equals(domain) && !url_p.equals(url_f)) ? false : true;

                // V1 test.com//sdsd?df
                if (Settings.DEBUG_SCANNER_URLS)
                    L.a(Settings.TAG_SCANNER, "url1 '" + url_f + "'");

                recordType = LibScan.dbScanData(this.domainsDB, url_f.getBytes(), url_f_len);
                scan_result.addType(recordType);

                // V2 test.com/sdsd
                if (!is_same) {
                    if (Settings.DEBUG_SCANNER_URLS)
                        L.a(Settings.TAG_SCANNER, "url2 '" + url_p + "'");

                    recordType = LibScan.dbScanData(this.domainsDB, url_p.getBytes(), url_p_len);
                    scan_result.addType(recordType);
                }

                // V3 //sdsd?df
                int path_len = 0;
                if (!url_f.equals(domain_f) && url_f_len > domain_f_len) {
                    String path = url_f.substring(domain_f_len);
                    path_len = path.length();

                    if (!path.equals("/") && !path.equals("?")) // TODO XXX
                    {
                        if (Settings.DEBUG_SCANNER_URLS)
                            L.a(Settings.TAG_SCANNER, "url3 '" + path + "'");

                        recordType = LibScan.dbScanData(this.domainsDB, path.getBytes(), path.length());
                        scan_result.addType(recordType);
                    }
                }

                // V4 /sdsd
                if (!is_same && url_p_len > domain_len) // TODO XXX url_p <= domain_f
                {
                    // prepareUrl remove :80 port from url, but prepareDomainFull do not this
                    String path = (LibNative.asciiEndsWith(":80", domain_f)) ?
                            url_p.substring(domain_len) : url_p.substring(domain_f_len);
                    int len = path.length();

                    if (path_len != len) {
                        if (Settings.DEBUG_SCANNER_URLS)
                            L.a(Settings.TAG_SCANNER, "url4 '" + path + "'");

                        recordType = LibScan.dbScanData(this.domainsDB, path.getBytes(), len);
                        scan_result.addType(recordType);
                    }
                }
            }
            while (false);

            // url signature scan

            if (referer != null)
                url = LibScan.urlAddReferer(url, referer); // add referer to exclude some detects

            int[] result = LibScan.dbScanData(this.urlsDB, url.getBytes(), url.length(), true);
            if (!scan_result.addScanResult(result))
                L.e(Settings.TAG_SCANNER, "url err: '", url, "'"); // wtf?

            if (this.fastDB0 != null) {
                result = LibScan.dbScanData(this.fastDB0, url.getBytes(), url.length(), true);
                if (!scan_result.addScanResult(result))
                    L.e(Settings.TAG_SCANNER, "url fast err: '", url, "'"); // wtf?
            }

            // debug signature scan

            if (this.debugDB != null) {
                result = LibScan.dbScanData(this.debugDB, url.getBytes(), url.length(), true);
                if (!scan_result.addScanResult(result))
                    L.e(Settings.TAG_SCANNER, "debug err: '", url, "'"); // wtf?
            }
        }

        //
        return scan_result;
    }
}
