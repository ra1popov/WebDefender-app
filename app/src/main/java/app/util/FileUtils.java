package app.util;

import java.io.File;
import java.math.BigInteger;

public class FileUtils {

    public static final long ONE_KB = 1024L;
    public static final BigInteger ONE_KB_BI = BigInteger.valueOf(1024L);
    public static final long ONE_MB = 1048576L;
    public static final BigInteger ONE_MB_BI;
    private static final long FILE_COPY_BUFFER_SIZE = 31457280L;
    public static final long ONE_GB = 1073741824L;
    public static final BigInteger ONE_GB_BI;
    public static final long ONE_TB = 1099511627776L;
    public static final BigInteger ONE_TB_BI;
    public static final long ONE_PB = 1125899906842624L;
    public static final BigInteger ONE_PB_BI;
    public static final long ONE_EB = 1152921504606846976L;
    public static final BigInteger ONE_EB_BI;
    public static final BigInteger ONE_ZB;
    public static final BigInteger ONE_YB;
    public static final File[] EMPTY_FILE_ARRAY;

    static {
        ONE_MB_BI = ONE_KB_BI.multiply(ONE_KB_BI);
        ONE_GB_BI = ONE_KB_BI.multiply(ONE_MB_BI);
        ONE_TB_BI = ONE_KB_BI.multiply(ONE_GB_BI);
        ONE_PB_BI = ONE_KB_BI.multiply(ONE_TB_BI);
        ONE_EB_BI = ONE_KB_BI.multiply(ONE_PB_BI);
        ONE_ZB = BigInteger.valueOf(1024L).multiply(BigInteger.valueOf(1152921504606846976L));
        ONE_YB = ONE_KB_BI.multiply(ONE_ZB);
        EMPTY_FILE_ARRAY = new File[0];
    }

    public static String byteCountToDisplaySize(long size) {
        return byteCountToDisplaySize(BigInteger.valueOf(size));
    }

    public static String byteCountToDisplaySize(BigInteger size) {
        String displaySize;
        if (size.divide(ONE_EB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = size.divide(ONE_EB_BI) + " EB";
        } else if (size.divide(ONE_PB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = size.divide(ONE_PB_BI) + " PB";
        } else if (size.divide(ONE_TB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = size.divide(ONE_TB_BI) + " TB";
        } else if (size.divide(ONE_GB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = size.divide(ONE_GB_BI) + " GB";
        } else if (size.divide(ONE_MB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = size.divide(ONE_MB_BI) + " MB";
        } else if (size.divide(ONE_KB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = size.divide(ONE_KB_BI) + " KB";
        } else {
            displaySize = size + " bytes";
        }

        return displaySize;
    }

}
