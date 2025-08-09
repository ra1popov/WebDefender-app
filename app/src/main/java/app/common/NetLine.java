package app.common;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import app.common.debug.L;
import app.internal.Settings;

public class NetLine {

    private static byte[] buf = new byte[262144];

    private static String cache = null;
    private static boolean cacheIsV4 = false;

    // not tread safe
    public static int getUidFromProc(byte[] serverIp, int serverPort, int localPort, boolean tryOld) {
        int res = -1;

        // check ipv4 cache
        // TODO XXX test this optimization (may be we found incorrect uid?)

        if (tryOld && cache != null && cacheIsV4) {
            res = findUidInTcp4(cache, serverIp, serverPort, localPort);
            if (res >= 0)
                return res;
        }

        // try ipv4 proc

        String buf = getFileAsString("/proc/self/net/tcp");
        cache = buf;
        cacheIsV4 = true;

        if (buf != null)
            res = findUidInTcp4(buf, serverIp, serverPort, localPort);
        if (res >= 0)
            return res;

        // try ipv6 proc

        buf = getFileAsString("/proc/self/net/tcp6");
        cache = buf;
        cacheIsV4 = false;

        if (buf != null)
            res = findUidInTcp6(buf, serverIp, serverPort, localPort);
        return res;
    }

    private static String getFileAsString(String fileName) {
        InputStream stream = null;
        try {
            stream = new BufferedInputStream(new FileInputStream(fileName));
            int read, off;
            read = stream.read(buf);
            off = read;

            while (read > 0) {
                if (off + 8192 > buf.length)
                    makeBufferBigger();
                read = stream.read(buf, off, 8192);
                if (read > 0)
                    off += read;
            }
            //L.w(Settings.TAG_NETLINE, "proc file size: " + off);

            String f = new String(buf, 0, 0, off);
            return f;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return null;
    }

    private static void makeBufferBigger() {
        byte[] b = new byte[buf.length + 131072];
        System.arraycopy(buf, 0, b, 0, buf.length);
        buf = b;
    }

    public static final String getIpString(byte[] ip) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < ip.length; i++) {
            buf.append(".").append((ip[i] & 0xFF));
        }
        return buf.substring(1);
    }

    /*
    Parsing line, that looks like this:
      sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode
       0: 010A0A0A:8EBC C7F8E136:1094 01 00000000:00000000 00:00000000 00000000 10089		 0 161500 1 00000000 21 4 18 10 -1
     */
    public static int findUidInTcp4(String buf, byte[] serverIp, int serverPort, int localPort) {
//		  if (DEBUG) L.d(Settings.TAG_NETLINE, buf);
        String locPort = getOnlyPortString(localPort);
        final String addr = getIpPortString(serverIp, serverPort, false);

        int portIndex = 0;
        while ((portIndex = LibNative.asciiIndexOf(locPort, portIndex + 1, buf)) >= 0) {
            final int addrIndex = LibNative.asciiIndexOf(addr, portIndex, buf);
            if (addrIndex >= 0 && addrIndex <= portIndex + 6) {
                final int uidPos = indexOfSpace(buf, addrIndex, 5);
                final String uidStr = buf.substring(uidPos, uidPos + 6).trim();

                if (Settings.DEBUG)
                    L.d(Settings.TAG_NETLINE, "localPort = " + locPort + " addr = " + addr + " UID = " + uidStr);

                try {
                    return Integer.parseInt(uidStr);
                } catch (NumberFormatException ex) {
                    return -1;
                }
            }
        }

        if (Settings.DEBUG) {
            L.d(Settings.TAG_NETLINE, buf);
            L.d(Settings.TAG_NETLINE, "localPort = " + locPort + " addr = " + addr);
        }
        return -1;
    }

    public static int findUidInTcp6(String buf, byte[] serverIp, int serverPort, int localPort) {
//		  if (DEBUG) L.d(Settings.TAG_NETLINE, buf);

        String locPort = getOnlyPortString(localPort);
        final String addr = getIpPortString(serverIp, serverPort, true);

        int portIndex = 0;
        while ((portIndex = LibNative.asciiIndexOf(locPort, portIndex + 1, buf)) >= 0) {
            final int addrIndex = LibNative.asciiIndexOf(addr, portIndex, buf);
            if (addrIndex >= 0 && addrIndex <= portIndex + 6) {
                final int uidPos = indexOfSpace(buf, addrIndex, 5);
                final String uidStr = buf.substring(uidPos, uidPos + 6).trim();

                if (Settings.DEBUG)
                    L.d(Settings.TAG_NETLINE, "localPort = " + locPort + " addr = " + addr + " UID = " + uidStr);

                try {
                    return Integer.parseInt(uidStr);
                } catch (NumberFormatException ex) {
                    return -1;
                }
            }
        }

        if (Settings.DEBUG) {
            L.d(Settings.TAG_NETLINE, buf);
            L.d(Settings.TAG_NETLINE, "localPort = " + locPort + " addr = " + addr);
        }

        return -1;
    }

    private static String getIpPortString(byte[] ip, int port, boolean ipv6) {
        byte[] buf;
        int pos = 0;

        if (ipv6) {
            buf = new byte[24 + 13];

            for (int i = 0; i < 16; i++)
                buf[pos++] = '0';
            for (int i = 0; i < 4; i++)
                buf[pos++] = 'F';
            for (int i = 0; i < 4; i++)
                buf[pos++] = '0';

            for (int i = ip.length - 1; i >= 0; i--) {
                byte b = ip[i];
                buf[pos++] = getByteFromInt((b >> 4) & 0x0F);
                buf[pos++] = getByteFromInt(b & 0x0F);
            }
            buf[pos++] = ':';

            buf[pos++] = getByteFromInt((port >> 12) & 0x0F);
            buf[pos++] = getByteFromInt((port >> 8) & 0x0F);
            buf[pos++] = getByteFromInt((port >> 4) & 0x0F);
            buf[pos] = getByteFromInt(port & 0x0F);
        } else {
            buf = new byte[13];

            for (int i = ip.length - 1; i >= 0; i--) {
                byte b = ip[i];
                buf[pos++] = getByteFromInt((b >> 4) & 0x0F);
                buf[pos++] = getByteFromInt(b & 0x0F);
            }
            buf[pos++] = ':';

            buf[pos++] = getByteFromInt((port >> 12) & 0x0F);
            buf[pos++] = getByteFromInt((port >> 8) & 0x0F);
            buf[pos++] = getByteFromInt((port >> 4) & 0x0F);
            buf[pos] = getByteFromInt(port & 0x0F);
        }

        String p = new String(buf, 0, 0, pos);
        return p;
    }

    private static String getOnlyPortString(int port) {
        byte[] buf = new byte[5];
        int pos = 0;

        buf[pos++] = ':';
        buf[pos++] = getByteFromInt((port >> 12) & 0x0F);
        buf[pos++] = getByteFromInt((port >> 8) & 0x0F);
        buf[pos++] = getByteFromInt((port >> 4) & 0x0F);
        buf[pos] = getByteFromInt(port & 0x0F);

        String p = new String(buf, 0, 0, pos);
        return p;
    }

    private static char getCharFromInt(int halfByte) {
        if (halfByte < 10)
            return (char) ('0' + halfByte);
        else
            return (char) ('A' - 10 + halfByte);
    }

    private static byte getByteFromInt(int halfByte) {
        if (halfByte < 10)
            return (byte) ('0' + halfByte);
        else
            return (byte) ('A' - 10 + halfByte);
    }

    public static int indexOfSpace(String buf, int start, int count) {
        int pos = start;
        int spaceCount = 0;
        boolean counted = false;

        while (pos < buf.length()) {
            char ch = buf.charAt(pos);
            boolean isSpace = (ch == ' ' || ch == '\t');

            if (!counted && isSpace) {
                spaceCount++;
                if (count == spaceCount)
                    return pos;

                counted = true;
            } else {
                if (counted && !isSpace)
                    counted = false;
            }

            pos++;
        }

        return -1;
    }
}
