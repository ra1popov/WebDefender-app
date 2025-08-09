
/*
 * scan functions
 *
 * last modified: 2015.09.18
 */

package app.scanner;

import java.nio.ByteBuffer;
import java.util.Set;

import app.common.LibNative;

public class LibScan {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /* --- scan db defines --- */

    // this numbers must be same as record_type enum in C code (see scan.h header)
    public static final int RECORD_TYPE_UNKNOWN = 0;
    //
    public static final int RECORD_TYPE_TEST = 1;
    public static final int RECORD_TYPE_WHITE = 2;
    //
    public static final int RECORD_TYPE_CHARGEABLE = 3;
    public static final int RECORD_TYPE_FRAUD = 4;
    public static final int RECORD_TYPE_MALWARE = 5;
    //
    public static final int RECORD_TYPE_ADS = 6; // invasive ads
    public static final int RECORD_TYPE_ADS_TPARTY = 7; // ads on external sites (== ADS in non browsers)
    public static final int RECORD_TYPE_ADS_OK = 8; // allowed ads
    //
    public static final int RECORD_TYPE_SOCIAL_OTHER = 20;
    public static final int RECORD_TYPE_SOCIAL_GPLUS = 21;
    public static final int RECORD_TYPE_SOCIAL_VK = 22;
    public static final int RECORD_TYPE_SOCIAL_FB = 23;
    public static final int RECORD_TYPE_SOCIAL_TWI = 24;
    public static final int RECORD_TYPE_SOCIAL_ODNKLASS = 25;
    public static final int RECORD_TYPE_SOCIAL_MAILRU = 26;
    public static final int RECORD_TYPE_SOCIAL_LJ = 27;
    public static final int RECORD_TYPE_SOCIAL_LINKEDIN = 28;
    public static final int RECORD_TYPE_SOCIAL_MOIKRUG = 30;
    //
    public static final int RECORD_TYPE_CLEAN = 255;

    public static String recordTypeToString(int type) {
        switch (type) {
            case RECORD_TYPE_TEST:
                return "TEST";
            case RECORD_TYPE_WHITE:
                return "WHITE";
            //
            case RECORD_TYPE_CHARGEABLE:
                return "CHARGEABLE";
            case RECORD_TYPE_FRAUD:
                return "FRAUD";
            case RECORD_TYPE_MALWARE:
                return "MALWARE";
            //
            case RECORD_TYPE_ADS:
                return "ADS";
            case RECORD_TYPE_ADS_TPARTY:
                return "ADS_TPARTY";
            case RECORD_TYPE_ADS_OK:
                return "ADS_OK";
            //
            case RECORD_TYPE_SOCIAL_OTHER:
                return "SOCIAL_OTHER";
            case RECORD_TYPE_SOCIAL_GPLUS:
                return "SOCIAL_GPLUS";
            case RECORD_TYPE_SOCIAL_VK:
                return "SOCIAL_VK";
            case RECORD_TYPE_SOCIAL_FB:
                return "SOCIAL_FB";
            case RECORD_TYPE_SOCIAL_TWI:
                return "SOCIAL_TWI";
            case RECORD_TYPE_SOCIAL_ODNKLASS:
                return "SOCIAL_ODNKLASS";
            case RECORD_TYPE_SOCIAL_MAILRU:
                return "SOCIAL_MAILRU";
            case RECORD_TYPE_SOCIAL_LJ:
                return "SOCIAL_LJ";
            case RECORD_TYPE_SOCIAL_LINKEDIN:
                return "SOCIAL_LINKEDIN";
            case RECORD_TYPE_SOCIAL_MOIKRUG:
                return "SOCIAL_MOIKRUG";
            //
            case RECORD_TYPE_CLEAN:
                return "CLEAN";
            default:
                return "UNKNOWN";
        }
    }

    public static boolean recordTypeIsValid(int type) {
        return (type >= RECORD_TYPE_TEST && type <= RECORD_TYPE_CLEAN);
    }

    public static String recordTypesToString(Set<Integer> types) {
        StringBuilder str = new StringBuilder();
        int size = types.size();
        int i = 0;

        str.append('[');
        for (Integer type : types) {
            int t = type;
            if (recordTypeIsValid(t))
                str.append(recordTypeToString(t));
            else
                str.append(t);

            if (++i != size)
                str.append(", ");
        }
        str.append(']');

        return str.toString();
    }

    public static int recordTypeFromString(String type) {
        if (type.equals("test")) return RECORD_TYPE_TEST;
        else if (type.equals("white")) return RECORD_TYPE_WHITE;
            //
        else if (type.equals("chargeable")) return RECORD_TYPE_CHARGEABLE;
        else if (type.equals("fraud")) return RECORD_TYPE_FRAUD;
        else if (type.equals("malware")) return RECORD_TYPE_MALWARE;
            //
        else if (type.equals("ads")) return RECORD_TYPE_ADS;
        else if (type.equals("ads_tparty")) return RECORD_TYPE_ADS_TPARTY;
        else if (type.equals("ads_ok")) return RECORD_TYPE_ADS_OK;
            //
        else if (type.equals("social_other")) return RECORD_TYPE_SOCIAL_OTHER;
        else if (type.equals("social_gplus")) return RECORD_TYPE_SOCIAL_GPLUS;
        else if (type.equals("social_vk")) return RECORD_TYPE_SOCIAL_VK;
        else if (type.equals("social_fb")) return RECORD_TYPE_SOCIAL_FB;
        else if (type.equals("social_twi")) return RECORD_TYPE_SOCIAL_TWI;
        else if (type.equals("social_odnklass")) return RECORD_TYPE_SOCIAL_ODNKLASS;
        else if (type.equals("social_mailru")) return RECORD_TYPE_SOCIAL_MAILRU;
        else if (type.equals("social_lj")) return RECORD_TYPE_SOCIAL_LJ;
        else if (type.equals("social_linkedin")) return RECORD_TYPE_SOCIAL_LINKEDIN;
        else if (type.equals("social_moikrug")) return RECORD_TYPE_SOCIAL_MOIKRUG;
            //
        else if (type.equals("clean")) return RECORD_TYPE_CLEAN;
        else return RECORD_TYPE_UNKNOWN;
    }

    // this numbers must be same as scan_type enum in C code (see scan.h header)
    public static final int SCAN_TYPE_UNKNOWN = 0;
    //
    public static final int SCAN_TYPE_DOMAIN = 1;
    public static final int SCAN_TYPE_URL = 2;
    public static final int SCAN_TYPE_HTML = 3;

    public static boolean scanTypeIsValid(int type) {
        return (type >= SCAN_TYPE_DOMAIN && type <= SCAN_TYPE_HTML);
    }

    /* --- format detect defines --- */

    // this numbers must be same as bin_data_type enum in C code (see bindata.h header)
    public static final int BINARY_TYPE_UNKNOWN = 0;
    //
    public static final int BINARY_TYPE_ZIP = 1;
    public static final int BINARY_TYPE_TAR = 2;
    public static final int BINARY_TYPE_GZIP = 3;
    public static final int BINARY_TYPE_BZIP2 = 4;
    public static final int BINARY_TYPE_RAR4 = 5;
    public static final int BINARY_TYPE_RAR5 = 6;
    public static final int BINARY_TYPE_ZIP7 = 7; // 7zip
    public static final int BINARY_TYPE_CAB = 8;
    public static final int BINARY_TYPE_XZ = 9;
    //
    public static final int BINARY_TYPE_ELF32 = 20;
    public static final int BINARY_TYPE_ELF64 = 21;
    public static final int BINARY_TYPE_DEX035 = 22;

    public static String binaryTypeToString(int type) {
        switch (type) {
            case BINARY_TYPE_ZIP:
                return "ZIP";
            case BINARY_TYPE_TAR:
                return "TAR";
            case BINARY_TYPE_GZIP:
                return "GZIP";
            case BINARY_TYPE_BZIP2:
                return "BZIP2";
            case BINARY_TYPE_RAR4:
                return "RAR4";
            case BINARY_TYPE_RAR5:
                return "RAR5";
            case BINARY_TYPE_ZIP7:
                return "ZIP7";
            case BINARY_TYPE_CAB:
                return "CAB";
            case BINARY_TYPE_XZ:
                return "XZ";
            //
            case BINARY_TYPE_ELF32:
                return "ELF32";
            case BINARY_TYPE_ELF64:
                return "ELF64";
            case BINARY_TYPE_DEX035:
                return "DEX035";
            //
            default:
                return "UNKNOWN";
        }
    }

    public static boolean binaryTypeIsValid(int type) {
        return (type >= BINARY_TYPE_ZIP && type <= BINARY_TYPE_DEX035);
    }

    // minimal and recommended data sizes to detect formats (see bindata.h header)
    public static final int BINARY_DATA_LEN_MIN = 4;
    public static final int BINARY_DATA_LEN_ALL = 265;

    /* --- scan db functions --- */

    /*
     * load database for specific scan type
     * scanType == SCAN_TYPE_*
     *
     * check with dbIsLoaded if database loaded or not
     */
    public static ByteBuffer loadDB(int scanType, String filepath, int programVersion) {
        return sf01(scanType, filepath, programVersion);
    }

    public static boolean unloadDB(ByteBuffer db) {
        return sf02(db);
    }

    // check that database loaded correctly
    public static boolean dbIsLoaded(ByteBuffer db) {
        return sf03(db);
    }

    // get database version, number of records in database, and compare records types
    public static int dbGetVersion(ByteBuffer db) {
        return sf04(db);
    }

    public static long dbGetRecordsNumber(ByteBuffer db) {
        return sf05(db);
    }

    /*
     * return int array with types of founded record (RECORD_TYPE_* values)
     *	   + same number of records ids (any record id maybe == 0)
     * on error return null
     *
     * to proper url scan, it must be in lowercase
     */
    private static int[] dbScanData(ByteBuffer db, byte[] bytes, long size, int dummy) {
        return sf10(db, bytes, size, dummy);
    }

    public static int[] dbScanData(ByteBuffer db, byte[] bytes, long size, boolean check) {
        int[] result = dbScanData(db, bytes, size, 0);
        if (check && result != null && result.length % 2 != 0)
            return null;

        return result;
    }

    /*
     * scan and return major verdict
     *
     * to proper url scan, it must be in lowercase
     */
    public static int dbScanData(ByteBuffer db, byte[] bytes, long size) {
        int[] result = dbScanData(db, bytes, size, true);

        if (result == null || result.length == 0)
            return RECORD_TYPE_CLEAN; // if == null error, wtf ?

        if (result.length == 2) {
            if (result[0] == RECORD_TYPE_UNKNOWN)
                return RECORD_TYPE_CLEAN; // wtf ?
            return result[0];
        }

        int type = RECORD_TYPE_CLEAN;
        int num = result.length / 2;
        for (int i = 0; i < num; i++) {
            if (result[i] == RECORD_TYPE_UNKNOWN)
                continue;
            type = recordTypeGetMajor(type, result[i]);
        }

        return type;
    }

    /*
     * compare records types (RECORD_TYPE_* values) and return most important
     *
     * RECORD_WHITE > RECORD_MALWARE > RECORD_FRAUD > RECORD_CHARGEABLE >
     *	   RECORD_SOCIAL_* > RECORD_ADS_OK > RECORD_ADS_TPARTY > RECORD_ADS >
     *	   RECORD_TEST > RECORD_CLEAN
     */
    public static int recordTypeGetMajor(int type1, int type2) {
        int type = sf06(type1, type2);
        if (type == RECORD_TYPE_UNKNOWN)
            type = RECORD_TYPE_CLEAN; // error, compare with RECORD_UNKNOWN or wtf ?

        return type;
    }

    /*
     * check if record type is ads, social network or dangerous object (malware, fraud)
     * type == RECORD_TYPE_*
     */
    public static boolean recordTypeIsDangerous(int type) {
        return sf07(type);
    }

    public static boolean recordTypeIsAds(int type) {
        return sf08(type);
    }

    public static boolean recordTypeIsSocial(int type) {
        return sf09(type);
    }

    /* --- format detect functions --- */

    /*
     * check binary data (bytes) of size for known formats
     * return BINARY_TYPE_* values
     */
    public static int binaryDataDetectType(byte[] bytes, long size) {
        return sf11(bytes, size, 0 /*dummy*/); // TODO XXX remove dummy value
    }

    /*
     * search binary data (bytes) of size for first known format (see binaryDataDetectType)
     * return BINARY_TYPE_* values
     *
     * TODO XXX return offset to founded format start
     */
    public static int binaryDataSearchType(byte[] bytes, long size) {
        return sf12(bytes, size, 0 /*dummy*/); // TODO XXX remove dummy value
    }

    /*
     * check if binary format type is archive or OS executable
     * type == BINARY_TYPE_* values
     */
    public static boolean binaryTypeIsArchive(int type) {
        return sf13(type);
    }

    public static boolean binaryTypeIsExecutable(int type) {
        return sf14(type);
    }

    /* --- additional functions --- */

    // NOTE! this functions work only with urls in lowercase
    // TODO XXX, optimize, rewrite to StringBuilder

    // 'http://www.test.com:8080//dfdf/?blabla' -> 'test.com:8080//dfdf/?blabla'
    public static String prepareUrlFull(String url) {
        //url = url.toLowerCase(Locale.ENGLISH);
        final int len = url.length();

        if (LibNative.asciiStartsWith("http://www.", url) && len > 11) url = url.substring(11);
        else if (LibNative.asciiStartsWith("http://", url) && len > 7) url = url.substring(7);
        else if (LibNative.asciiStartsWith("https://www.", url) && len > 12) url = url.substring(12);
        else if (LibNative.asciiStartsWith("https://", url) && len > 8) url = url.substring(8);
        else if (LibNative.asciiStartsWith("www.", url) && len > 4) url = url.substring(4);

        return url;
    }

    /*
     * 'http://www.test.com:8080//dfdf/?blabla' -> 'test.com:8080/dfdf'
     * 'http://test2.com:80/dfdf -> 'test2.com/dfdf'
     * 'http://www.test.com/dfdf/#blabla' -> 'test.com:8080/dfdf'
     */
    public static String prepareUrl(String url) {
        url = prepareUrlFull(url);

        int q = url.indexOf('?');
        int r = url.indexOf('#');
        if ((r >= 0 && q >= 0 && r < q) || q < 0) q = r;
        if (q >= 0) url = url.substring(0, q);

        if (LibNative.asciiIndexOf("//", url) >= 0)
            url = url.replaceAll("/+", "/");
        if (LibNative.asciiEndsWith("/", url))
            url = url.substring(0, url.length() - 1);

        if (LibNative.asciiEndsWith(":80", url)) url = url.substring(0, url.length() - 3);
        int s = url.indexOf('/');
        if (s >= 0) {
            int p = LibNative.asciiIndexOf(":80/", url);
            if (p >= 0 && s > p)
                return (url.substring(0, p) + url.substring(p + 3));
        }

        return url;
    }

    // 'http://www.test.com:8080//dfdf/?blabla' -> 'test.com:8080'
    public static String prepareDomainFull(String url) {
        url = prepareUrlFull(url);

        int s = url.indexOf('/');
        int q = url.indexOf('?');
        if ((q >= 0 && s >= 0 && q < s) || s < 0) s = q;
        if (s >= 0) return url.substring(0, s);
        else return url;
    }

    // 'http://www.test.com:8080//dfdf/?blabla' -> 'test.com'
    public static String prepareDomain(String url) {
        url = prepareUrlFull(url);

        int s = url.indexOf('/');
        int p = url.indexOf(':');
        int q = url.indexOf('?');
        if ((p >= 0 && s >= 0 && p < s) || s < 0) s = p;
        if ((q >= 0 && s >= 0 && q < s) || s < 0) s = q;
        if (s >= 0) return url.substring(0, s);
        else return url;
    }

    /*
     * 'test.com/dfdf', 'amazon.com' -> 'test.com^616d617a6f6e2e636f6d'
     * use after prepare* functions before url scan
     */
    public static String urlAddReferer(String url, String referer) {
        if (referer.isEmpty())
            return url;

        byte[] refbuf = referer.getBytes();
        byte[] hexbuf = new byte[refbuf.length * 2];
        int i;

        for (i = 0; i < refbuf.length; i++) {
            hexbuf[2 * i] = (byte) HEX_CHARS[(refbuf[i] & 0xF0) >>> 4];
            hexbuf[2 * i + 1] = (byte) HEX_CHARS[refbuf[i] & 0x0F];
        }

        String str = new String(hexbuf, 0, 0, hexbuf.length);
        return (url + "^" + str);
    }

    /* --------- real --------- */

    private static native ByteBuffer sf01(int scanType, String filepath, int programVersion);

    private static native boolean sf02(ByteBuffer db);

    private static native boolean sf03(ByteBuffer db);

    private static native int sf04(ByteBuffer db);

    private static native long sf05(ByteBuffer db);

    private static native short sf06(int type1, int type2);

    private static native boolean sf07(int type);

    private static native boolean sf08(int type);

    private static native boolean sf09(int type);

    private static native int[] sf10(ByteBuffer db, byte[] bytes, long size, int dummy);

    private static native short sf11(byte[] bytes, long size, int dummy);

    private static native short sf12(byte[] bytes, long size, int dummy);

    private static native boolean sf13(int type);

    private static native boolean sf14(int type);
}
