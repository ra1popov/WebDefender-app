package app.netfilter.proxy;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;


public class ClientList {

    // TODO XXX not thread safe. save ClientParams in deque.

    private final HashMap<ClientParams, IClient> map = new HashMap<ClientParams, IClient>();

    private ClientParams last = null;
    private IClient lastClient = null;

    private ClientParams getKey(byte[] dstIp, int dstPort, byte[] srcIp, int srcPort, int protocol) {
        byte[] data = new byte[17];

        data[0] = dstIp[0];
        data[1] = dstIp[1];
        data[2] = dstIp[2];
        data[3] = dstIp[3];

        data[4] = srcIp[0];
        data[5] = srcIp[1];
        data[6] = srcIp[2];
        data[7] = srcIp[3];

        data[8] = ((byte) (dstPort >> 24));
        data[9] = ((byte) (dstPort >> 16));
        data[10] = ((byte) (dstPort >> 8));
        data[11] = ((byte) dstPort);

        data[12] = ((byte) (srcPort >> 24));
        data[13] = ((byte) (srcPort >> 16));
        data[14] = ((byte) (srcPort >> 8));
        data[15] = ((byte) srcPort);

        data[16] = ((byte) protocol);

        return (new ClientParams(data));
    }

    public IClient get(Packet paramPacket) {
        ClientParams params = getKey(paramPacket.dstIp, paramPacket.dstPort,
                paramPacket.srcIp, paramPacket.srcPort,
                paramPacket.protocol);

        if (last != null && last.equals(params))
            return lastClient;

        last = params;
        lastClient = (IClient) this.map.get(params);
        return lastClient;
    }

    public int getCount() {
        return this.map.size();
    }

    public Iterator<IClient> getIterator() {
        return this.map.values().iterator();
    }

    public void put(IClient client) {
        ClientParams params = getKey(client.getServerIp(), client.getServerPort(),
                client.getLocalIp(), client.getLocalPort(),
                client.getProtocolNo());
        this.map.put(params, client);
        last = params;
        lastClient = client;
    }

    public void remove(IClient client) {
        ClientParams params = getKey(client.getServerIp(), client.getServerPort(),
                client.getLocalIp(), client.getLocalPort(),
                client.getProtocolNo());
        this.map.remove(params);
        if (last != null) {
            last = null;
            lastClient = null;
        }
    }

    public void clear(ProxyWorker worker) {
        final Collection<IClient> clients = map.values();
        for (IClient client : clients) {
            worker.clearTimeout(client);
            client.destroy(false);
        }

        map.clear();
        if (last != null) {
            last = null;
            lastClient = null;
        }
    }

    //
    public static final class ClientParams {
        private final byte[] data;

        public ClientParams(byte[] params) {
            if (params == null)
                throw new NullPointerException();

            this.data = params;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ClientParams))
                return false;

            byte[] data0 = this.data;
            byte[] data1 = ((ClientParams) obj).data;

            if (data0 == data1 || (
                    data0[16] == data1[16] &&                                                                                // protocol
                            data0[12] == data1[12] && data0[13] == data1[13] && data0[14] == data1[14] && data0[15] == data1[15] && // src port
                            data0[8] == data1[8] && data0[9] == data1[9] && data0[10] == data1[10] && data0[11] == data1[11] &&        // dst port
                            data0[4] == data1[4] && data0[5] == data1[5] && data0[6] == data1[6] && data0[7] == data1[7] &&            // src ip
                            data0[0] == data1[0] && data0[1] == data1[1] && data0[2] == data1[2] && data0[3] == data1[3]            // dst ip
            )) {
                return true;
            }

            return false;
        }

        public int hashCode() {
            return Arrays.hashCode(this.data);
        }
    }
}
