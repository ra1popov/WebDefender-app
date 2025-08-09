package app.netfilter.debug;

import app.common.debug.L;
import app.internal.Settings;

public class ProxyEvent {
    public int interrupted = 0;
    public int notifyUdpTimeout = 0;
    public int selectUnblocked = 0;
    public int socketEvent = 0;
    public int tcpClientCreated = 0;
    public int tcpClientRemoved = 0;
    public int tcpSynAllowed = 0;
    public int tcpSynDenied = 0;
    public int tcpSynPending = 0;
    public int tcpSynPendingDuplicate = 0;
    public int tcpSynRetransmission = 0;
    public int tunInputPacket = 0;
    public int tunNoNetInfoForSyn = 0;
    public int tunPacketInvalid = 0;
    public int tunPacketParseFailed = 0;
    public int tunPacketTcp = 0;
    public int tunPacketTcpNoClient = 0;
    public int tunPacketTcpNoClientWithFin = 0;
    public int tunPacketTcpSyn = 0;
    public int tunPacketUdp = 0;
    public int tunReadQueueProcessed = 0;
    public int udpClientCreated = 0;
    public int udpClientRemoved = 0;
    public int unknownProtocol = 0;

    public ProxyEvent() {
        super();
    }

    public void dump() {
        if (Settings.DEBUG) {
            String[] lines;
            lines = toString().split("\n");

            for (String line : lines) {
                L.i(Settings.TAG_PROXYEVENT, line.trim());
            }
        }
    }

    public String toString() {
        if (Settings.DEBUG) {
            StringBuilder sb;
            sb = new StringBuilder();
            sb.append("ProxyEvent:\n");
            sb.append("  tunInputPacket:").append(tunInputPacket).append("\n");
            sb.append("  tunReadQueueProcessed:").append(tunReadQueueProcessed).append("\n");
            sb.append("    tunPacketInvalid:").append(tunPacketInvalid).append("\n");
            sb.append("    tunPacketParseFailed:").append(tunPacketParseFailed).append("\n");
            sb.append("\n");
            sb.append("  selectUnblocked:").append(selectUnblocked).append("\n");
            sb.append("    socketEvent:").append(socketEvent).append("\n");
            sb.append("    notifyUdpTimeout:").append(notifyUdpTimeout).append("\n");
            sb.append("    interrupted:").append(interrupted).append("\n");
            sb.append("\n");
            sb.append("  tunPacketTcp:").append(tunPacketTcp).append("\n");
            sb.append("    tunPacketTcpSyn:").append(tunPacketTcpSyn).append("\n");
            sb.append("		 tunNoNetInfoForSyn:").append(tunNoNetInfoForSyn).append("\n");
            sb.append("		 tcpSynDenied:").append(tcpSynDenied).append("\n");
            sb.append("		 tcpClientCreated:").append(tcpClientCreated).append("\n");
            sb.append("		 tcpClientRemoved:").append(tcpClientRemoved).append("\n");
            sb.append("		 tcpSynAllowed:").append(tcpSynAllowed).append("\n");
            sb.append("		 tcpSynPending:").append(tcpSynPending).append("\n");
            sb.append("		 tcpSynPendingDuplicate:").append(tcpSynPendingDuplicate).append("\n");
            sb.append("		 tcpSynRetransmission:").append(tcpSynRetransmission).append("\n");
            sb.append("    tunPacketTcpNoClient:").append(tunPacketTcpNoClient).append("\n");
            sb.append("    tunPacketTcpNoClientWithFin:").append(tunPacketTcpNoClientWithFin).append("\n");
            sb.append("\n");
            sb.append("  tunPacketUdp:").append(tunPacketUdp).append("\n");
            sb.append("    udpClientCreated:").append(udpClientCreated).append("\n");
            sb.append("    udpClientRemoved:").append(udpClientRemoved).append("\n");
            sb.append("\n");
            sb.append("  unknownProtocol:").append(unknownProtocol).append("\n");
            return sb.toString();
        }
        return "";
    }
}
