package app.netfilter.proxy;

import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import app.App;
import app.common.NetInfo;
import app.common.NetUtil;
import app.common.Utils;
import app.common.debug.EthernetFrame;
import app.common.debug.L;
import app.common.debug.PCapFileWriter;
import app.common.memdata.ByteBufferPool;
import app.internal.ProxyBase;
import app.internal.Settings;
import app.netfilter.FilterVpnService;
import app.netfilter.IFilterVpnPolicy;
import app.netfilter.IHasherListener;
import app.netfilter.IPacketLogger;
import app.netfilter.IUserNotifier;
import app.netfilter.debug.ProxyEvent;
import app.netfilter.dns.DNSRequest;
import app.security.PolicyRules;

public class ProxyWorker implements Runnable {
    private ProxyEvent ev;

    static final int FORCE_GC_CALL_COUNT = 50; // number of empty calls to selectNow to force GC
    private int gc_counter;

    static final int FORCE_TIMEOUT_CLIENT_COUNT = 200; // see timeoutsProcessing
    static final int FORCE_CLOSE_CLIENT_COUNT = 800;
    static final long FORCE_CLOSE_CLIENT_TIMEOUT = 15000;

    private final CountDownLatch latch = new CountDownLatch(3); // all 3 network threads must call onThreadFinished
    private final IThreadEventListener threadEventListener = new IThreadEventListener() {
        public void onThreadFinished(boolean isError) {
            ProxyWorker.this.latch.countDown();
            if (isError && ProxyWorker.this.latch.getCount() > 0L) // on error stop all other threads
                ProxyWorker.this.stop();
        }
    };

    private final ClientList clientList = new ClientList();
    private final FileDescriptor fd;
    private final ParcelFileDescriptor pfd;
    private final VpnService vpn;
    private Selector selector;
    private Set<SelectionKey> selectedKeys = new HashSet<SelectionKey>();
    private Thread thread;
    private TunReadThread tunReadThread = null;
    private TunWriteThread tunWriteThread = null;
    private IPacketLogger logger = null;
    private IFilterVpnPolicy policy = null;
    private IHasherListener hasherListener = null;
    private IUserNotifier userNotifier = null;

    private long nearestAbsTimeout; // nearest client timeout time
    private volatile boolean isSelectorWakeup = false;

    private volatile boolean blockingRuleChanged = false;
    private volatile boolean udpTimeoutNotified = false;
    private boolean needToCloseProxyConnections = false;
    private boolean needToCloseAllConnections = false;

    private long dataReadSize = 0; // stats for user
    private long dataWriteSize = 0; // stats for user
    private int tcpRefCount = 0; // stats for server
    private int udpRefCount = 0;

    private static PCapFileWriter pktDumper = null; // packet dumper (for debug) TODO XXX static!!!
    private static boolean pktDumpEnabled = false;
    private static final Object pktDumperLock = new Object();

    public ProxyWorker(VpnService vpnService, ParcelFileDescriptor parcelFileDescriptor, IFilterVpnPolicy policy,
                       IPacketLogger logger, IHasherListener hasherListener, IUserNotifier userNotifier) {
        ev = null;
        if (Settings.DEBUG_PROFILE_NET) ev = new ProxyEvent();

        this.vpn = vpnService;
        this.fd = parcelFileDescriptor.getFileDescriptor();
        this.pfd = parcelFileDescriptor; // from vpnBuilder.establish
        this.policy = policy;
        this.logger = logger;
        this.hasherListener = hasherListener;
        this.userNotifier = userNotifier;
    }

    private void processTunReadPacket(Packet packet) {
        if (Settings.DEBUG_PROFILE_NET) ev.tunInputPacket++;

        if (packet == null || packet.frameLen <= 0) {
            if (Settings.DEBUG_PROFILE_NET) ev.tunPacketInvalid++;
            return;
        }
        if (logger != null)
            logger.log(packet, false);

        if (!packet.parseFrame()) {
            if (Settings.DEBUG_PROFILE_NET) ev.tunPacketParseFailed++;
            return;
        }
        if (logger != null)
            logger.log(packet, true);

        switch (packet.protocol) {
            case Packet.IP_PROT_TCP:
                processTunReadPacketTCP(packet);
                break;

            case Packet.IP_PROT_UDP:
                processTunReadPacketUDP(packet);
                break;

            default:
                if (Settings.DEBUG_PROFILE_NET) ev.unknownProtocol++;
                //L.i(Settings.TAG_PROXYWORKER, "not even UDP. protocol=" + packet.protocol);
        }
    }

    private void processTunReadPacketTCP(Packet packet) {
        //L.d(Settings.TAG_PROXYWORKER, "TCP Packet: " + packet);
        if (Settings.DEBUG_PROFILE_NET) ev.tunPacketTcp++;

        IClient client = clientList.get(packet);

        if ((packet.flag & Packet.TCP_FLAG_SYN) == 0) {
            // next packet in TCP connection (not SYN)
            if (client == null) {
                // uups, no connection for this packet

                //L.d(Settings.TAG_PROXYWORKER, "Client for packet not found!");
                if (Settings.DEBUG_PROFILE_NET) ev.tunPacketTcpNoClient++;

                if ((packet.flag & Packet.TCP_FLAG_RST) != 0)
                    return; // receive unknown RST packet, skip

                Packet rst = TCPStateMachine.getRst0(packet);
                writeToTun(rst);

                if ((packet.flag & Packet.TCP_FLAG_FIN) != 0) {
                    if (Settings.DEBUG_PROFILE_NET) ev.tunPacketTcpNoClientWithFin++;
                }

                return;
            }

            // process packet in TCPClient

            //L.d(Settings.TAG_PROXYWORKER, "Client for packet found!");
            if (!client.onTunInput(packet)) {
                // error on packet processing, closing connection
                //L.d(Settings.TAG_PROXYWORKER, "Tun packet hasn't been transmitted! Client is being removed!");

                clientDestroy(client, null, false);

                // TODO XXX avoid sending RST to other packets to this client in queue
                // and what about not readed packets in tun?
            }

            return;
        }

        // new TCP connection (SYN packet has arrived)

        //L.d(Settings.TAG_PROXYWORKER, "Packet with SYN flag: " + packet);
        if (Settings.DEBUG_PROFILE_NET) ev.tunPacketTcpSyn++;

        if (client != null) {
            // found client!

            if (!client.isDead()) {
                // tcp syn retransmit
                if (Settings.DEBUG_PROFILE_NET) ev.tcpSynRetransmission++;
                return;
            }

            // client was marked as dead, close it (avoid send rst) and destroy
            clientDestroy(client, null, true);
        }

        int uid = 0;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            uid = NetInfo.findMatchingUidInTcp(packet.srcPort, packet.dstIp, packet.dstPort);
        } else {
            uid = NetInfo.findMatchingUidInTcpQ(packet.srcIp, packet.srcPort, packet.dstIp, packet.dstPort);
        }

        PolicyRules rules = (policy != null) ? policy.getPolicy(packet) : new PolicyRules();

        //L.d(Settings.TAG_PROXYWORKER, "uid for TCP: " + uid);

        // check by policy

        if (uid != 0 && policy != null) {
            //if (packet.dstPort == 443 || packet.dstPort == 9090) rules = new PolicyRules(PolicyRules.DROP); else
            //if (packet.dstPort == 443) rules = new PolicyRules(PolicyRules.DROP); else
            rules = policy.getPolicy(packet, uid);

            if (uid == -1)
                if (Settings.DEBUG) L.e(Settings.TAG_PROXYWORKER, "Packet: ", packet.toString());
        }

        // create new TCPClient

        do {
            if (rules.hasPolicy(PolicyRules.DROP)) {
                break;
            }

            // debug
            //String[] packages = LibNative.getNamesFromUid(uid);
            //if (packages != null && packages.length > 0 && !packages[0].equals("org.zwanoo.android.speedtest")) break;

            if (Settings.DEBUG_PROFILE_NET) ev.tcpClientCreated++;
            if (Settings.DEBUG_PROFILE_NET) ev.tcpSynAllowed++;

            client = TCPClient.newClient(vpn, this, selector, uid, packet.dstIp, packet.dstPort,
                    packet.srcIp, packet.srcPort, packet.seqNo);
            if (client.isDead())
                break;

            if (!client.allowConnection()) {
                clientDestroy(client, null, false);
                break;
            }

            clientList.put(client);

            final int ref = TCPClient.getRefCount();
            if (ref > tcpRefCount)
                tcpRefCount = ref;

            return; // all ok
        }
        while (false);

        // connection blocked or error, send reset

        if (rules.hasPolicy(PolicyRules.NOTIFY_USER) && userNotifier != null)
            userNotifier.notify(rules, packet.dstIp);

        Packet rst = TCPStateMachine.getRst(packet);
        writeToTun(rst);
    }

    private void processTunReadPacketUDP(Packet packet) {
        //L.d(Settings.TAG_PROXYWORKER, "UDP Packet: " + packet);
        if (Settings.DEBUG_PROFILE_NET) ev.tunPacketUdp++;

        IClient client = clientList.get(packet);

        while (client == null) {
            // new UDP connection (may contain only single packet)

            int uid = 0;

            DNSRequest req = null;

            // parse if DNS and check by policy

            if (policy != null) {
                PolicyRules rules = policy.getPolicy(packet); // check packet

                if (packet.dstPort == 53) {
                    req = new DNSRequest(packet);
                }

                // DNS check return only DROP or NOTIFY_USER so we can use isNormal() check
                if (uid != 0 && (rules == null || rules.isNormal()))
                    rules = policy.getPolicy(uid);              // check by UID

                if (rules.hasPolicy(PolicyRules.DROP)) {
                    // uups, drop connection

                    if (req != null) {
                        // send fake response if drop DNS request

                        Packet empty = req.getEmptyResponse(packet).getPacket();
                        writeToTun(empty);

                        // TODO XXX notify not only for dropped DNS requests
                        if (req.domains != null && req.domains.length > 0) {
                            if (rules.hasPolicy(PolicyRules.NOTIFY_USER) && userNotifier != null)
                                userNotifier.notify(rules, req.domains[0]);
                        }
                    }

                    break;
                }
            }

            // create new UDPClient

            if (Settings.DEBUG_PROFILE_NET) ev.udpClientCreated++;
            client = UDPClient.newClient(vpn, this, selector, uid, packet.dstIp, packet.dstPort,
                    packet.srcIp, packet.srcPort);
            if (req != null)
                ((UDPClient) client).setRequest(req);

            // TODO Make domain addition in the client to catch exceptions in DNS responses.

            if (!client.allowConnection()) {
                clientDestroy(client, null, false);
                //L.w(Settings.TAG_PROXYWORKER, packet.toString());
                return;
            }

            clientList.put(client);

            final int ref = UDPClient.getRefCount();
            if (ref > udpRefCount)
                udpRefCount = ref;

            break;
        }

        // process packet in UDPClient

        if (client != null && !client.onTunInput(packet))
            clientDestroy(client, null, false); // error on packet processing. maybe not destroy client?
    }

    public void writeToTun(Packet packet) {
        tunWriteThread.write(packet);
    }

    /*
     * destroy client and remove it from all lists
     * if silent == true client didn't send any network data to client (by tun)
     */
    private void clientDestroy(IClient client, Iterator<IClient> iterator, boolean silent) {
        boolean isUdp = (client.getProtocolNo() == Packet.IP_PROT_UDP);

        if (iterator != null)
            iterator.remove();
        else
            clientList.remove(client);

        clearTimeout(client);
        client.destroy(silent);

        if (Settings.DEBUG_PROFILE_NET) {
            if (isUdp) ev.udpClientRemoved++;
            else ev.tcpClientRemoved++;
        }
    }

    public void selectorWakeup() {
        if (!isSelectorWakeup) {
            isSelectorWakeup = true;
            //selector.wakeup();
        }

        // TODO XXX calls wakeup on every packet, even if isSelectorWakeup == true
        // very bad speedtest if call this only on !isSelectorWakeup

        if (selector.isOpen()) // check selector state. may be add exception handler instead?
            selector.wakeup();
    }

    public void notifyBlockingRuleChanged() {
        L.i(Settings.TAG_PROXYWORKER, "Blocking rule changed. notify");
        blockingRuleChanged = true;
        selectorWakeup();
    }

    public void notifyDnsResolved() {
        L.i(Settings.TAG_PROXYWORKER, "Dns resolved. notify");
        blockingRuleChanged = true;
        selectorWakeup();
    }

    // add to selected keys new key
    public void selectedKeysUpdate(SelectionKey key) {
        if (selectedKeys == null)
            return;

        selectedKeys.add(key);
    }

    public void run() {
        ProxyBase.setWorker(this);

        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        nearestAbsTimeout = 0;
        gc_counter = 0;

        // TODO XXX can we change read/write buffers for this fd?
        tunReadThread = TunReadThread.create(fd, this, threadEventListener, "TunReadThread");
        tunWriteThread = TunWriteThread.create(fd, this, threadEventListener, "TunWriteThread");

        Utils.maximizeThreadPriority();

        networkProcessingThread(); // loop
        //L.e("ProxyWorker", "Thread finished!");
    }

    // main network loop
    private void networkProcessingThread() {
        while (true) {
            // select ready sockets
            // first use 'fast' selectNow then if no ready sockets
            // use select with calculated timeout (or without)
            // and sometimes call GC (TODO XXX maybe not needed under ART?)

            dataWait();

            //
            if (Thread.currentThread().isInterrupted()) {
                // OOups! shutdown
                onNetworkProcessingEnd();
                return;
            }

            // get selected sockets in dataWait
            // keys list maybe updated while processing data from tun (see TcpClient.forceEvent)
            selectedKeys.addAll(selector.selectedKeys());
            selector.selectedKeys().clear();

            //

            clientProcessing();         // process input packets from tun
            tunWriteThread.flush();

            serverProcessing();         // process ready sockets
            tunWriteThread.flush();

            timeoutsProcessing();     // process timeouts
            tunWriteThread.flush();

            //

            selectedKeys.clear();

            if (Settings.DNS_IP_BLOCK_CLEAN)
                BlockIPCache.update();

            ProxyBase.updateServers(false);

        }
    }

    // actions if networkProcessingThread iterrupted
    private void onNetworkProcessingEnd() {
        if (Settings.DEBUG_PROFILE_NET) ev.interrupted++;

        L.i(Settings.TAG_PROXYWORKER, "Thread interrupted");
        L.i(Settings.TAG_PROXYWORKER, "ProxyWorker main thread finished");

        if (selector.isOpen())
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        clientList.clear(this); // TODO clear tunReadQueue and release Packets

        if (Settings.DNS_IP_BLOCK_CLEAN)
            BlockIPCache.clear();

        threadEventListener.onThreadFinished(false);
    }

    // select ready socket for processing (servers) or wait tun packets (clients). see networkProcessingThread
    private void dataWait() {
        try {
            if (!isSelectorWakeup && selector.selectNow() == 0) {
                long timeout = callTimeouts(); // process timeouts and get delta to next timeout
                ChannelPool.clear(true);

                if (++gc_counter >= FORCE_GC_CALL_COUNT) {
                    //L.d(Settings.TAG_PROXYWORKER, "System.gc");
                    gc_counter = 0;
                    System.gc();
                }

                //L.a("TCP", "timeout " + timeout);
                selector.select((timeout != 0 && timeout < 1000) ? 1000 : timeout);
            }

            if (isSelectorWakeup)
                isSelectorWakeup = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4729342
        // on android this bug alive
        catch (CancelledKeyException e) {
            e.printStackTrace();
        } catch (ClosedSelectorException e) {
            e.printStackTrace();
            // TODO XXX ???
            FilterVpnService.notifyConfigChanged(App.getContext(), FilterVpnService.STATE_CHANGED);
        }

        //L.e(Settings.TAG_PROXYWORKER, "Selector slept for: " + (System.currentTimeMillis() - ts) + " ms");
        if (Settings.DEBUG_PROFILE_NET) ev.selectUnblocked++;
    }

    // process packets from clients (packets from tun)
    private void clientProcessing() {
        Packet[] packets = tunReadThread.getPackets();
        if (packets == null)
            return; // no packets

        if (Settings.DEBUG_PROFILE_NET) ev.tunReadQueueProcessed++;

        Packet packet;
        int count = 0;

        while ((packet = packets[count++]) != null) {
            processTunReadPacket(packet);
        }

        tunReadThread.clearPackets();

        //if (count > 0) L.e(Settings.TAG_PROXYWORKER, "processTunReadPacket count = " + count + " time = " + (System.currentTimeMillis() - t));
    }

    // process connecttions to servers (sockets)
    private void serverProcessing() {
        IClient iClient;
        Iterator<SelectionKey> keys = selectedKeys.iterator();

        while (keys.hasNext()) {
            if (Settings.DEBUG_PROFILE_NET) ev.socketEvent++;

            SelectionKey selectionKey = keys.next();
            keys.remove();

            try {
                if (!selectionKey.isValid())
                    continue;

                iClient = (IClient) selectionKey.attachment();
                final boolean ok = iClient.onSockEvent(selectionKey);
                final boolean dead = iClient.isDead();
                if (!ok || dead) {
                    clientDestroy(iClient, null, false);
                    selectionKey.cancel(); // TODO XXX sometimes channels generate events after client destroy (proxy pool?)
                }
            } catch (CancelledKeyException e) {
                e.printStackTrace();
            }
        }
    }

    //
    private void timeoutsProcessing() {
        if (needToCloseProxyConnections || needToCloseAllConnections)
            closeProxyConnectionsImpl(needToCloseAllConnections); // TODO XXX why separate method?

        int ccount = clientList.getCount();

        if (ccount >= FORCE_CLOSE_CLIENT_COUNT)
            // force clients close with timeout > FORCE_CLOSE_CLIENT_TIMEOUT if too many clients
            forceClose();

        else if (ccount >= FORCE_TIMEOUT_CLIENT_COUNT)
            // normally callTimeouts called after empty call to selectNow
            // but force if clients more average number
            callTimeouts();
    }

    public void clearTimeout(IClient client) {
        client.clearTimeout();
    }

    public void setTimeout(long absTimeout) {
        if (absTimeout != 0 && (absTimeout < this.nearestAbsTimeout || this.nearestAbsTimeout == 0))
            this.nearestAbsTimeout = absTimeout;
    }

    // return delta to next timeout (or 0 if no next timeout)
    public long callTimeouts() {
        //L.d(Settings.TAG_PROXYWORKER, "callTimeouts");
        //int removed = 0;
        long nextTimeout = 0;
        long currentTime = System.currentTimeMillis();

        if (currentTime < this.nearestAbsTimeout)
            return (this.nearestAbsTimeout - currentTime);

        final Iterator<IClient> iterator = clientList.getIterator();
        while (iterator.hasNext()) {
            IClient iClient = iterator.next(); // (IClient)
            if (iClient.onTimeout(currentTime)) {
                // client timeout
                clientDestroy(iClient, iterator, false);
                //removed++;
            } else {
                // check client timeout and update worker timeout
                long clientTime = iClient.getTimeout();
                if (clientTime > currentTime && (clientTime < nextTimeout || nextTimeout == 0))
                    nextTimeout = clientTime;
            }
        }

        //L.d(Settings.TAG_PROXYWORKER, "callTimeouts: " + removed);

        this.nearestAbsTimeout = nextTimeout;
        if (nextTimeout == 0)
            return 0;
        else
            return (nextTimeout - currentTime);
    }

    private void forceClose() {
        //L.d(Settings.TAG_PROXYWORKER, "forceClose");
        long currentTime = System.currentTimeMillis();

        final Iterator<IClient> iterator = clientList.getIterator();
        while (iterator.hasNext()) {
            IClient iClient = iterator.next();
            long clientTime = iClient.getTimeout();

            if (clientTime == 0 ||
                    (clientTime > currentTime && clientTime - currentTime > FORCE_CLOSE_CLIENT_TIMEOUT)) {
                // client timeout
                clientDestroy(iClient, iterator, false);
            }
        }
    }

    public void closeProxyConnections() {
        if (selector != null) {
            needToCloseProxyConnections = true;
            selectorWakeup();
        }
    }

    public void closeAllConnections() {
        if (selector != null) {
            needToCloseAllConnections = true;
            selectorWakeup();
        }
    }

    // TODO XXX copy/past from main func (run())
    private void closeProxyConnectionsImpl(boolean all) {
        if (all)
            needToCloseAllConnections = false;
        else
            needToCloseProxyConnections = false;

        final Iterator<IClient> iterator = clientList.getIterator();
        while (iterator.hasNext()) {
            IClient ic = iterator.next();
            if (all || ic.isProxied())
                clientDestroy(ic, iterator, false);
        }
    }

    public void start() {
        thread = new Thread(this);
        thread.setName("ProxyWorker");
        thread.start();
    }

    public void stop() {
        thread.interrupt(); // see if(.isInterrupted()) in run

        if (tunReadThread != null)
            tunReadThread.cancel();
        if (tunWriteThread != null)
            tunWriteThread.cancel();

        // close tun file descriptor and resend interrupt, see TunReadThread.cancel comments
        if (pfd != null)
            try {
                pfd.close();
            } catch (IllegalStateException ie) {
            } catch (IOException e) { /*e.printStackTrace();*/ }

        if (tunReadThread != null)
            tunReadThread.interruptBySignal();

        //
        ByteBufferPool.clear();
        PacketPool.compact();
        ChannelPool.clear(false);

        ProxyBase.setWorker(null);
    }

    public void ensureStopped() {
        try {
            latch.await();
            L.i(Settings.TAG_PROXYWORKER, "All threads finished");
        } catch (InterruptedException e) {
            if (Settings.DEBUG) L.i(Settings.TAG_PROXYWORKER, "Interrupted exception on stop!", e.toString());
        }
    }

    public boolean isStopped() {
        long count = latch.getCount();
        return (count == 0L);
    }

    public ClientList getClientListForDebug() {
        return this.clientList;
    }

    public ProxyEvent getEvent() {
        return this.ev;
    }

    public int getTcpRefCount() {
        return tcpRefCount;
    }

    public int getUdpRefCount() {
        return udpRefCount;
    }

    public IFilterVpnPolicy getPolicy() {
        return policy;
    }

    public IHasherListener getHasherListener() {
        return hasherListener;
    }

    public IUserNotifier getUserNotifier() {
        return userNotifier;
    }

    // addDataRead, addDataWrite, isConnectAlive. why here?
    public void addDataRead(long size) {
        dataReadSize += size;
    }

    public void addDataWrite(long size) {
        dataWriteSize += size;
    }

    // check if current connection used to transfer data (main usage ?)
    public boolean isConnectAlive() {
        int status = NetUtil.getStatus(false);
        return (status != -1 && dataReadSize > 1024);
    }

    // dump tun packets for debug purpose
    public static boolean pktEnableDump(String filePath, long limit) {
        synchronized (pktDumperLock) {
            if (pktDumpEnabled || pktDumper != null)
                return false;

            try {
                File file = new File(filePath);
                pktDumper = new PCapFileWriter(file, false);

                if (limit > 0)
                    pktDumper.setLimit(limit);

                pktDumpEnabled = true;
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (pktDumpEnabled)
                return true;

            return false;
        }
    }

    public static void pktDisableDump() {
        synchronized (pktDumperLock) {
            pktDumpEnabled = false;
            if (pktDumper != null) {
                pktDumper.close();
                pktDumper = null;
            }
        }
    }

    public void pktDump(Packet packet, boolean input) {
        if (!pktDumpEnabled)
            return;

        synchronized (pktDumperLock) {
            try {
                EthernetFrame full = new EthernetFrame();
                if (!input) {
                    byte[] src = full.getSrcMacByteArray();
                    full.setSrcMacAddress(full.getDstMacByteArray());
                    full.setDstMacAddress(src);
                }

                byte[] ipdata = packet.getIpFrame();
                full.createIPPacketBytes(ipdata);
                pktDumper.addPacket(full.getRawBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
