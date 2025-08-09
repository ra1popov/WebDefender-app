
/*
 * native helper functions
 *
 * last modified: 2015.09.18
 */

package app.common;

import java.nio.ByteBuffer;

public class LibNative {

    public static long LIBS_VERSION = 20150914; // sync with common.h !!! (and update on every libs edit)


    /* --- test and debug functions --- */


    public static long getLibsVersion() {
        return nf71();
    }

    public static String getString() {
        return nf72();
    }

    public static void dumpProcs() {
        nf73();
    }

    /// /	public static native void	 testNotify ();
    public static void testNetlink() {
        nf74();
    }


    /* --- process functions --- */


    // search processes by uid and return it name (pkgname), on uid 0 return "" (return null if not found)
    // works only if app with uid is running
    public static String[] getNamesFromUid(int uid, int dummy) {
        return nf50(uid);
    }

    // exit ;)
    public static void exit(int code) {
        nf51(code);
    }


    /* --- network functions --- */


    // return h_errno code, e.g. 0 - NET_SUCCESS, 1 - HOST_NOT_FOUND, 2 - TRY_AGAIN
    public static int getnameinfo(String ip, String[] names) {
        return nf21(ip, names);
    }

    // calc RFC 791 checksum, return -1 on error
    public static short calcCheckSum(int start_value, byte[] bytes, int offset, int size) {
        return nf22(start_value, bytes, offset, size);
    }

    // fill first 40 bytes of frame array with Ip + Tcp headers data, frame array must be allocated (40 bytes at least)
    // return -1 on error or 40 + dataSize on success
    public static int addIpTcpHeaderNative(byte[] frame, byte[] sourceIp, int sourcePort, byte[] destIp, int destPort,
                                           int seqNo, int ackNo, int flags, int windowSize, int dataSize) {
        return nf23(frame, sourceIp, sourcePort, destIp, destPort,
                seqNo, ackNo, flags, windowSize, dataSize);
    }

    // check that netlink work correctly
    public static boolean netlinkIsWork() {
        return nf24();
    }

    // search process uid for connection by src ip + dest ip + dest port
    // return uid or -1 if not found (-2 on error)
    // rIp == byte[4] array with IPv4 address in BE format
    public static int netlinkFindUid(int lPort, byte[] rIp, int rPort) {
        return nf25(lPort, rIp, rPort);
    }

    // start thread to watch network changes and call java callback (type (I)V) with message id
    // return -1 on error
    // ids: 16 NEWLINK, 17 DELLINK, 20 NEWADDR, 21 DELADDR, 24 NEWROUTE, 25 DELROUTE
    public static int netlinkWatchNet(String className, String funcName) {
        return nf26(className, funcName);
    }

    // convert buffer with IPv4 address in BE format to string (return "null" on error)
    public static String ipToString(byte[] ip, int port) {
        return nf27(ip, port);
    }

    //
    public static void socketPrintInfo(int fd) {
        nf28(fd);
    }

    // set socket keepalive params
    // idle - connecttion idle time (sec) before starts KA
    // count - number of KA probes before dropping the connection
    // interval - time (sec) between individual KA probes
    public static boolean socketSetKAS(int fd, int idle, int count, int interval) {
        return nf29(fd, idle, count, interval);
    }

    // enable/disable socket keepalive
    public static boolean socketEnableKA(int fd, boolean enable) {
        return nf30(fd, enable);
    }

    // check if socket have unsended data
    public static boolean socketHaveDataToSend(int fd) {
        return nf31(fd);
    }

    // try to send data through socket with flush (return false if no data)
    public static boolean socketSendDataForce(int fd, byte[] data) {
        return nf32(fd, data);
    }

    // return names of all or active (up == true) network interfaces (or null)
    public static String[] getIfNames(boolean up) {
        return nf33(up);
    }


    /* --- file functions --- */


    // set file fd to blocking/non-blocking state
    public static void fileSetBlocking(int fd, boolean enable) {
        nf10(fd, enable);
    }

    // close file fd, returns zero on success
    public static int fileClose(int fd) {
        return nf11(fd);
    }

    // calculate sha1 from file, return byte[20] on success or null on error
//	  public static native byte[]	   sha1File (String filepath);

    // get file content from zip archive and return buffer or null on error
    public static ByteBuffer zipReadFile(String zipfile, String filename) {
        return nf01(zipfile, filename);
    }

    public static boolean zipFreeData(ByteBuffer data) {
        return nf02(data);
    }


    /* --- text functions --- */


    public static String asciiToLower(String str) {
        return nf81(str);
    }

    public static boolean asciiStartsWith(String start, String str) {
        return nf82(start, str);
    }

    public static boolean asciiEndsWith(String end, String str) {
        return nf83(end, str);
    }

    public static int asciiIndexOf(String search, String str) {
        return nf84(search, 0, str);
    }

    public static int asciiIndexOf(String search, int from, String str) {
        return nf84(search, from, str);
    }


    /* --- signal and thread functions --- */


    // set handler for signals (SIGHUP 1) and return 0 on success
    public static int signalHandlerSet(int signal) {
        return nf61(signal);
    }

    // send signal to our process (SIGHUP 1)
    public static int signalSendSelf(int signal) {
        return nf62(signal);
    }

    // get current thread native id
    public static long threadGetSelfId() {
        return nf63();
    }

    // send signal to thread (see threadGetSelfId) and return 0 on success
    public static int threadSendSignal(long id, int signal) {
        return nf64(id, signal);
    }


    /* --------- real --------- */


    private static native long nf71();

    private static native String nf72();

    private static native void nf73();

    private static native void nf74();

    private static native String[] nf50(int uid);

    private static native void nf51(int code);

    private static native int nf21(String ip, String[] names);

    private static native short nf22(int start_value, byte[] bytes, int offset, int size);

    private static native int nf23(byte[] frame, byte[] sourceIp, int sourcePort, byte[] destIp, int destPort,
                                   int seqNo, int ackNo, int flags, int windowSize, int dataSize);

    private static native boolean nf24();

    private static native int nf25(int lPort, byte[] rIp, int rPort);

    private static native int nf26(String className, String funcName);

    private static native String nf27(byte[] ip, int port);

    private static native void nf28(int fd);

    private static native boolean nf29(int fd, int idle, int count, int interval);

    private static native boolean nf30(int fd, boolean enable);

    private static native boolean nf31(int fd);

    private static native boolean nf32(int fd, byte[] data);

    private static native String[] nf33(boolean up);

    private static native void nf10(int fd, boolean enable);

    private static native int nf11(int fd);

    private static native ByteBuffer nf01(String zipfile, String filename);

    private static native boolean nf02(ByteBuffer data);

    private static native String nf81(String str);

    private static native boolean nf82(String start, String str);

    private static native boolean nf83(String end, String str);

    private static native int nf84(String search, int from, String str);

    private static native int nf61(int signal);

    private static native int nf62(int signal);

    private static native long nf63();

    private static native int nf64(long id, int signal);
}
