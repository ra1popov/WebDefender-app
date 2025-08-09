package app.netfilter.proxy;

import android.annotation.SuppressLint;
import android.net.VpnService;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

import app.common.LibNative;
import app.common.Utils;
import app.common.debug.L;
import app.internal.ProxyBase;
import app.internal.Settings;
import app.netfilter.IFilterVpnPolicy;
import app.netfilter.IHasherListener;
import app.netfilter.IUserNotifier;
import app.netfilter.debug.ClientEvent;
import app.security.Browsers;
import app.security.Processes;

public class TCPClient implements IClient {

    protected final ClientEvent ev;

    public static final int SOCKET_WRITE_MAX_TRY = 7; // maybe set lower?

    private final String[] pkgs;
    private final Selector selector;
    private final TCPStateMachine sm;
    private final VpnService vpn;
    private final ProxyWorker worker;
    private SocketChannel chan = null;
    private SelectionKey selectorKey = null;
    private int uid;
    private IFilterVpnPolicy policy = null;
    private IHasherListener hasherListener = null;
    private long timeoutAbsTime = 0;
    private int fd = 0;

    private boolean readEventEnabled = true;
    private boolean writeEventEnabled = false;
    private boolean writeEventForce = false;

    private boolean isDead = false;
    private boolean isBrowser = false;
    private boolean isException = false;
    private boolean isKeepAlive = false;
    private boolean isOperaClassic = false;

    private boolean isCrypt = false; // proxy crypt
    int encryptKey1Pos = 0;
    int encryptKey2Pos = 0;
    int decryptKey1Pos = 0;
    int decryptKey2Pos = 0;
    byte[] key1 = null;
    byte[] key2 = null;
    int key1Len = 0;
    int key2Len = 0;

    ProxyBase.ProxyServer curProxy = null; // active proxy

    private static AtomicInteger countRef = new AtomicInteger(); // stats for server

    private TCPClient(VpnService vpn, ProxyWorker worker, Selector selector, int uid,
                      byte[] servIp, int servPort, byte[] locIp, int locPort, int ack) {
        if (Settings.DEBUG_PROFILE_NET) ev = new ClientEvent();
        else ev = null;

        this.vpn = vpn;
        this.worker = worker;
        this.selector = selector;
        this.policy = worker.getPolicy();
        this.hasherListener = worker.getHasherListener();
        this.uid = uid;

        sm = TCPStateMachine.newClient(this, servIp, servPort, locIp, locPort, ack);
        if (sm.hasBuffers()) {
            pkgs = Processes.getNamesFromUid(uid); // TODO XXX remove this (don't need in TCPClient!!!)
            if (pkgs != null) {
                if (policy != null)
                    isBrowser = policy.isBrowser(uid); // TODO XXX mays be use Browsers.isBrowser???
                isOperaClassic = Browsers.isOperaClassic(pkgs);
                //sm.useProxy();
            } else {
                if (Settings.DEBUG)
                    L.e(Settings.TAG_TCPCLIENT, "NO Packages for uid: ", Integer.toString(uid));
            }

            countRef.getAndIncrement();
        } else {
            pkgs = null;
            setDead(); // TODO XXX hmmm, and what about RST?
        }
        //L.e(Settings.TAG_TCPCLIENT, "New TCPClient for: " + packages.getCommaJoinedString());
    }

    public static TCPClient newClient(VpnService vpn, ProxyWorker worker, Selector selector, int uid,
                                      byte[] servIp, int servPort, byte[] locIp, int locPort, int ack) {
        return new TCPClient(vpn, worker, selector, uid, servIp, servPort, locIp, locPort, ack);
    }

    public boolean allowConnection() {
        return sm.onConnectAllowedByUser();
    }

    public boolean denyConnection() {
        return sm.onConnectDeniedByUser();
    }

    public void destroy(boolean silent) {
        //L.e(Settings.TAG_TCPCLIENT, "destroy()");
        setDead();

        // sockClose call must be first
        boolean alive = sockClose(true); // TODO XXX close without pool?

        if (!silent && (alive || isException))
            sm.sendRst(); // send RST if connection was alive

        sm.freeBuffers();
        //throw new NullPointerException("TCPClient::destroy!");
    }

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

    public boolean isBrowser() {
        return isBrowser;
    }

    public boolean isOperaClassic() // temp function for opera referrer workaround (see TCPStateMachine)
    {
        return isOperaClassic;
    }

    public static int getRefCount() {
        return countRef.get();
    }

    public byte[] getLocalIp() {
        return sm.localIp;
    }

    public int getLocalPort() {
        return sm.localPort;
    }

    public String[] getPkgs() {
        return pkgs;
    }

    public String getPkg0() {
        if (pkgs != null && pkgs.length > 0)
            return pkgs[0];

        return null;
    }

    public int getProtocolNo() {
        return 6;
    }

    public byte[] getServerIp() {
        return sm.serverIp;
    }

    public int getServerPort() {
        return sm.serverPort;
    }

    public boolean isPending() {
        return sm.isPending();
    }

    public boolean isProxied() {
        return sm.proxied;
    }

    public boolean isAlive() {
        return (!isDead && chan != null && (chan.isConnected() || chan.isConnectionPending()));
    }

    public boolean isConnected() {
        return chan.isConnected();
    }

    // process socket to server events, return false on error
    public boolean onSockEvent(SelectionKey selectionKey) {
        //L.w(Settings.TAG_TCPCLIENT, "onSockEvent() readyOps = " + selectionKey.readyOps());

        if (isDead)
            return false;

        if (selectionKey.isConnectable())
            return onConnectEvent(selectionKey);

        // perform read/write operations

        if (!selectionKey.isValid())
            return true; // TODO XXX why not bad?

        if (selectionKey.isReadable()) {
            if (Settings.DEBUG_PROFILE_NET) ev.sockEventReadable++;

            if (!sm.onSockReadReady())
                return false;
        }

        // write events are constantly generated if registred with writeEventEnabled mask (see sockUpdateEvents)
        if (selectionKey.isWritable() || writeEventForce)
        //if (writeEventEnabled)
        {
            if (Settings.DEBUG_PROFILE_NET) ev.sockEventWritable++;

            if (writeEventForce)
                writeEventForce = false;

            if (!sm.onSockWriteReady())
                return false;
        }

        return true;
    }

    private boolean onConnectEvent(SelectionKey selectionKey) {
        if (Settings.DEBUG_PROFILE_NET) ev.sockEventConnectable++;

        try {
            if (!chan.isConnectionPending() || !chan.finishConnect()) {
                // uups, connect problems
                if (Settings.DEBUG_PROFILE_NET) ev.finishConnectFailed++;
                L.i("TCPClient", "finishConnect returned false!!");

                return false;
            }
            //L.w(Settings.TAG_TCPCLIENT, "Connecting socket event");

            if (curProxy != null) {
                ProxyBase.notifyServersUp();
            }

            readEventEnabled = true;
            writeEventEnabled = (sm.proxied) ? true : false; // enable write for proxy auth start
            // TODO XXX uncomment this code to reproduce proxy connections simultaneous close
            // and bad proxy bug (on proxy connections timeout)
            // enable write for proxy auth start only with CONNECT method to skip auth on blocked connection (ads, etc)
            // but now some connections don't destroyed on block!!!
            //writeEventEnabled = (sm.isProxyMethodConnect()) ? true : false;
            sockUpdateEvents();

            if (!sm.onSockConnected())
                return false;

            // here we know that we are truly connected

            if (sm.proxied) {
                if (!enableServerKeepAlive(true))
                    L.e(Settings.TAG_TCPCLIENT, "enableServerKeepAlive failed!");
            }

            // OK
        } catch (IOException e) {
            // exception on OP_CONNECT && finishConnect after server connection failed

            if (curProxy == null) {
                // not using proxy
                if (Settings.DEBUG_PROFILE_NET) ev.ioexception++;
                if (Settings.DEBUG) L.i("TCPClient", "finishConnect IO exception!! ", e.toString());

                isException = true;
                return false;
            }

            // using proxy
            // try connect to current WG proxy server 3 times with 0 sec intreval
            // and then mark server as dead
            //
            // TODO XXX if have 10 connects through bad proxy at one time?

            ChannelPool.decCount();

            try {
                chan.close();
            } catch (IOException ex) {
            }
            chan = null;

            try {
                // reset events and try reconnect
                readEventEnabled = true;
                writeEventEnabled = false;
                sockUpdateEvents();

                sockConnect();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return true;
    }

    // return true if timeout
    public boolean onTimeout(long currentTime) {
        if (this.timeoutAbsTime == 0 || currentTime < this.timeoutAbsTime)
            return false; // no timeout

        if (!isAlive())
            sm.onSockDisconnected(); // set state to HALF_CLOSED_BY_SERVER

        return sm.onTimeout(currentTime, fd);
    }

    public void clearTimeout() {
        this.timeoutAbsTime = 0;
    }

    /*
     * update server soket timeout for main network cycle (see ProxyWorker.run())
     * but not all client<->server state timeout value (see TCPStateMachine.onTimeout)
     */
    public void updateTimeout(long timeout) {
        updateTimeout(timeout, true);
    }

    public void updateTimeout(long timeout, boolean disableServerKA) {
        if (disableServerKA)
            enableServerKeepAlive(false);

        this.timeoutAbsTime = System.currentTimeMillis() + timeout;
        worker.setTimeout(this.timeoutAbsTime);
    }

    // return absolute timeout time
    public long getTimeout() {
        return this.timeoutAbsTime;
    }

    // process packet from tun, return false on error
    public boolean onTunInput(Packet packet) {
        if (isDead)
            return false;

        return sm.onTunInput(packet);
    }

    /*
     * return true if closed connection was open
     * TODO XXX analyse all sockClose(true) calls for proxy correct work
     */
    public synchronized boolean sockClose(boolean force) {
        try {
            if (chan == null || !chan.isOpen())
                return false;

            // if something is requested and we didn't get response we close it without adding to pool
            boolean closeWithoutPool = (force) ? true : sm.isWaitingHttp();

            if (sm.proxied) {
                if (!closeWithoutPool && !sm.proxyUsedMethodConnect) {
                    // save proxy connect to pool
                    sockDisableEvents();
                    ChannelPool.release(chan, fd, encryptKey1Pos, encryptKey2Pos, decryptKey1Pos, decryptKey2Pos,
                            key1, key2, key1Len, key2Len);
                    chan = null;

                    return true;
                }

                ChannelPool.decCount();
            }

            // really close socket
            //L.d(Settings.TAG_TCPCLIENT, "sockClose: close connection");

            chan.close();
            chan = null;

            //L.w(Settings.TAG_TCPCLIENT, "Client count: " + clientCount);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /*
     * start socket connection to server (or proxy)
     * for this moment timeout already started (see TCPStateMachine constructor)
     */
    public synchronized void sockConnect() throws IOException {
        // TODO XXX we set sm.proxied here if connected to proxy. may be set in other place? (constructor?)

        boolean proxify = policy.isProxyUse(sm.serverIp, sm.serverPort, getUid(), isBrowser);
        boolean proxy_ready = ProxyBase.isReady();

        // proxified connection and have proxy connection in pool?
        if (policy != null && proxify && proxy_ready) {
            ChannelData data = ChannelPool.alloc();
            if (data != null) {
                chan = data.channel;
                fd = data.fd;

                setCryptKeys(data.encryptKey1Pos, data.encryptKey2Pos, data.decryptKey1Pos, data.decryptKey2Pos,
                        data.key1, data.key2, data.key1Len, data.key2Len);

                curProxy = policy.getProxyHost(); // TODO XXX proxy must be saved with channel
                isCrypt = policy.isProxyCryptUse();
                sm.proxied = true;

                // TODO XXX http on nonstandart port
                if (getServerPort() == 80)
                    sm.proxySetConnectState(TCPStateMachine.PROXY_STATE_OK);
                else
                    sm.proxyConnect();

                // didn't enable write because proxy auth already passed
                sockUpdateEvents();

                if (!sm.onSockConnected())
                    setDead();

                return;
            }
        }

        // create new connection

        chan = SocketChannel.open();
        chan.configureBlocking(false);

        if (!vpn.protect(chan.socket())) {
            L.i("TCPClient", "failed to protect SocketChannel.");
            throw new IOException("Channel protect failed");
        }

        InetSocketAddress addr = null;
        ProxyBase.ProxyServer proxy = null;

        if (policy != null && proxify && proxy_ready) {
            proxy = policy.getProxyHost();
            if (Settings.DEBUG)
                if (proxy == null) L.d(Settings.TAG_TCPCLIENT, "getProxyHost failed");
        }
        if (proxy != null) {
            if (Settings.DEBUG) L.d(Settings.TAG_TCPCLIENT, "Using proxy on ", proxy.toString());

            addr = new InetSocketAddress(proxy.getAddr(), policy.getProxyPort());
            isCrypt = policy.isProxyCryptUse();
            sm.proxied = true;

            ChannelPool.incCount();
        } else {
            addr = new InetSocketAddress(InetAddress.getByName(Utils.ipToString(sm.serverIp, 0)), sm.serverPort);
        }

        if (!chan.connect(addr)) {
            // channel opened but not yet connected

            //L.d(Settings.TAG_TCPCLIENT, "Did not connect at once...");
            curProxy = proxy;
            sm.connectStartTime = System.currentTimeMillis();
            selectorKey = chan.register(selector, SelectionKey.OP_CONNECT, this);
            //updateTimeout(30000L); // need to update timeout?
        } else {
            // TODO XXX what if channel can't connect to ip? exception?
            // must call onConnectEvent code here

            writeEventEnabled = (sm.proxied) ? true : false; // enable write for proxy auth start
            sockUpdateEvents();

            if (!sm.onSockConnected())
                setDead();
        }

        //L.w(Settings.TAG_TCPCLIENT, "Client count: " + clientCount);
        //L.e("TCPClient", "Connection ok: " + String.format("%d.%d.%d.%d", buf));

        fd = getServerFD();
        if (fd != 0) {
            // set default values for keepalive
            // 1, 3, 6 - 1 sec idle to start KA, 3 attempts max, 6 sec interval

            if (sm.proxied)
                LibNative.socketSetKAS(fd, 1, 3, 10);
            else
                LibNative.socketSetKAS(fd, 1, 3, 6);
        }
    }

    public void sockEnableReadEvent(boolean enable) throws IOException {
        //L.a(Settings.TAG_TCPCLIENT, "sockEnableReadEvent: " + enable + " in " + Integer.toHexString(sm.thisId));
        //new Exception().printStackTrace();

        if (readEventEnabled != enable) {
            readEventEnabled = enable;
            sockUpdateEvents();
        }
    }

    public void sockEnableWriteEvent(boolean enable) throws IOException {
        //L.a(Settings.TAG_TCPCLIENT, "sockEnableWriteEvent: " + enable + " in " + Integer.toHexString(sm.thisId));
        //new Exception().printStackTrace();

        if (writeEventEnabled != enable) {
            if (enable && chan.socket().isOutputShutdown())
                return;

            writeEventEnabled = enable;
            sockUpdateEvents();

            //if (enable)
            //	  sockForceEvents(); // TODO XXX adding it here break network processing
        }
    }

    // update events for this socket in selector (check readEventEnabled, writeEventEnabled)
    private void sockUpdateEvents() throws IOException {
        byte opsMask = 0;

        if (readEventEnabled)
            opsMask = (byte) 1;
        if (writeEventEnabled)
            opsMask = (byte) (opsMask | 4);

        //L.w(Settings.TAG_TCPCLIENT, "sockUpdateEvents opsMask = " + opsMask);

        if (chan != null) {
            try {
                selectorKey = chan.register(selector, opsMask, this);
            } catch (CancelledKeyException ex) {

            }
        }
    }

    // disable all events for this socket in selector
    private void sockDisableEvents() throws IOException {
        readEventEnabled = false;
        writeEventEnabled = false;
        writeEventForce = false;

        sockUpdateEvents();
    }

    // force to add this client to selected keys with select (if have)
    public void sockForceEvents() {
        if (selectorKey == null)
            return;

        if (!writeEventForce && !chan.socket().isOutputShutdown())
            writeEventForce = true;

        worker.selectedKeysUpdate(selectorKey);
    }

    public int sockRead(ByteBuffer buf, boolean skipCrypt) throws IOException {
        //L.w("TCPClient", "Reading data from outer connection!");
        if (!chan.isConnected())
            return 0;

        int pos = 0;
        if (isCrypt)
            pos = buf.position();

        int read = chan.read(buf);
        if (read <= 0) {
            if (Settings.DEBUG)
                L.i(Settings.TAG_TCPCLIENT, "Read ", Integer.toString(read), " bytes!");
            return read;
        }

        worker.addDataRead(read); // stats
        if (isCrypt && !skipCrypt)
            cryptBuf(buf, pos, read, true);

        return read;
    }

    public int sockWrite(ByteBuffer buf, boolean skipCrypt) throws IOException {
        //L.w("TCPClient", "Writing data to outer connection! Size = " + buf.remaining(), " chan.validOps = " + chan.validOps());
        if (!buf.hasRemaining())
            return 0;

        if (chan == null) {
            L.w(Settings.TAG_TCPCLIENT, "chan null!");
            return 0;
        }

        int towrite = 0;
        if (isCrypt && !skipCrypt) {
            towrite = buf.remaining();
            cryptBuf(buf, buf.position(), towrite, false);
        }

        int written = 0;
        int tryCount = 0;
        int toWrite = buf.remaining();

        while (toWrite > 0 && tryCount++ < SOCKET_WRITE_MAX_TRY) {
            int w = chan.write(buf);

            toWrite -= w;
            written += w;
        }
        //L.w("TCPClient", "Time: " + (System.currentTimeMillis() - t));

        if (written > 0)
            worker.addDataWrite(written); // stats

        if (isCrypt && !skipCrypt && written != towrite) {
            L.e(Settings.TAG_TCPCLIENT, "DECRYPTING DATA THAT DIDN'T GO TO SERVER");
            cryptBuf(buf, written, towrite - written, true); // TODO XXX fix this !!!
        }

        return written;
    }

    public void sockWriteShutdown() throws IOException {
        chan.socket().shutdownOutput();
    }

    public void tunWrite(Packet packet) {
        if (Settings.DEBUG_PROFILE_NET) ev.tunWrite++;
        worker.writeToTun(packet);
    }

    public int getUid() {
        return uid;
    }

    public IFilterVpnPolicy getPolicy() {
        return policy;
    }

    public IUserNotifier getUserNotifier() {
        return worker.getUserNotifier();
    }

    /*
     * return socket fd from SocketChannel to server
     * check correct work on each new version of android!!!
     */
    @SuppressLint("DiscouragedPrivateApi")
    @SuppressWarnings("JavaReflectionMemberAccess")
    public int getServerFD() {
        try {
            Field field1 = chan.getClass().getDeclaredField("fd");
            field1.setAccessible(true);
            FileDescriptor fd1 = (FileDescriptor) field1.get(chan);

            if (fd1 != null) {
                Field field2 = fd1.getClass().getDeclaredField("descriptor");
                field2.setAccessible(true);
                int fd2 = field2.getInt(fd1);

                if (fd2 != -1) {
                    return fd2;
                }
            }

        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ignored) {
        }

        return 0;
    }

    // enable keepalive on server connection (return keepalive status)
    public boolean enableServerKeepAlive(boolean enable) {
        if (sm != null && sm.proxied && !enable)
            return true;
        else if (enable && isKeepAlive)
            return true;
        else if (fd == 0 || (!enable && !isKeepAlive))
            return false;

        if (LibNative.socketEnableKA(fd, enable))
            isKeepAlive = !isKeepAlive;

        return isKeepAlive;
    }

    public boolean serverKeepAliveEnabled() {
        return isKeepAlive;
    }

    public void setCryptKeys(int encryptKey1Pos, int encryptKey2Pos, int decryptKey1Pos, int decryptKey2Pos,
                             byte[] key1, byte[] key2, int key1Len, int key2Len) {
        this.encryptKey1Pos = encryptKey1Pos;
        this.encryptKey2Pos = encryptKey2Pos;
        this.decryptKey1Pos = decryptKey1Pos;
        this.decryptKey2Pos = decryptKey2Pos;
        this.key1 = key1;
        this.key2 = key2;
        this.key1Len = key1Len;
        this.key2Len = key2Len;
    }

    protected boolean cryptBuf(ByteBuffer buf, int pos, int size, boolean isDecrypt) {
        byte[] message = buf.array();
        if (message == null)
            return false;

        int kpos1, kpos2;
        byte[] k1, k2;
        int klen1, klen2;

        if (isDecrypt) {
            kpos2 = decryptKey1Pos;
            kpos1 = decryptKey2Pos;

            k2 = key1;
            k1 = key2;
            klen2 = key1Len;
            klen1 = key2Len;
        } else {
            kpos1 = encryptKey1Pos;
            kpos2 = encryptKey2Pos;

            k1 = key1;
            k2 = key2;
            klen1 = key1Len;
            klen2 = key2Len;
        }
        //L.w(Settings.TAG_TCPCLIENT, "crypting - " + isDecrypt +" pos = "+pos+" size = "+size+" kpos = "+kpos);

        if (k1 != null && klen1 > 0) {
            int ml = size; //message.length;
            int i = pos; //0

            while (ml > 0) {
                int j = Math.min(klen1 - kpos1, ml);
                ml -= j;
                for (; j > 0; j--, i++, kpos1++) {
                    byte b = message[i];
                    if (b != 0 && (b ^= (byte) k1[kpos1]) != 0)
                        message[i] = b;
                }
                if (kpos1 == klen1)
                    kpos1 = 0;
            }
        }

        if (k2 != null && klen2 > 0) {
            int ml = size; //message.length;
            int i = pos; //0

            while (ml > 0) {
                int j = Math.min(klen2 - kpos2, ml);
                ml -= j;
                for (; j > 0; j--, i++, kpos2++) {
                    byte b = message[i];
                    if (b != 0 && (b ^= (byte) k2[kpos2]) != 0)
                        message[i] = b;
                }
                if (kpos2 == klen2)
                    kpos2 = 0;
            }
        }

        if (isDecrypt) {
            decryptKey1Pos = kpos2;
            decryptKey2Pos = kpos1;
        } else {
            encryptKey1Pos = kpos1;
            encryptKey2Pos = kpos2;
        }

        return true;
    }

    public void dump() {
        if (Settings.DEBUG) {
            String[] r3;
            int i0, i1;
            r3 = this.toString().split("\n");
            i0 = r3.length;
            i1 = 0;

            while (i1 < i0) {
                L.i("TCPClient", r3[i1].trim());
                i1 = i1 + 1;
            }
        }
    }

    public String toString() {
        if (Settings.DEBUG) {
            String r6, r12, r18, r26, r33, r37, r45;
            StringBuilder r30;
            r6 = (new StringBuilder()).append("").append("prot: TCP\n").toString();
            r12 = (new StringBuilder()).append(r6).append(sm.toString()).toString();
            r18 = (new StringBuilder()).append(r12).append(ev.toString()).toString();
            r26 = (new StringBuilder()).append(r18).append("isPending: ").append(isPending()).append("\n").toString();
            r30 = (new StringBuilder()).append(r26).append("chan: ");

            if (chan == null) {
                r33 = "(none)";
            } else {
                r33 = chan.toString();
            }

            r37 = r30.append(r33).append("\n").toString();
            r45 = (new StringBuilder()).append(r37).append("writeEventEnabled: ").append(writeEventEnabled).append("\n").toString();
            return (new StringBuilder()).append(r45).append("readEventEnabled: ").append(readEventEnabled).append("\n").toString();
        }

        return "";
    }
}
