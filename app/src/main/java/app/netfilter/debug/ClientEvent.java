package app.netfilter.debug;

import app.common.debug.L;
import app.internal.Settings;

public class ClientEvent {
    public int allowed;
    public int closeGracefully;
    public int connected;
    public int denied;
    public int finishConnectFailed;
    public int invalidAckNo;
    public int ioexception;
    public int onTunInput;
    public int receivedRst;
    public int receivedSeqnoHasData;
    public int receivedSeqnoHasNoData;
    public int receivedSeqnoMatch;
    public int receivedSeqnoMismatch;
    public int retransmit;
    public int sockEventConnectable;
    public int sockEventReadable;
    public int sockEventWritable;
    public int sockRead;
    public int sockReadZero;
    public int sockShutdown;
    public int tunInputAfterFinReceived;
    public int tunWrite;

    public ClientEvent() {
        super();
        sockEventConnectable = 0;
        sockEventReadable = 0;
        sockEventWritable = 0;
        finishConnectFailed = 0;
        tunWrite = 0;
        allowed = 0;
        denied = 0;
        connected = 0;
        onTunInput = 0;
        receivedRst = 0;
        receivedSeqnoMatch = 0;
        receivedSeqnoHasData = 0;
        receivedSeqnoHasNoData = 0;
        closeGracefully = 0;
        receivedSeqnoMismatch = 0;
        tunInputAfterFinReceived = 0;
        retransmit = 0;
        invalidAckNo = 0;
        ioexception = 0;
        sockShutdown = 0;
        sockReadZero = 0;
        sockRead = 0;
    }

    public void dump() {
        if (Settings.DEBUG) {
            String[] r3;
            int i0, i1;
            r3 = this.toString().split("\n");
            i0 = r3.length;
            i1 = 0;

            while (i1 < i0) {
                L.i(Settings.TAG_CLIENTEVENT, r3[i1].trim());
                i1 = i1 + 1;
            }
        }
    }

    public String toString() {
        if (Settings.DEBUG) {
            StringBuilder r1;
            r1 = new StringBuilder();
            r1.append("sockEvent:\n");
            r1.append("  connect: ").append(sockEventConnectable).append("\n");
            r1.append("  read: ").append(sockEventReadable).append("\n");
            r1.append("  write: ").append(sockEventWritable).append("\n");
            r1.append("finishConnectFailed: ").append(finishConnectFailed).append("\n");
            r1.append("tunWrite: ").append(tunWrite).append("\n");
            r1.append("allowed: ").append(allowed).append("\n");
            r1.append("denied: ").append(denied).append("\n");
            r1.append("connected: ").append(connected).append("\n");
            r1.append("onTunInput: ").append(onTunInput).append("\n");
            r1.append("receivedRst: ").append(receivedRst).append("\n");
            r1.append("receivedSeqnoMatch: ").append(receivedSeqnoMatch).append("\n");
            r1.append("  receivedSeqnoHasData: ").append(receivedSeqnoHasData).append("\n");
            r1.append("  receivedSeqnoHasNoData: ").append(receivedSeqnoHasNoData).append("\n");
            r1.append("receivedSeqnoMismatch: ").append(receivedSeqnoMismatch).append("\n");
            r1.append("closeGracefully: ").append(closeGracefully).append("\n");
            r1.append("tunInputAfterFinReceive: ").append(tunInputAfterFinReceived).append("\n");
            r1.append("retransmit: ").append(retransmit).append("\n");
            r1.append("invalidAckNo: ").append(invalidAckNo).append("\n");
            r1.append("ioexception: ").append(ioexception).append("\n");
            r1.append("sockShutdown: ").append(sockShutdown).append("\n");
            r1.append("sockReadZero: ").append(sockReadZero).append("\n");
            r1.append("sockRead: ").append(sockRead).append("\n");
            return r1.toString();
        }
        return "";
    }
}
