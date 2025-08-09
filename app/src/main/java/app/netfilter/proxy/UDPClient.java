package app.netfilter.proxy;

import android.net.VpnService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import app.analytics.FirebaseAnalyticsSDK;
import app.common.Utils;
import app.common.debug.L;
import app.internal.Settings;
import app.netfilter.IFilterVpnPolicy;
import app.netfilter.debug.ClientEvent;
import app.netfilter.dns.DNSRequest;
import app.netfilter.dns.DNSResponse;
import app.security.PolicyRules;

public class UDPClient implements IClient {

    private static final int READ_TIMEOUT = 10000;
    private static final int DNS_TIMEOUT = 3000;
    private static final int LAST_DNS_RESPONSES_SIZE = 13;

    public static final int DNS = 53;

    private final byte[] localIp;
    private final int localPort;
    private final byte[] serverIp;
    private final int serverPort;

    private final String[] pkgs;
    private final Selector selector;
    private final VpnService vpn;
    private final ProxyWorker worker;
    private DatagramChannel chan = null;
    private int uid = 0;
    private IFilterVpnPolicy policy = null;
    private long timeoutAbsTime = 0;
    private int dnsRequestsSent = 0;
    private int dnsErrorsReceived = 0;
    private DNSRequest request;
    private boolean isDead = false;

    private static ByteBuffer readBuffer;
    private static ByteBuffer writeBuffer;

    // dns servers to forward DNS requests (simultaneously)
    private static ArrayList<InetAddress> dnsAddresses;
    // save here info about responses from DNS servers, to send client only first response
    private static ArrayDeque<Long> lastDnsResponses = new ArrayDeque<Long>(LAST_DNS_RESPONSES_SIZE);

    private static AtomicInteger countRef = new AtomicInteger(); // stats for server

    static {
        // https://www.securecoding.cert.org/confluence/display/java/OBJ10-J.+Do+not+use+public+static+nonfinal+variables
        for (int i = 0; i < LAST_DNS_RESPONSES_SIZE; i++)
            lastDnsResponses.add(Long.valueOf(0));
    }

    private UDPClient(VpnService service, ProxyWorker worker, Selector selector, int uid, byte[] serverIp, int serverPort,
                      byte[] localIp, int localPort) {
        this.vpn = service;
        this.worker = worker;
        this.selector = selector;
        this.pkgs = null; // It still cannot be found.
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.localIp = localIp;
        this.localPort = localPort;
        this.uid = uid;
        this.policy = worker.getPolicy();

        if (readBuffer == null) {
            readBuffer = ByteBuffer.allocate(PacketPool.POOL4_PACKET_SIZE);
            writeBuffer = ByteBuffer.allocate(PacketPool.POOL4_PACKET_SIZE);
        }

        countRef.getAndIncrement();
    }

    public static UDPClient newClient(VpnService service, ProxyWorker worker, Selector selector, int uid,
                                      byte[] serverIp, int serverPort, byte[] localIp, int localPort) {
        return new UDPClient(service, worker, selector, uid, serverIp, serverPort, localIp, localPort);
    }

    public static void setDNSServers(ArrayList<String> servers) {
        try {
            dnsAddresses = new ArrayList<InetAddress>();
            for (String s : servers) {
                //L.d(Settings.TAG_UDPCLIENT, "Local DNS: " + s);
                dnsAddresses.add(InetAddress.getByName(s));
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static void setDNSServers(String[] servers) {
        try {
            dnsAddresses = new ArrayList<InetAddress>();
            for (String s : servers) {
                //L.d(Settings.TAG_UDPCLIENT, "Local DNS: " + s);
                dnsAddresses.add(InetAddress.getByName(s));
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static void addDNSServers(ArrayList<String> servers) {
        try {
            for (String s : servers) {
                //L.d(Settings.TAG_UDPCLIENT, "Local DNS: " + s);
                dnsAddresses.add(InetAddress.getByName(s));
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static void addDNSServers(String[] servers) {
        try {
            for (String s : servers) {
                //L.d(Settings.TAG_UDPCLIENT, "Local DNS: " + s);
                dnsAddresses.add(InetAddress.getByName(s));
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public boolean allowConnection() {
        try {
            chan = DatagramChannel.open();
            chan.socket().bind(null);
            chan.configureBlocking(false);

            if (vpn.protect(chan.socket())) {
                chan.register(selector, 1, this);
                //L.d(Settings.TAG_UDPCLIENT, "UDPClient count = ", Integer.toString(clientCount));
                return true;
            } else {
                throw new IOException("Channel protect failed");
            }
        } catch (IOException e) {
            if (Settings.DEBUG) {
                L.d(Settings.TAG_UDPCLIENT, e.toString(), " (", Utils.ipToString(serverIp, serverPort), ")");
                if (pkgs != null && pkgs.length > 0)
                    L.d(Settings.TAG_UDPCLIENT, pkgs[0]);
            }
        }

        return false;
    }

    public boolean denyConnection() {
        return true;
    }

    public void destroy(boolean silent) {
        //L.d(Settings.TAG_UDPCLIENT, "destroy()");
        setDead();
        sockClose();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        countRef.getAndDecrement();
    }

    @Override
    public boolean isDead() {
        return isDead;
    }

    @Override
    public void setDead() {
        isDead = true;
    }

    public int getUid() {
        return uid;
    }

    public ClientEvent getEvent() {
        return null;
    }

    public byte[] getLocalIp() {
        return localIp;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String[] getPkgs() {
        return pkgs;
    }

    public int getProtocolNo() {
        return 17;
    }

    public byte[] getServerIp() {
        return serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public boolean isPending() {
        return false;
    }

    @Override
    public boolean isProxied() {
        return false;
    }

    public static int getRefCount() {
        return countRef.get();
    }

    // DNS spoofing prevention mechanism
    private boolean isValidResponse(InetSocketAddress addr) {
        boolean valid = true;
        if (serverPort == DNS && addr != null) {
            if (serverPort != addr.getPort())
                valid = false;

            InetAddress inetAddr = addr.getAddress();
            byte[] ip = null;
            if (inetAddr != null)
                ip = inetAddr.getAddress();

            if (ip != null && Arrays.equals(ip, serverIp))
                return true;

            // checking our additional DNS servers
            boolean additionalValid = false;
            if (dnsAddresses != null && dnsAddresses.size() > 0) {
                int size = dnsAddresses.size();
                for (int i = 0; i < size; i++) {
                    InetAddress a = dnsAddresses.get(i);
                    if (Arrays.equals(a.getAddress(), addr.getAddress().getAddress()))
                        additionalValid = true;
                }
                valid = valid && additionalValid;
            }
        }

        return valid;
    }

    public boolean onSockEvent(SelectionKey selKey) {
        //L.d(Settings.TAG_UDPCLIENT, "Client: " + this);

        if (isDead)
            return false;

        try {
            if (selKey.isValid() && selKey.isReadable()) {
                boolean valid = true;
                boolean local = false;

                try {
                    synchronized (vpn) {
                        if (chan != null) {
                            InetSocketAddress addr = (InetSocketAddress) chan.receive(readBuffer);
                            if (addr != null) {
                                if (Settings.DEBUG)
                                    L.d(Settings.TAG_UDPCLIENT, "Answer from: ", addr.toString());
                                valid = isValidResponse(addr);
                                if (valid)
                                    local = addr.getAddress().isSiteLocalAddress();
                            }
                        }
                    }

                    if (readBuffer.position() == 0) {
                        L.i(Settings.TAG_UDPCLIENT, "onSockEvent: read no data");

                        sockClose();
                        return false;
                    }
                } catch (IOException e) {
                    if (Settings.DEBUG)
                        L.i(Settings.TAG_UDPCLIENT, "onSockEvent: read error ", e.toString());

                    sockClose();
                    return false;
                }

                if (valid) {
                    Packet packet = PacketPool.alloc(readBuffer.position() + 28);
                    int size = Math.min(readBuffer.position(), packet.frame.length - 28);
                    readBuffer.flip();
                    readBuffer.get(packet.frame, 28, size);
                    packet.addIpUdpHeader(serverIp, serverPort, localIp, localPort, size);

                    final boolean dns = (serverPort == DNS);
                    boolean written = false;

                    if (dns && policy != null) {
                        DNSResponse resp = new DNSResponse(packet);

                        if (!resp.parsed) {
                            dnsErrorsReceived++;

                            if (resp.readNameError && request != null) {
                                if (Settings.EVENTS_LOG) {

                                }
                            }
                        }

                        // the last erroneous dns response needs to be sent to the client (TODO XXX wtf???)
                        //if (resp.parsed || (dnsErrorsReceived == dnsRequestsSent - 1) || (dnsErrorsReceived > 1 && local))
                        if (resp.parsed) {
                            if (wasDnsResponse(resp)) {
                                // already send to client response
                                written = true;
                                //L.a(Settings.TAG_UDPCLIENT, "dublicate");
                            } else {
                                addDnsResponse(resp);

                                PolicyRules rules = policy.getPolicy(resp);

                                if (rules != null && rules.hasPolicy(PolicyRules.DROP))

                                    // TODO XXX in result invalid dns response
                                    // in chrome: DNS_PROBE_FINISHED_NXDOMAIN instead of ERR_NAME_NOT_RESOLVED
                                    //
                                    // TODO XXX no notification if block malware or fraud
                                    resp.clearAnswers();

                                if (resp.modified) {
                                    worker.writeToTun(resp.getPacket());
                                    written = true;
                                }
                            }
                        }
                    }

                    if (written)
                        PacketPool.release(packet);
                    else
                        worker.writeToTun(packet);

                    updateTimeout(dns ? DNS_TIMEOUT : READ_TIMEOUT); // timeout to destroy client?
                    readBuffer.clear();
                } else {
                    L.d(Settings.TAG_UDPCLIENT, "DNS Spooffing attack was blocked!");
                }
            }
        } catch (CancelledKeyException e) {
            return false;
        }

        return true;
    }

    // return true if timeout
    public boolean onTimeout(long currentTime) {
        if (this.timeoutAbsTime == 0 || currentTime < this.timeoutAbsTime)
            return false;

        //final long timeout = System.currentTimeMillis() - lastReadTime;
        //final boolean clientAlive = !(lastReadTime != 0 && timeout >= READ_TIMEOUT);
        return true;
    }

    public void clearTimeout() {
        this.timeoutAbsTime = 0;
    }

    public void updateTimeout(long timeout) {
        this.timeoutAbsTime = System.currentTimeMillis() + timeout;
        worker.setTimeout(this.timeoutAbsTime);
    }

    // return absolute timeout time
    public long getTimeout() {
        return this.timeoutAbsTime;
    }

    public boolean onTunInput(Packet packet) {
        if (isDead || packet.length < 8)
            return false;

        if (Settings.DEBUG_NET)
            L.a(Settings.TAG_UDPCLIENT, "UDPPacket length = ", Integer.toString(packet.length));

        synchronized (vpn) {
            if (chan == null) {
                L.e(Settings.TAG_UDPCLIENT, "Trying to write to closed channel!");
                return false;
            }

            writeBuffer.put(packet.frame, packet.ipHeaderLen + 8, packet.length - 8);
            writeBuffer.flip();
            try {
                if (!Utils.ip4Cmp(packet.dstIp, Settings.DNS_FAKE_IP_AR)) {
                    InetSocketAddress addr = new InetSocketAddress(InetAddress.getByAddress(packet.dstIp), packet.dstPort);
                    while (writeBuffer.hasRemaining())
                        chan.send(writeBuffer, addr);
                    writeBuffer.flip();
                }

                if (packet.dstPort == 53 && dnsAddresses != null && dnsAddresses.size() > 0) {
                    // TODO XXX hmm, if packet to 53 port but not for 1.1.1.1?

                    int size = dnsAddresses.size();
                    for (int i = 0; i < size; i++) {
                        InetAddress addr = dnsAddresses.get(i);
                        //L.w(Settings.TAG_UDPCLIENT, "DNS: ", addr.toString());

                        // if webdefender is updated while connected to wifi it gets local dns as 1.1.1.1
                        // MyUpdaterReceiver will reconnect wifi and refresh local DNS

                        if (Utils.ip4Cmp(addr.getAddress(), Settings.DNS_FAKE_IP_AR))
                            continue;

                        sendTo(addr, packet.dstPort, writeBuffer);
                        dnsRequestsSent++;
                        writeBuffer.flip();
                    }
                    //L.w(Settings.TAG_UDPCLIENT, "dnsRequestsSent = ", Integer.toString(dnsRequestsSent));
                }

                writeBuffer.clear();
                updateTimeout(READ_TIMEOUT);
                return true;
            } catch (IOException e) {
                if (Settings.DEBUG)
                    L.e(Settings.TAG_UDPCLIENT, "onTunInput: failed to write packet to server ", e.toString());

                final String mes = e.getMessage();
                if (mes != null && (mes.contains("EACCES") || mes.contains("EPERM"))) {
                    FirebaseAnalyticsSDK.LogWarning("UDPClient onPermissionDenied");
                }

                sockClose();
                writeBuffer.clear();
                updateTimeout(1000); // clear on timeout?
            }

            return false;
        }
    }

    private void sendTo(InetAddress address, int port, ByteBuffer buf) throws IOException {
        InetSocketAddress addr = new InetSocketAddress(address, port);
        while (buf.hasRemaining())
            chan.send(buf, addr);
    }

    // return true if closed connection was open
    private boolean sockClose() {
        try {
            synchronized (vpn) {
                setDead(); // isStale = true;
                if (chan != null) {
                    chan.close();
                    chan = null;
                    //L.d(Settings.TAG_UDPCLIENT, "UDPClient count = ", Integer.toString(clientCount));

                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public void setRequest(DNSRequest request) {
        this.request = request;
    }

    static private void addDnsResponse(DNSResponse response) {
        lastDnsResponses.poll();
        lastDnsResponses.add(Long.valueOf(response.getHash()));
    }

    static private boolean wasDnsResponse(DNSResponse response) {
        return lastDnsResponses.contains(Long.valueOf(response.getHash()));
    }

    public void dump() {
        if (Settings.DEBUG) {
            String[] v1 = toString().split("\n");
            int v2 = 0;
            while (v2 < v1.length) {
                L.i(Settings.TAG_UDPCLIENT, v1[v2].trim());
                v2 = (v2 + 1);
            }
        }
    }

    public String toString() {
        if (Settings.DEBUG) {
            StringBuilder sb = new StringBuilder().append("").append("isPending: ").append(isPending()).append("\n").append("chan: ");
            String channel;
            synchronized (vpn) {
                if (chan == null)
                    channel = "(none)";
                else
                    channel = chan.toString();
            }
            return sb.append(channel).append("\n").append(super.toString()).append("localPort: ").append(localPort).toString();
        }

        return "";
    }
}
