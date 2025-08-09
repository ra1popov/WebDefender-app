package app.netfilter.dns;

import androidx.annotation.NonNull;

import app.common.LibNative;
import app.common.Utils;
import app.common.debug.L;
import app.internal.Settings;
import app.netfilter.proxy.Packet;

public class DNSRequest {

    private static final boolean NORMALIZE_DATA = true; // convert to lowercase domain names

    private int clientPort = 0;
    public String[] domains = null;
    public int transactionID = 0; // 16-bit field identifying a specific DNS transaction
    public int flags = 0;
    int[] requestTypes = null;
    int questions = 0;
    int answers = 0;
    int authorities = 0;
    int additional = 0;
    public boolean parsed = false;

    public DNSRequest(Packet packet) {
        if (!packet.parsed()) {
            if (!packet.parseFrame())
                return;
        }

        byte[] buf = packet.getData();
        if (buf == null || buf.length <= 12)
            return;

        clientPort = packet.srcPort;

        transactionID = ((0xFF & buf[0]) << 8) + (0xFF & buf[1]);
        flags = ((0xFF & buf[2]) << 8) + (0xFF & buf[3]);
        questions = ((0xFF & buf[4]) << 8) + (0xFF & buf[5]);
        answers = ((0xFF & buf[6]) << 8) + (0xFF & buf[7]);
        authorities = ((0xFF & buf[8]) << 8) + (0xFF & buf[9]);
        additional = ((0xFF & buf[10]) << 8) + (0xFF & buf[11]);

        if (flags == 0x0100) {
            parseQuestions(buf);
            parsed = true;

            if (Settings.DEBUG_DNS) L.a(Settings.TAG_DNSREQUEST, "Requesting: " + domains[0]);
        }
    }

    // TODO XXX compare with DNSResponse.parseQuestions
    private void parseQuestions(byte[] buf) {
        domains = new String[questions];
        requestTypes = new int[questions];

        // questions start at 12
        int off = 12;

        // TODO XXX check this!
        for (int i = 0; i < questions; i++) {
            StringBuilder sb = new StringBuilder(16);

            int len = (buf[off] & 0xFF);
            while (len != 0) {
                if (len < 128) {
                    off++;
                    for (int c = 0; c < len; c++) {
                        if (Settings.DEBUG)
                            // TODO XXX crash on sb.append!!!
                            if (off + c > buf.length - 1)
                                L.e(Settings.TAG_DNSREQUEST, "Error in parse! ", sb.toString());

                        sb.append((char) buf[off + c]);
                    }
                    off += len;
                }

                len = (buf[off] & 0xFF);
                if (len != 0)
                    sb.append('.');
            }

            domains[i] = sb.toString();
            if (NORMALIZE_DATA) {
                if (domains[i] != null)
                    domains[i] = LibNative.asciiToLower(domains[i]);
            }

            requestTypes[i] = ((0xFF & buf[off]) << 8) + (0xFF & buf[off + 1]);
            off += 4; // class is ignored
        }

        if (Settings.DEBUG_DNS) L.a(Settings.TAG_DNSREQUEST, "this: " + this.toString());
    }

    // prepare packet with empty DNS response to send on DNS block, see ProxyWorker
    // TODO XXX is this function create valid packet???
    public DNSResponse getEmptyResponse(Packet packet) {
        return new DNSResponse(transactionID, domains, packet.dstIp, packet.dstPort, packet.srcIp, packet.srcPort);
    }

    public long getHash() {
        return clientPort * 100000L + transactionID;
    }

    @NonNull
    @Override
    public String toString() {
        return Utils.concatStrings(domains);
    }

}
