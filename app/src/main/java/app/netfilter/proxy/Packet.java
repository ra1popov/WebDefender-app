package app.netfilter.proxy;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import app.common.LibNative;
import app.common.debug.L;
import app.internal.Settings;

public class Packet {

    public static final int IP_PROT_TCP = 6;
    public static final int IP_PROT_UDP = 17;

    public static final int IPTCP_HEADER_SIZE = 40;
    public static final int IPUDP_HEADER_SIZE = 28;
    public static final int TCP_BODY_SIZE_MAX = 1360;
    public static final int UDP_BODY_SIZE_MAX = 1372;

    static final int TCP_FLAG_ACK = 16;
    static final int TCP_FLAG_FIN = 1;
    static final int TCP_FLAG_PSH = 8;
    static final int TCP_FLAG_RST = 4;
    static final int TCP_FLAG_SYN = 2;

    private static AtomicInteger countRef = new AtomicInteger();
    public final byte[] frame;
    int ackNo;
    int dataOfs;
    public byte[] dstIp;
    public int dstPort;
    byte flag;
    int frameLen;
    int ipHeaderLen;
    int length;
    public int protocol;
    int seqNo;
    public byte[] srcIp;
    public int srcPort;
    int totalLen;
    int tcpCheckSum;
    int udpCheckSum;
    private int ipVer;

    public Packet(int frameSize) {
        super();
        frameLen = 0;
        dstIp = null;
        srcIp = null;
        frame = new byte[frameSize];
        countRef.getAndIncrement();
    }

    protected void finalize() throws Throwable {
        super.finalize();
        countRef.getAndDecrement();
    }

    public static int getRefCount() {
        return countRef.get();
    }

    public static int addIpTcpHeaderNative(byte[] data, byte[] sourceIp, int sourcePort,
                                           byte[] destIp, int destPort, int seqNo, int ackNo,
                                           int flags, int winSize, int dataSize) {
        return LibNative.addIpTcpHeaderNative(data, sourceIp, sourcePort, destIp, destPort,
                seqNo, ackNo, flags, winSize, dataSize);
    }

    public static short calcCheckSum(int startValue, byte[] buf, int offset, int length) {
        return LibNative.calcCheckSum(startValue, buf, offset, length);
    }

    public static short calcCheckSum_(int i0, byte[] r0, int i1, int i2) {

        int i5, i6, i21;
        i5 = i1 + (i2 & -2);
        i6 = i1;

        while (i6 < i5) {
            i0 = i0 + (r0[i6] & 0xFF << 8 | r0[i6 + 1] & 0xFF);
            i6 = i6 + 2;
        }

        if ((i2 & 1) != 0) {
            i0 = i0 + (r0[i6] & 0xFF << 8);
        }

        i21 = (i0 >> 16 & 65535) + (i0 & 65535);
        return (short) (i21 + (i21 >> 16 & 65535) ^ -1);
    }

    public static short calcIpCheckSum(byte[] buf, int offset, int length) {
        return Packet.calcCheckSum(0, buf, offset, length);
    }

    public static short calcTcpCheckSum(byte[] frame, int offset, int length, byte[] serverIp, byte[] localIp) {
        int startValue = 0 + (length + 6) +
                ((0xFF & serverIp[0]) << 8 | 0xFF & serverIp[1]) +
                ((0xFF & serverIp[2]) << 8 | 0xFF & serverIp[3]) +
                ((0xFF & localIp[0]) << 8 | 0xFF & localIp[1]) +
                ((0xFF & localIp[2]) << 8 | 0xFF & localIp[3]);
        return calcCheckSum(startValue, frame, offset, length);
    }

    private static short calcUdpCheckSum(byte[] paramArrayOfByte1, int paramInt1, int paramInt2,
                                         byte[] paramArrayOfByte2, byte[] paramArrayOfByte3) {
        int startValue = 0 + (paramInt2 + 17) +
                ((0xFF & paramArrayOfByte2[0]) << 8 | 0xFF & paramArrayOfByte2[1]) +
                ((0xFF & paramArrayOfByte2[2]) << 8 | 0xFF & paramArrayOfByte2[3]) +
                ((0xFF & paramArrayOfByte3[0]) << 8 | 0xFF & paramArrayOfByte3[1]) +
                ((0xFF & paramArrayOfByte3[2]) << 8 | 0xFF & paramArrayOfByte3[3]);
        return calcCheckSum(startValue, paramArrayOfByte1, paramInt1, paramInt2);
    }

    public static String flagToString(int flag) {
        String str = "";

        if ((flag & 0x2) != 0)
            str = str + "SYN ";
        if ((flag & 0x10) != 0)
            str = str + "ACK ";
        if ((flag & 0x1) != 0)
            str = str + "FIN ";
        if ((flag & 0x8) != 0)
            str = str + "PSH ";
        if ((flag & 0x4) != 0)
            str = str + "RST ";

        return str;
    }

    public void addIpTcpHeader(byte[] sourceIp, int sourcePort, byte[] destIp, int destPort,
                               int seqNo, int ackNo, int flags, int winSize, int dataSize) {
        frameLen = Packet.addIpTcpHeaderNative(frame, sourceIp, sourcePort, destIp, destPort,
                seqNo, ackNo, flags, winSize, dataSize);
        if (frameLen < 0)
            L.i(Settings.TAG_PACKET, "header failed");
    }

    public void addIpTcpHeader2(byte[] serverIp, int serverPort, byte[] localIp, int localPort,
                                int seqNo, int ackNo, int i4, int i5, int i6) {
        byte[] r3;
        short ipCheckSum, tcpCheckSum;

        if (ackNo == -1)
            ackNo = 0;

        frameLen = i6 + 40;
        r3 = frame;
        r3[0] = (byte) 69;
        r3[1] = (byte) 0;
        r3[2] = (byte) (frameLen >> 8);
        r3[3] = (byte) frameLen;
        r3[4] = (byte) 0;
        r3[5] = (byte) 0;
        r3[6] = (byte) 64;
        r3[7] = (byte) 0;
        r3[8] = (byte) 64;
        r3[9] = (byte) 6;
        r3[10] = (byte) 0;
        r3[11] = (byte) 0;
        r3[12] = serverIp[0];
        r3[13] = serverIp[1];
        r3[14] = serverIp[2];
        r3[15] = serverIp[3];
        r3[16] = localIp[0];
        r3[17] = localIp[1];
        r3[18] = localIp[2];
        r3[19] = localIp[3];
        ipCheckSum = Packet.calcIpCheckSum(r3, 0, 20);
        r3[10] = (byte) (ipCheckSum >> 8);
        r3[11] = (byte) ipCheckSum;
        r3[20] = (byte) (serverPort >> 8);
        r3[21] = (byte) serverPort;
        r3[22] = (byte) (localPort >> 8);
        r3[23] = (byte) localPort;
        r3[24] = (byte) (seqNo >> 24);
        r3[25] = (byte) (seqNo >> 16);
        r3[26] = (byte) (seqNo >> 8);
        r3[27] = (byte) seqNo;
        r3[28] = (byte) (ackNo >> 24);
        r3[29] = (byte) (ackNo >> 16);
        r3[30] = (byte) (ackNo >> 8);
        r3[31] = (byte) ackNo;
        r3[32] = (byte) 80;
        r3[33] = (byte) i4;
        r3[34] = (byte) (i5 >> 8);
        r3[35] = (byte) i5;
        r3[36] = (byte) 0;
        r3[37] = (byte) 0;
        r3[38] = (byte) 0;
        r3[39] = (byte) 0;
        tcpCheckSum = Packet.calcTcpCheckSum(r3, 20, i6 + 20, serverIp, localIp);
        r3[36] = (byte) (tcpCheckSum >> 8);
        r3[37] = (byte) tcpCheckSum;
    }

    public void addIpUdpHeader(byte[] serverIp, int serverPort, byte[] localIp, int localPort, int size) {
        short ipCheckSum, udpCheckSum;

        frameLen = size + 28;
        frame[0] = (byte) 69;
        frame[1] = (byte) 0;
        frame[2] = (byte) (frameLen >> 8);
        frame[3] = (byte) frameLen;
        frame[4] = (byte) 0;
        frame[5] = (byte) 0;
        frame[6] = (byte) 64;
        frame[7] = (byte) 0;
        frame[8] = (byte) 64;
        frame[9] = (byte) 17;
        frame[10] = (byte) 0;
        frame[11] = (byte) 0;
        frame[12] = serverIp[0];
        frame[13] = serverIp[1];
        frame[14] = serverIp[2];
        frame[15] = serverIp[3];
        frame[16] = localIp[0];
        frame[17] = localIp[1];
        frame[18] = localIp[2];
        frame[19] = localIp[3];
        ipCheckSum = Packet.calcIpCheckSum(frame, 0, 20);
        frame[10] = (byte) (ipCheckSum >> 8);
        frame[11] = (byte) ipCheckSum;
        frame[20] = (byte) (serverPort >> 8);
        frame[21] = (byte) serverPort;
        frame[22] = (byte) (localPort >> 8);
        frame[23] = (byte) localPort;
        frame[24] = (byte) (size + 8 >> 8);
        frame[25] = (byte) (size + 8);
        frame[26] = (byte) 0;
        frame[27] = (byte) 0;
        udpCheckSum = Packet.calcUdpCheckSum(frame, 20, size + 8, serverIp, localIp);
        frame[26] = (byte) (udpCheckSum >> 8);
        frame[27] = (byte) udpCheckSum;
    }

    public String toString() {
        if (Settings.DEBUG) {
            if (srcIp == null)
                return "Packet not parsed!";

            StringBuilder r5, r31;
            Object[] r6, r33;
            String res;
            r5 = (new StringBuilder()).append("");
            r6 = new Object[13];

            r6[0] = ipVer;
            r6[1] = protocol;
            r6[2] = totalLen;
            r6[3] = srcIp[0] & 0xFF;
            r6[4] = srcIp[1] & 0xFF;
            r6[5] = srcIp[2] & 0xFF;
            r6[6] = srcIp[3] & 0xFF;
            r6[7] = srcPort;
            r6[8] = dstIp[0] & 0xFF;
            r6[9] = dstIp[1] & 0xFF;
            r6[10] = dstIp[2] & 0xFF;
            r6[11] = dstIp[3] & 0xFF;
            r6[12] = dstPort;
            res = r5.append(String.format("ip=%d prot=%d total=%d %d.%d.%d.%d:%d -> %d.%d.%d.%d:%d", r6)).toString();

            if (protocol == IP_PROT_TCP) {
                r31 = (new StringBuilder()).append(res);
                r33 = new Object[4];
                r33[0] = seqNo;
                r33[1] = ackNo;
                r33[2] = flagToString(flag);
                r33[3] = totalLen - dataOfs;
                res = r31.append(String.format(" s=%04X a=%04X %s dlen=%d", r33)).toString();
                res += " chkSum=" + Integer.toHexString(tcpCheckSum);
            }

            return res;
        }

        return "";
    }

    public void dump() {
        if (Settings.DEBUG) L.i(Settings.TAG_PACKET, toString());
    }

    public boolean parseFrame() {
        boolean parsedOk = false;
        byte[] buf;

        buf = frame;
        ipVer = buf[0] >> 4 & 15;

        label_0:
        if (ipVer != 4) {
            if (ipVer != 6)
                L.i(Settings.TAG_PACKET, "unknown IP version");
            else
                L.i(Settings.TAG_PACKET, "IPv6 not supported");
        } else {
            ipHeaderLen = (4 * (0xF & buf[0]));
            totalLen = (((0xFF & buf[2]) << 8) | (0xFF & buf[3]));
            protocol = (0xFF & buf[9]);
            srcIp = Arrays.copyOfRange(buf, 12, 16);
            dstIp = Arrays.copyOfRange(buf, 16, 20);

            if (totalLen != frameLen) {
                if (Settings.DEBUG)
                    L.e(Settings.TAG_PACKET, "Packet length = ", Integer.toString(totalLen), ", but frameLen = ", Integer.toString(frameLen));
            }

            if (protocol == IP_PROT_TCP) {
                int j = ipHeaderLen;
                srcPort = (((0xFF & buf[j]) << 8) + (0xFF & buf[(j + 1)]));
                dstPort = (((0xFF & buf[(j + 2)]) << 8) + (0xFF & buf[(j + 3)]));
                seqNo = ((0xFF & buf[(j + 4)]) << 24 |
                        (0xFF & buf[(j + 5)]) << 16 |
                        (0xFF & buf[(j + 6)]) << 8 |
                        0xFF & buf[(j + 7)]);
                ackNo = ((0xFF & buf[(j + 8)]) << 24 |
                        (0xFF & buf[(j + 9)]) << 16 |
                        (0xFF & buf[(j + 10)]) << 8 |
                        0xFF & buf[(j + 11)]);
                dataOfs = (j + 4 * (0xF & buf[(j + 12)] >> 4));
                flag = buf[(j + 13)];
//				  b86 = buf[ipHeaderLen + 16];
//				  b90 = buf[ipHeaderLen + 17];
                tcpCheckSum = (((0xFF & buf[j + 16]) << 8) + (0xFF & buf[(j + 17)]));
            } else if (protocol == IP_PROT_UDP) {
                srcPort = (((0xFF & buf[ipHeaderLen]) << 8) + (0xFF & buf[(ipHeaderLen + 1)]));
                dstPort = (((0xFF & buf[(ipHeaderLen + 2)]) << 8) + (0xFF & buf[(ipHeaderLen + 3)]));
                length = (((0xFF & buf[(ipHeaderLen + 4)]) << 8) + (0xFF & buf[(ipHeaderLen + 5)]));
                udpCheckSum = (((0xFF & buf[ipHeaderLen + 6]) << 8) + (0xFF & buf[(ipHeaderLen + 7)])); /* TODO ? */
//				  b122 = buf[ipHeaderLen + 6];
//				  b126 = buf[ipHeaderLen + 7];
                dataOfs = ipHeaderLen + 8;

                if (frameLen < length + ipHeaderLen) {
                    L.i(Settings.TAG_PACKET, "data too short. cropped?");
                    break label_0;
                }
            } else {
                if (Settings.DEBUG) L.i(Settings.TAG_PACKET, "Unknown Protocol Num ", Integer.toString(protocol));
                break label_0;
            }

            parsedOk = true;
        }

        return parsedOk;
    }

    public boolean parsed() {
        return srcIp != null && dstIp != null && totalLen > 0;
    }

    public void clear() {
        srcIp = dstIp = null;
        srcPort = dstPort = 0;
        frameLen = totalLen = dataOfs = 0;
    }

    public byte[] getData() {
        if (frameLen == length + ipHeaderLen)
            return Arrays.copyOfRange(frame, dataOfs, frameLen);

        L.i(Settings.TAG_PACKET, "getData null");
        return null;
    }

    public byte[] getIpFrame() {
        return Arrays.copyOfRange(frame, 0, frameLen);
    }
}
