package app.netfilter.proxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

import app.common.Hasher;
import app.common.LibNative;
import app.common.Rnd;
import app.common.Utils;
import app.common.debug.L;
import app.common.memdata.ByteBufferPool;
import app.common.memdata.MemoryBuffer;
import app.common.memdata.MemoryCache;
import app.internal.Settings;
import app.netfilter.IFilterVpnPolicy;
import app.netfilter.IHasherListener;
import app.netfilter.IUserNotifier;
import app.netfilter.debug.ClientEvent;
import app.netfilter.dns.DNSAnswer;
import app.netfilter.http.ChunkedReader;
import app.netfilter.http.RequestHeader;
import app.netfilter.http.ResponseHeader;
import app.security.Policy;
import app.security.PolicyRules;

public class TCPStateMachine {

    protected ClientEvent ev;

    public int thisId = 0;
    private static final boolean DEBUG_PROXY_V01 = true; // use proxy protocol v0.1

    public static final long TIMEOUT_PENDING_VALUE = 15000L; // timeout for connecting to server (or proxy)
    public static final long TIMEOUT_CLOSE_VALUE = 5000L;  // timeout for closing connection with client (after FIN)
    public static final long TIMEOUT_RECEIVE_INITIAL = 10000L; // start timeout value while s/r data (after every packet with data)
    public static final long TIMEOUT_EXPIRE_LIMIT = 60000L; // common timeout limit
    public static final long TIMEOUT_KEEPALIVE_TEST = 30000L; // interval to send KA to server before closing con. (if KA failed)
    public static final long TIMEOUT_STATE_LIMIT = 5000L;  // timeout between proxy auth states change

    public static final int PROXY_STATE_INITED = 0;
    public static final int PROXY_STATE_HELLO_SENT = 1;
    public static final int PROXY_STATE_UNAUTHORIZED = 2;
    public static final int PROXY_STATE_AUTH_SENT = 3;
    public static final int PROXY_STATE_CONNECT_SENT = 4;
    public static final int PROXY_STATE_PING_SENT = 5;
    public static final int PROXY_STATE_OK = 6;
    public static final int PROXY_STATE_ERROR = -1;

    public static final int TCP_HEADER_LENGTH = 40;
    private static final int TCP_WINDOW_HOLE = 1024;
    private static final int TCP_INVALID_SEQNO_MAX = 3;   // max number of sequential invalid SeqNo or Acks
    private static final int TCP_DUPACK_MAX = 13;  // max number of dupAcks (3 bad value for speedtest)
    private static final int TCP_AFTERFIN_MAX = 13;  // max number of packets to send after any fin

    private static final int REDIRECT_MAX_LENGTH = PacketPool.POOL4_PACKET_SIZE - 2048;

    // server connect states
    private enum State {
        CONNECT_PENDING,      // only created
        CONNECTING_TO_SERVER,
        CONNECTED_TO_SERVER,
        DISCONNECTED,          // TODO XXX not used, use
        HALF_CLOSED_BY_APP,   // client connection closing in process
        HALF_CLOSED_BY_SERVER // server socket closed
    }

    private int seqNo;                          // position of last octet we sent
    private int ackNo;                          // next octet number we expect from the peer
    private int keepaliveSeqNo = 0;
    private boolean isFinReceived = false;      // fin from app
    private boolean isFinSent = false;          // fin sended to app
    private boolean isFinAckReceived = false; // ack to fin received
    private boolean is404 = false;              // 404 sended, ready to close connection
    private long lastReceivedAck = 2147483648L; // ??? why long?
    private long lastRetransmitTime = 0L;
    public final byte[] localIp;
    public final int localPort;
    public final byte[] serverIp;
    public final int serverPort;

    private int sameAckCount = 0;
    private int keepaliveCounter = 0;
    private int invalidSeqNoCounter = 0;
    private int dupAckCounter = 0;
    private int afterFinCounter = 0;

    private State state;
    private boolean isHttpProtocol = true;
    private boolean isFirstRequest = true;
    private boolean wasPostDetected = false;
    public long connectStartTime;
    private long timeout;
    private boolean timeoutRestart = false;

    private final TCPClient client;
    private ByteBuffer sockReadBuf;
    private ByteBuffer sockWriteBuf;

    private ChunkedReader chunkedReader = new ChunkedReader();
    private Hasher hasher = null;
    private MemoryBuffer buffer = null;
    private IFilterVpnPolicy policy = null;
    private IHasherListener hasherListener = null;

    private ArrayList<RequestHeader> requests = new ArrayList<RequestHeader>(1); // for pipelining
    private boolean requestsAllocated = false;
    private String lastReferrer = null;

    private int scanDataBufferSize = 0; // policy can set required minimum size of buffered server data before separate check
    private boolean scanDataBuffered = false;

    private int proxyConnectState = PROXY_STATE_INITED; // proxy vars
    private long proxyStateChangeTime = 0;
    public boolean proxied = false;
    public boolean proxyUsedMethodConnect = false;
    private static final PublicKey proxyPublicKey;
    private static Cipher cipher = null;

    private static final byte[] CHECK_BUFFER = new byte[ByteBufferPool.BUFFER_SIZE_BIG << 1];

    private static final byte[] CHECK_BUFFER_SEC = new byte[512];

    // bad perf http://java-performance.info/java-util-random-java-util-concurrent-threadlocalrandom-multithreaded-environments/
    //private static Random random = new Random();
    private static Rnd random = new Rnd(); // TODO XXX check multithreaded perf

    static {
        proxyPublicKey = Utils.getPublicKeyFromAsset("proxy.pem");
        if (proxyPublicKey != null)
            cipherInit(proxyPublicKey);
        if (cipher == null)
            L.e(Settings.TAG_TCPSTATEMACHINE, "cipher IS NULL!");

        if (Settings.DEBUG_TCP)
            L.a(Settings.TAG_TCPSTATEMACHINE, "--- tcp ---");
    }

    private TCPStateMachine(TCPClient tcpClient, byte[] servIp, int servPort, byte[] locIp, int locPort, int ack) {
        super();

        ev = null;
        if (Settings.DEBUG_PROFILE_NET) ev = tcpClient.ev;

        client = tcpClient;
        serverIp = servIp;
        serverPort = servPort;
        localIp = locIp;
        localPort = locPort;

        setSeqNo(0);
        setAckNo(ack + 1);

        state = State.CONNECT_PENDING;
        timeout = TIMEOUT_PENDING_VALUE;

        // why >1000 ?
        //int bufSize = (serverPort > 1000) ? ByteBufferPool.BUFFER_SIZE_SMALL : ByteBufferPool.BUFFER_SIZE_BIG;
        int bufSize = (serverPort == 80 || serverPort == 443) ?
                ByteBufferPool.BUFFER_SIZE_BIG : ByteBufferPool.BUFFER_SIZE_SMALL;

        sockReadBuf = ByteBufferPool.alloc(bufSize);
        sockWriteBuf = ByteBufferPool.alloc(bufSize);
        if (sockReadBuf != null && sockWriteBuf != null)
            // TODO XXX if not allocate memory? why only not update timeout?
            updateTimeout(timeout, true);

        this.policy = client.getPolicy();
    }

    public static TCPStateMachine newClient(TCPClient client, byte[] servIp, int servPort, byte[] locIp, int locPort, int ack) {
        return new TCPStateMachine(client, servIp, servPort, locIp, locPort, ack);
    }

    private void setAckNo(int ackNo) {
        this.ackNo = ackNo;
    }

    private void setSeqNo(int seqNo) {
        this.seqNo = seqNo;
    }

    private int calcAckToSend() {
        int pos = sockWriteBuf.position();

        int ack = ackNo + pos;
        if (isFinReceived)
            ack += 1;

        return ack;
    }

    /*
    > data seq 1 ack 1 nseq 1 + 536
    > data seq 537 ack 1 nseq 537 + 228
    < ack seq 1 ack 537
    < ack seq 1 ack 765
    < ack seq 1 ack 765 winup

    < data seq 1 ack 765 nseq 1 + 816
    > ack seq 765 ack 817
    > fin seq 765 ack 817

    < ack seq 817 ack 766
    */
    private boolean isSeqnoMatch(Packet packet) {
        int pos = sockWriteBuf.position();

        int delta = packet.seqNo - ackNo;
        return (delta == pos);
    }

    private boolean hasReceivedDupAck(Packet r1) {
        if ((int) lastReceivedAck == r1.ackNo) {
            int i3 = (int) lastReceivedAck - seqNo;
            if (i3 >= 0) {
                int pos = sockReadBuf.position();
                if (i3 < pos) {
                    sameAckCount += 1;
                    if (sameAckCount == 3)
                        return true;
                }
            } else {
                L.i(Settings.TAG_TCPSTATEMACHINE, "retrans invalid ack"); // what this???
            }
        } else {
            lastReceivedAck = (long) r1.ackNo;
        }

        sameAckCount = 0;
        lastRetransmitTime = 0L;

        return false;
    }

    public boolean hasBuffers() {
        return (sockReadBuf != null && sockWriteBuf != null);
    }

    public void freeBuffers() {
        //L.e(Settings.TAG_TCPSTATEMACHINE, "freeBuffer() in ", this.toString());

        if (sockReadBuf != null)
            ByteBufferPool.release(sockReadBuf);
        if (sockWriteBuf != null)
            ByteBufferPool.release(sockWriteBuf);

        sockReadBuf = null;
        sockWriteBuf = null;
    }

    // connection with client closed?
    public boolean isReadyToClose() {
        return (isFinReceived && isFinAckReceived);
    }

    private void setClosingState() {
        // TODO XXX read event is switched on somewhere after setClosingState and we send MANY dublicated FIN,ACKs!!!!!!
        // so disable events even if state == State.HALF_CLOSED_BY_APP

        if (state != State.HALF_CLOSED_BY_APP) {
            updateTimeout(TIMEOUT_CLOSE_VALUE, false);
            state = State.HALF_CLOSED_BY_APP;
        }

        // TODO XXX we call setClosingState when receive fin from APP and disable read but server can have data to APP!!!!!!!!
        try {
            client.sockEnableReadEvent(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // have some http pipeline requests without response?
    public boolean isWaitingHttp() {
        return (isHttpProtocol && requests.size() > 0);
    }

    /*
     * return true if need use proxy CONNECT method for current connection
     * TODO XXX now it simple check port (if http on nonstandart port?)
     */
    public boolean isProxyMethodConnect() {
        return (proxied && serverPort != 80);
    }

    // return true if connect not yet established with client (tun)
    public boolean isPending() {
        return (state == State.CONNECT_PENDING);
    }

    // return true if connection bad (we have bugs in our tcp stack :( )
    private boolean isBadState() {
        if ((isFinSent || isFinReceived) && afterFinCounter++ > TCP_AFTERFIN_MAX)
            return true;

        return false;
    }

    // check if connect allowed
    public boolean onConnectAllowedByUser() {
        //L.d(Settings.TAG_TCPSTATEMACHINE, "TCPStateMachine::onConnectAllowedByUser()");
        if (Settings.DEBUG_PROFILE_NET) ev.allowed += 1;

        state = State.CONNECTING_TO_SERVER;

        try {
            client.sockConnect();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean onConnectDeniedByUser() {
        L.d(Settings.TAG_TCPSTATEMACHINE, "onConnectDeniedByUser()");
        if (Settings.DEBUG_PROFILE_NET) ev.denied++;

        forceClose(true); // need here?
        return true;
    }

    public void proxySetConnectState(int state) {
        if (Settings.DEBUG_WGPROXY)
            L.a(Settings.TAG_TCPSTATEMACHINE, "State: " + state + " (" + Integer.toHexString(thisId) + ")");

        proxyConnectState = state;
        proxyStateChangeTime = System.currentTimeMillis();
    }

    public boolean proxyConnect() {
        if (Settings.DEBUG_WGPROXY)
            L.a(Settings.TAG_TCPSTATEMACHINE, "proxyConnect (" + Integer.toHexString(thisId) + ")");

        boolean ok = false;
        String domain;
        DNSAnswer answer = null;

        if (Settings.DNS_USE_CACHE)
            answer = DNSCache.getForIp(serverIp);

        if (answer != null && answer.domain != null)
            domain = answer.domain + ":" + serverPort;
        else
            domain = Utils.ipToString(serverIp, serverPort);

        if (Settings.DEBUG_WGPROXY)
            L.a(Settings.TAG_TCPSTATEMACHINE, "Connecting to " + domain + " through proxy... (" + Integer.toHexString(thisId) + ")");

        //L.e(Settings.TAG_TCPSTATEMACHINE, "Connecting to ", Utils.ipToString(serverIp, serverPort), " through proxy...");
        ByteBuffer buf = ByteBufferPool.alloc(ByteBufferPool.BUFFER_SIZE_SMALL);
        String method = "CONNECT " + domain + " HTTP/1.1\r\n\r\n";

        if (Settings.DEBUG_WGPROXY)
            L.a(Settings.TAG_TCPSTATEMACHINE, "Method: " + method + " (" + Integer.toHexString(thisId) + ")");

        buf.put(method.getBytes());
        buf.flip();
        try {
            client.sockWrite(buf, false); // TODO if (written < size)
            proxySetConnectState(PROXY_STATE_CONNECT_SENT);
            client.sockEnableWriteEvent(false);

            ok = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ByteBufferPool.release(buf);
        }

        if (!ok) {
            proxySetConnectState(PROXY_STATE_ERROR);
            return false;
        }

        return true;
    }

    private boolean proxyConnectResponse() {
        if (Settings.DEBUG_WGPROXY)
            L.a(Settings.TAG_TCPSTATEMACHINE, "proxyConnectResponse (" + Integer.toHexString(thisId) + ")");
        //L.e(Settings.TAG_TCPSTATEMACHINE, "Reading proxy handshake response from ", Utils.ipToString(serverIp, serverPort), " ...");

        boolean ok = false;
        ByteBuffer buf = ByteBufferPool.alloc(ByteBufferPool.BUFFER_SIZE_SMALL);

        try {
            int read = client.sockRead(buf, false);
            //L.e(Settings.TAG_TCPSTATEMACHINE, "Read ", Integer.toString(read), " bytes");
            if (read > 0) {
                int pos = buf.position();
                buf.flip();
                buf.get(CHECK_BUFFER, 0, Math.min(CHECK_BUFFER.length, pos));
                String s = new String(CHECK_BUFFER, 0, 0, pos);

                if (Settings.DEBUG_WGPROXY)
                    L.a(Settings.TAG_TCPSTATEMACHINE, "Response from proxy: " + s + " (" + Integer.toHexString(thisId) + ")");

                if (LibNative.asciiIndexOf("200 OK", s) >= 0) {
                    proxySetConnectState(PROXY_STATE_OK);
                    proxyUsedMethodConnect = true;
                    boolean has = sockWriteBuf.hasRemaining();
                    client.sockEnableWriteEvent(has);

                    ok = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ByteBufferPool.release(buf);
        }

        if (!ok) {
            proxySetConnectState(PROXY_STATE_ERROR);
            return false;
        }

        return true;
    }

    // init cipher
    private static void cipherInit(final PublicKey publicKey) {
        try {
            cipher = Cipher.getInstance("RSA/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, proxyPublicKey);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            cipher = null;
        } catch (NoSuchPaddingException ex) {
            ex.printStackTrace();
            cipher = null;
        } catch (InvalidKeyException ex) {
            ex.printStackTrace();
            cipher = null;
        }
    }

    // put result into upper part of buffer (i.e. input size must be >= inputLen + inputLen)
    // return > 0 if all ok, 0 if invalid params and -1 need to change buffer
    private static int cipherDoFinal(byte[] input, int inputLen) {

        boolean reinit = false;

        if (inputLen <= 0) {
            return 0;
        }

        try {

            Arrays.fill(input, inputLen, inputLen + inputLen, (byte) 0);
            cipher.doFinal(Arrays.copyOfRange(input, 0, inputLen), 0, inputLen, input, inputLen);

            if (inputLen == 0) throw new SignatureException(); // compiler workaround
            return inputLen;

        } catch (IllegalBlockSizeException ex) {
            reinit = true;
        } catch (BadPaddingException ex) {
            reinit = true;
        } catch (ShortBufferException ex) {
            reinit = true;
        } catch (SignatureException ex) {
            // doFinal can throw SignatureException: data too large for modulus
            // on this exception need to recreate cipher and change buffer

            //L.a(Settings.TAG_TCPSTATEMACHINE, "SignatureException");
            reinit = true;
        } catch (Exception ex) {
            reinit = true;
        }

        if (reinit) {
            cipher = null;
            cipherInit(proxyPublicKey);
            return -1;
        }

        return 0;

    }

    private boolean proxyHello_v01() {
        if (Settings.DEBUG_WGPROXY)
            L.a(Settings.TAG_TCPSTATEMACHINE, "proxyHello_v01 (" + Integer.toHexString(thisId) + ")");

        boolean ok = false;
        ByteBuffer buf = ByteBufferPool.alloc(ByteBufferPool.BUFFER_SIZE_SMALL);

        // YOYO + zone name (2, ascii) + TTTTTTTT
        //String method = "YOYORU09870987"; // hello with fake token to enable testing crypt key
        String method = "YOYO" + "RU" + "00000000"; // TODO zone
        //L.d(Settings.TAG_TCPSTATEMACHINE, "Method: ", method);

        buf.put(method.getBytes());
        buf.flip();
        try {
            client.sockWrite(buf, true); // TODO if (written < size)
            proxySetConnectState(PROXY_STATE_HELLO_SENT);
            client.sockEnableWriteEvent(false);

            ok = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ByteBufferPool.release(buf);
        }

        if (!ok) {
            proxySetConnectState(PROXY_STATE_ERROR);
            return false;
        }

        return true;
    }

    private boolean proxyHelloResponse_v01() {
        if (Settings.DEBUG_WGPROXY)
            L.a(Settings.TAG_TCPSTATEMACHINE, "proxyHelloResponse_v01 (" + Integer.toHexString(thisId) + ")");

        boolean ok = false;
        ByteBuffer buf = ByteBufferPool.alloc(ByteBufferPool.BUFFER_SIZE_SMALL);

        try {
            int read = client.sockRead(buf, true);
            //L.e(Settings.TAG_TCPSTATEMACHINE, "Read ", Integer.toString(read), " bytes");
            if (read > 0) {
                int pos = buf.position();
                buf.flip();
                buf.get(CHECK_BUFFER, 0, Math.min(CHECK_BUFFER.length, pos));

                // YO + SS + AAAAAAAAAAAAAAAA + TTTTTTTT
                if (pos >= 28 && CHECK_BUFFER[0] == 'Y' && CHECK_BUFFER[1] == 'O') {
                    if (CHECK_BUFFER[2] == 'O' && CHECK_BUFFER[3] == 'K') {
                        // ok
                        proxySetConnectState(PROXY_STATE_UNAUTHORIZED);
                        client.sockEnableWriteEvent(true);

                        ok = true;
                    } else if (CHECK_BUFFER[2] == 'R' && CHECK_BUFFER[3] == 'D') {
                        // redirect
                        byte[] ip = new byte[4];
                        ip[0] = CHECK_BUFFER[4];
                        ip[1] = CHECK_BUFFER[5];
                        ip[2] = CHECK_BUFFER[6];
                        ip[3] = CHECK_BUFFER[7];
                        byte[] token = new byte[8];
                        token[0] = CHECK_BUFFER[20];
                        token[1] = CHECK_BUFFER[21];
                        token[2] = CHECK_BUFFER[22];
                        token[3] = CHECK_BUFFER[23];
                        token[4] = CHECK_BUFFER[24];
                        token[5] = CHECK_BUFFER[25];
                        token[6] = CHECK_BUFFER[26];
                        token[7] = CHECK_BUFFER[27];
                        // TODO redirect
                    }
                }
            }
            //else if (read == 0)
            //{
            //	  ok = true;
            //}
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ByteBufferPool.release(buf);
        }

        if (!ok) {
            proxySetConnectState(PROXY_STATE_ERROR);
            return false;
        }

        return true;
    }

    // Method for generating a secret byte
    private byte generateSuperByte() {
        while (true) {
            byte b = random.generateByte();
            if (b == 0 || (b & 0xFF) >= 0xC0) {
                continue;
            }
            return b;
        }
    }

    private boolean proxyAuth_v01() {
        if (Settings.DEBUG_WGPROXY)
            L.a(Settings.TAG_TCPSTATEMACHINE, "proxyAuth_v01 (" + Integer.toHexString(thisId) + ")");

        boolean ok = false;

        do {

            String id = Policy.getUserToken(true);

            if (id == null) {
                break;
            }

            ByteBuffer buf = ByteBufferPool.alloc(ByteBufferPool.BUFFER_SIZE_SMALL);

            boolean isGPId = (id.length() == 32); // see getUserToken
            if (!isGPId && id.length() > 4) {
                // token, remove first 4 bytes
                id = id.substring(4, id.length());
            }

            // settings
            System.arraycopy(("xWGIf1__f2__" + id + "cd__________").getBytes(), 0, CHECK_BUFFER_SEC, 0, 56);
            //System.arraycopy("xWGIf1__f2__id______________________________cd__________".getBytes(), 0, CHECK_BUFFER, 0, 56);
            CHECK_BUFFER_SEC[4] = 0;
            CHECK_BUFFER_SEC[5] = 0;
            CHECK_BUFFER_SEC[6] = 0;
            CHECK_BUFFER_SEC[7] = (byte) Policy.getProxyFlags(); // flags1
            CHECK_BUFFER_SEC[8] = 0;
            CHECK_BUFFER_SEC[9] = 0;
            CHECK_BUFFER_SEC[10] = 0;
            CHECK_BUFFER_SEC[11] = 0; // flags2


            if (!isGPId)
                // set 3 bit, to say proxy that token is free (only for proto v0.1)
                CHECK_BUFFER_SEC[7] |= 4;

            // gen 2 keys (max len 99, min 77)
            byte[] k1 = new byte[99]; // key1
            byte[] k2 = new byte[99]; // key2
            byte len1, len2;


            try {
                random.generateBytesFill(k1);
                len1 = (byte) (77 + random.generateInt(99 - 77 + 1));
                random.generateBytesFill(k2);
                len2 = (byte) (77 + random.generateInt(99 - 77 + 1));
            } catch (SecurityException ex) {
                ex.printStackTrace();
                break;
            }


            // prepare buffer
            System.arraycopy(k1, 0, CHECK_BUFFER_SEC, 56 + 1, 99);
            System.arraycopy(k2, 0, CHECK_BUFFER_SEC, 56 + 1 + 99 + 1, 99);
            CHECK_BUFFER_SEC[56] = len1;
            CHECK_BUFFER_SEC[56 + 1 + 99] = len2;


            // crypt buffer
            int t = 0;
            while (buf.position() == 0 && cipher != null && t < 10) {
                t++;
                byte b = generateSuperByte();

                //L.a(Settings.TAG_TCPSTATEMACHINE, "b " + b);

                CHECK_BUFFER_SEC[0] = b;
                for (int i = 1; i < 256; i++) {
                    CHECK_BUFFER_SEC[i] ^= b;
                }

                int res = cipherDoFinal(CHECK_BUFFER_SEC, 256);

                if (res == -1 || res == 0) {
                    for (int i = 1; i < 256; i++) {
                        CHECK_BUFFER_SEC[i] ^= b;
                    }
                    continue;
                }
                //L.ar(Settings.TAG_TCPSTATEMACHINE, CHECK_BUFFER, 256, 256);
                buf.put(CHECK_BUFFER_SEC, 256, 256);

            }


            if (cipher == null || t >= 10) {
                // We ran into trouble and couldn’t authenticate with the server — we’ll report it if possible.
                break;
            }

            //
            buf.flip();

            try {

                client.sockWrite(buf, true); // TODO if (written < size)
                proxySetConnectState(PROXY_STATE_AUTH_SENT);
                client.sockEnableWriteEvent(false);

                client.setCryptKeys(0, 0, 0, 0, k1, k2, len1, len2);

                ok = true;

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ByteBufferPool.release(buf);
            }

        } while (false);


        if (!ok) {

            proxySetConnectState(PROXY_STATE_ERROR);
            return false;
        }

        return true;
    }

    private boolean proxyAuthResponse_v01() {
        if (Settings.DEBUG_WGPROXY)
            L.a(Settings.TAG_TCPSTATEMACHINE, "proxyAuthResponse_v01 (" + Integer.toHexString(thisId) + ")");

        boolean ok = false;

        ByteBuffer buf = ByteBufferPool.alloc(ByteBufferPool.BUFFER_SIZE_SMALL);

        try {
            int read = client.sockRead(buf, true);
            //L.e(Settings.TAG_TCPSTATEMACHINE, "Read ", Integer.toString(read), " bytes");
            if (read > 0) {
                int pos = buf.position();
                buf.flip();
                buf.get(CHECK_BUFFER, 0, Math.min(CHECK_BUFFER.length, pos));

                if (pos >= 4 && CHECK_BUFFER[0] == 'Y' && CHECK_BUFFER[1] == 'O') {
                    if (CHECK_BUFFER[2] == 'G' && CHECK_BUFFER[3] == 'O') {
                        // ok, working
                        proxySetConnectState(PROXY_STATE_OK);
                        boolean has = sockWriteBuf.hasRemaining();
                        client.sockEnableWriteEvent(has);

                        ok = true;
                    } else if (CHECK_BUFFER[2] == 'B' && CHECK_BUFFER[3] == 'I') {
                        // bad identifier
                        Policy.clearToken();
                        Policy.refreshToken(true);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ByteBufferPool.release(buf);
        }

        if (!ok) {
            proxySetConnectState(PROXY_STATE_ERROR);
            return false;
        }

        return true;
    }

    // proto v0.2
    // TODO XXX save ident packet buffer and only reXor it
    private boolean proxyAuth_v02() {
        if (Settings.DEBUG_WGPROXY)
            L.a(Settings.TAG_TCPSTATEMACHINE, "proxyAuth_v02 (" + Integer.toHexString(thisId) + ")");

        boolean ok = false;

        do {
            String id = Policy.getUserToken(true);
            if (id == null)
                break;

            ByteBuffer buf = ByteBufferPool.alloc(ByteBufferPool.BUFFER_SIZE_SMALL);

            // ident packet (256 bytes)

            boolean isGPId = (id.length() == 32); // see getUserToken
            if (isGPId)
                id += "____";

            // xor byte (1) + WGI (3) + zone name (2) + redirect token (8) + client flags (8) + id (36) + client data (12)
            // TODO XXX zone
            System.arraycopy(("xWGIRU00000000f1__f2__" + id + "cd__________").getBytes(), 0, CHECK_BUFFER, 0, 70);
            //System.arraycopy("xWGIz_t_______f1__f2__id__________________________________cd__________".getBytes(), 0, CHECK_BUFFER, 0, 70);

            // client flags1 (4) + flags2 (4)
            CHECK_BUFFER[14] = 0;
            CHECK_BUFFER[15] = 0;
            CHECK_BUFFER[16] = 0;
            CHECK_BUFFER[17] = (byte) Policy.getProxyFlags();
            CHECK_BUFFER[18] = 0;
            CHECK_BUFFER[19] = 0;
            CHECK_BUFFER[20] = 0;
            CHECK_BUFFER[21] = 0;

            if (isGPId)
                CHECK_BUFFER[54] = 0;
            CHECK_BUFFER[55] = 0;
            CHECK_BUFFER[56] = 0;
            CHECK_BUFFER[57] = 0; // reset id end

            // gen 2 keys (max len 92, min 61)
            byte[] k1 = new byte[92]; // key1
            byte[] k2 = new byte[92]; // key2
            byte len1, len2;

            try {
                random.generateBytesFill(k1);
                len1 = (byte) (61 + random.generateInt(92 - 61 + 1));
                random.generateBytesFill(k2);
                len2 = (byte) (61 + random.generateInt(92 - 61 + 1));
            } catch (SecurityException ex) {
                ex.printStackTrace();
                break;
            }

            // copy keys to buffer (key len byte + key)
            System.arraycopy(k1, 0, CHECK_BUFFER, 70 + 1, 92);
            System.arraycopy(k2, 0, CHECK_BUFFER, 70 + 1 + 92 + 1, 92);
            CHECK_BUFFER[70] = len1;
            CHECK_BUFFER[70 + 1 + 92] = len2;
            //L.a(Settings.TAG_TCPSTATEMACHINE, "k1 " + len1 + " k2 " + len2);
            //L.a(Settings.TAG_TCPSTATEMACHINE, "kf1 " + k1[0] + " " + k1[1] + " " + k1[2] + " " + k1[3] + " " + k1[4] + " " + k1[5] + " " + k1[6]);
            //L.a(Settings.TAG_TCPSTATEMACHINE, "kf2 " + k2[0] + " " + k2[1] + " " + k2[2] + " " + k2[3] + " " + k2[4] + " " + k2[5] + " " + k2[6]);

            // crypt buffer
            while (buf.position() == 0 && cipher != null) {
                byte b = random.generateByte();
                if (b == 0 || (b & 0xFF) >= 0xC0)
                    continue;
                //L.a(Settings.TAG_TCPSTATEMACHINE, "b " + b);

                CHECK_BUFFER[0] = b;
                for (int i = 1; i < 256; i++)
                    CHECK_BUFFER[i] ^= b;

                if (cipherDoFinal(CHECK_BUFFER, 256) == -1) {
                    for (int i = 1; i < 256; i++)
                        CHECK_BUFFER[i] ^= b;
                    continue;
                }
                //L.ar(Settings.TAG_TCPSTATEMACHINE, CHECK_BUFFER, 256, 256);
                buf.put(CHECK_BUFFER, 256, 256);
            }
            if (cipher == null)
                break;
            //L.a(Settings.TAG_TCPSTATEMACHINE, "go");

            // send

            buf.flip();
            try {
                client.sockWrite(buf, true); // TODO if (written < size)
                proxySetConnectState(PROXY_STATE_AUTH_SENT);
                client.sockEnableWriteEvent(false);

                client.setCryptKeys(0, 0, 0, 0, k1, k2, 0, 0);

                ok = true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                ByteBufferPool.release(buf);
            }
        }
        while (false);

        if (!ok) {
            proxySetConnectState(PROXY_STATE_ERROR);
            return false;
        }

        return true;
    }

    // proto v0.2
    private boolean proxyAuthResponse_v02() {
        if (Settings.DEBUG_WGPROXY)
            L.a(Settings.TAG_TCPSTATEMACHINE, "proxyAuthResponse_v02 (" + Integer.toHexString(thisId) + ")");

        boolean ok = false;
        ByteBuffer buf = ByteBufferPool.alloc(ByteBufferPool.BUFFER_SIZE_SMALL);

        try {
            int read = client.sockRead(buf, true);
            //L.e(Settings.TAG_TCPSTATEMACHINE, "Read ", Integer.toString(read), " bytes");
            if (read > 0) {
                int pos = buf.position();
                buf.flip();
                buf.get(CHECK_BUFFER, 0, Math.min(CHECK_BUFFER.length, pos));

                // YO (2) + GO or BI or RD (2) + redirect address (16) + redirect token (8)
                // TODO XXX if read not full answer???
                if (pos == 2 + 2 + 16 + 8 && CHECK_BUFFER[0] == 'Y' && CHECK_BUFFER[1] == 'O') {
                    if (CHECK_BUFFER[2] == 'G' && CHECK_BUFFER[3] == 'O') {
                        // ok, working
                        proxySetConnectState(PROXY_STATE_OK);
                        boolean has = sockWriteBuf.hasRemaining();
                        client.sockEnableWriteEvent(has);

                        ok = true;
                    } else if (CHECK_BUFFER[2] == 'B' && CHECK_BUFFER[3] == 'I') {
                        // bad identifier
                        Policy.clearToken();
                        Policy.refreshToken(true);
                    } else if (CHECK_BUFFER[2] == 'R' && CHECK_BUFFER[3] == 'D') {
                        // redirect
                        byte[] ip = new byte[4];
                        ip[0] = CHECK_BUFFER[4];
                        ip[1] = CHECK_BUFFER[5];
                        ip[2] = CHECK_BUFFER[6];
                        ip[3] = CHECK_BUFFER[7];
                        byte[] token = new byte[8];
                        token[0] = CHECK_BUFFER[20];
                        token[1] = CHECK_BUFFER[21];
                        token[2] = CHECK_BUFFER[22];
                        token[3] = CHECK_BUFFER[23];
                        token[4] = CHECK_BUFFER[24];
                        token[5] = CHECK_BUFFER[25];
                        token[6] = CHECK_BUFFER[26];
                        token[7] = CHECK_BUFFER[27];
                        // TODO
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ByteBufferPool.release(buf);
        }

        if (!ok) {
            proxySetConnectState(PROXY_STATE_ERROR);
            return false;
        }

        return true;
    }

    // if socket connected to server
    public boolean onSockConnected() {
        if (Settings.DEBUG_PROFILE_NET) ev.connected += 1;

        try {
            thisId = random.generateInt();
        } catch (SecurityException ex) {
            thisId = 7;
        }
        setSeqNo(thisId);
        //L.d(Settings.TAG_TCPSTATEMACHINE, "onSockConnected in ", Integer.toHexString(thisId), " port = ", Integer.toString(localPort));

        sendSyn();
        setSeqNo(seqNo + 1);
        //packet.parseFrame();

        state = State.CONNECTED_TO_SERVER;
        resetDataTimeout(false);

        if (Settings.DEBUG_NET || Settings.DEBUG_WGPROXY) {
            L.a(Settings.TAG_TCPSTATEMACHINE, "Socket connected! (" + Integer.toHexString(thisId) + ")");
            //L.printBacktrace(Settings.TAG_TCPSTATEMACHINE, 'a');
        }

        return true;
    }

    // if socket to server disconnected
    public void onSockDisconnected() {
        state = State.HALF_CLOSED_BY_SERVER;
    }

    /*
     * if socket to server ready to be readed (return false on error)
     */
    public boolean onSockReadReady() {
        //L.d(Settings.TAG_TCPSTATEMACHINE, "onSockReadReady in ", Integer.toHexString(thisId));
        //L.a(Settings.TAG_TCPSTATEMACHINE, "onSockReadReady in ", Integer.toHexString(thisId));

        if (proxied && proxyConnectState < PROXY_STATE_OK)
            return onProxySockReady(false);

        try {
            int tmp;
            int posOld = sockReadBuf.position();
            //L.e(Settings.TAG_TCPSTATEMACHINE, "onSockReadReady seqNo = " + Integer.toHexString(seqNo));
            //L.e(Settings.TAG_TCPSTATEMACHINE, "onSockReadReady posOld = " + posOld);
            int read = client.sockRead(sockReadBuf, false);

            if (Settings.DEBUG_TCP) debug_log("read " + read, null);

            if (read < 0 || (read == 0 && posOld == 0)) {
                // no read
                if (Settings.DEBUG_PROFILE_NET)
                    if (!isFinReceived) ev.sockReadZero++;

                // TODO XXX and what if read 0 not -1?
                sendFin();

                return true;
            }

            // processing readed data

            int posNew = sockReadBuf.position();
            sockReadBuf.flip();
            sockReadBuf.position(posOld); // restore pos before read
            tmp = sockReadBuf.remaining(); // buffer may contain data from previous read (see toSend == 0)

            int checkSize = Math.min(tmp, CHECK_BUFFER.length);
            sockReadBuf.get(CHECK_BUFFER, 0, checkSize);
            tmp = checkIncomingData(CHECK_BUFFER, 0, checkSize);

            if (tmp < 0) {
                return false; // error parsing answer HTTP header
            } else if (tmp == 0) {
                // need more data
                // test with: http://login.wi-fi.ru/am/UI/Login?switch_url=http://1.1.1.1/login.html&ap_mac=18:9c:5d:9b:df:e0&client_mac=88:30:8a:50:85:b3&wlan=MosMetro_Free&redirect=vmet.ro/&org=mac&ForceAuth=true
                if (Settings.DEBUG_TCP) debug_log("need more", null);

                sockReadBuf.clear();
                sockReadBuf.position(posNew);

                tmp = sockReadBuf.remaining();
                if (read == 0 || tmp <= 0 || checkSize == CHECK_BUFFER.length)
                    // if too big HTTP header for our buffers or second read 0 and still can't parse headers
                    return false;

                resetDataTimeout(true);

                return true;
            }

            if (scanDataBufferSize != 0) {
                // data scanning requested
                // need to fill buffer up to selected size for check
                // TODO XXX may not work and contain bugs, check
                // TODO XXX what about pipelining?

                tmp = sockReadBuf.position();
                if (tmp < scanDataBufferSize) {
                    // not full, waiting for data
                    L.d(Settings.TAG_TCPSTATEMACHINE, "waiting buffer");

                    resetDataTimeout(true);
                    sockReadBuf.limit(scanDataBufferSize);
                    //client.sockEnableReadEvent(true);

                    return true;
                }

                // check
                PolicyRules rules = policy.getPolicyForData(requests.size() > 0 ? requests.get(0) : null, sockReadBuf);
                if (rules.hasPolicy(PolicyRules.DROP)) {
//					System.out.println("TEST ADS0003 ");
                    send404(true, rules.getRedirect());
                    sendFin(); // reset connection because have data from server (need to skip)

                    return false;
                }

                scanDataBufferSize = 0;
            }

            // sending data to client

            sockReadBuf.position(posOld);
            int toSend = sockReadBuf.remaining(); // posNew - posOld

            int seq = seqNo + posOld;
            int offset = 0;

            while (toSend > 0) {
                Packet packet = PacketPool.alloc(TCP_HEADER_LENGTH + toSend);
                int size = Math.min(toSend, packet.frame.length - TCP_HEADER_LENGTH);
                sockReadBuf.get(packet.frame, TCP_HEADER_LENGTH, size);

                tmp = sockWriteBuf.remaining();
                sendData(seq + offset, tmp, packet, size);

                toSend -= size;
                offset += size;
            }

            sockReadBuf.clear();
            sockReadBuf.position(posNew); // data below a pos used for retransmit (really ?)

            resetDataTimeout(true);

            // disable read if no free space in read buffer
            tmp = sockReadBuf.remaining();
            if (tmp <= 0)
                client.sockEnableReadEvent(false);

            //L.w(Settings.TAG_TCPSTATEMACHINE, "onSockReadReady() done");
            return true;
        } catch (IOException e) {
            //e.printStackTrace();
            L.w(Settings.TAG_TCPSTATEMACHINE, e.toString());
        }

        return false;
    }

    /*
     * if socket to server ready to write (return false on error)
     */
    public boolean onSockWriteReady() {
        //L.d(Settings.TAG_TCPSTATEMACHINE, "onSockWriteReady in ", Integer.toHexString(thisId));
        //L.a(Settings.TAG_TCPSTATEMACHINE, "onSockWriteReady in ", Integer.toHexString(thisId));

        if (!client.isConnected())
            return false;

        //L.w(Settings.TAG_TCPSTATEMACHINE, "onSockWriteReady() in ", this.toString());
        if (proxied && proxyConnectState < PROXY_STATE_OK)
            return onProxySockReady(true);

        boolean ready = false;
        try {
            // have any data to server?
            int pos = sockWriteBuf.position();
            if (pos <= 0) {
                client.sockEnableWriteEvent(false);
                if (!isFinReceived)
                    return true; // ok

                // client send us fin, ending server connection

                if (!proxied)
                    client.sockWriteShutdown();

                return true;
            }

            // sending data to server

            sockWriteBuf.flip();
            ready = checkAndWriteBuffer(sockWriteBuf);

            resetDataTimeout(true);

            // not all data written and ready to send rest?

            pos = sockWriteBuf.position();
            if (pos > 0 && ready)
                client.sockEnableWriteEvent(true);
            else
                client.sockEnableWriteEvent(false);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    /*
     * process proxy states on socket read/write events
     */
    private boolean onProxySockReady(boolean isWrite) {
        boolean result = true;

        if (Settings.DEBUG) L.d(Settings.TAG_TCPSTATEMACHINE, "onProxySockReady(" + isWrite + ")");

        switch (proxyConnectState) {
            case PROXY_STATE_INITED:
            case PROXY_STATE_UNAUTHORIZED:
                if (isWrite) break;
                L.d(Settings.TAG_TCPSTATEMACHINE, "invalid read event");
                //L.a(Settings.TAG_TCPSTATEMACHINE, "invalid read event");
                return false; // read event on this states - invalid

            case PROXY_STATE_HELLO_SENT:
            case PROXY_STATE_AUTH_SENT:
            case PROXY_STATE_CONNECT_SENT:
            case PROXY_STATE_PING_SENT:
                if (isWrite) return true; // nothing to do on write
                break;

            case PROXY_STATE_ERROR:
            default:
                return false;
        }

        if (isWrite) {
            switch (proxyConnectState) {
                case PROXY_STATE_INITED: // not used in proto v0.2
                    if (DEBUG_PROXY_V01) {
                        result = proxyHello_v01();
                        break;
                    } else {
                        proxyConnectState = PROXY_STATE_UNAUTHORIZED;
                    }

                case PROXY_STATE_UNAUTHORIZED:
                    if (DEBUG_PROXY_V01)
                        result = proxyAuth_v01();
                    else
                        result = proxyAuth_v02();
                    break;
            }
        } else {
            switch (proxyConnectState) {
                case PROXY_STATE_HELLO_SENT: // not used in proto v0.2
                    if (DEBUG_PROXY_V01) {
                        result = proxyHelloResponse_v01();
                        break;
                    }

                case PROXY_STATE_AUTH_SENT:
                    if (DEBUG_PROXY_V01)
                        result = proxyAuthResponse_v01();
                    else
                        result = proxyAuthResponse_v02();
                    if (!result)
                        break;

                    if (isProxyMethodConnect())
                        result = proxyConnect();
                    break;

                case PROXY_STATE_CONNECT_SENT:
                    result = proxyConnectResponse();
                    break;

                case PROXY_STATE_PING_SENT:
                    // TODO add response to ping on proxy
                    break;
            }
        }

        return result;
    }

    /*
     * return true on real timeout
     *
     * timeout work if server socket not closed:
     * timeout == TIMEOUT_RECEIVE_INITIAL (10000)
     * timeout * 2 == 20000
     * timeout * 2 == 40000
     * timeout * 2 == 80000 (>= TIMEOUT_EXPIRE_LIMIT (60000) -> keepalive test)
     * timeout == TIMEOUT_KEEPALIVE_TEST (30000)
     * goto start
     */
    public boolean onTimeout(long currentTime, int fd) {
        //L.d(Settings.TAG_TCPSTATEMACHINE, "onTimeout in ", Integer.toHexString(thisId));
        //L.d(Settings.TAG_TCPSTATEMACHINE, "SM:onTimeout");

        if (this.timeoutRestart) {
            resetDataTimeout(false);
            return false;
        }

        //L.a(Settings.TAG_TCPSTATEMACHINE, "onTimeout in ", Integer.toHexString(thisId));

        boolean keepalive = true; // send keepalive to tun?

        if (state == State.HALF_CLOSED_BY_SERVER || // see TCPClient.onTimeout
                timeout >= TIMEOUT_EXPIRE_LIMIT) {
            // close connection if server close or start/stop connection keep alive (for progs with persistent connections)

            do {
                if (state != State.HALF_CLOSED_BY_SERVER) {
                    if (proxied || client.serverKeepAliveEnabled()) {
                        if (Settings.DEBUG_NET)
                            if (!proxied)
                                L.a(Settings.TAG_TCPSTATEMACHINE, "stop keepalive test (" + fd + ") (" + Integer.toHexString(thisId) + ")");

                        // timeout with server KA enabled and connect alive, reset KA and wait
                        client.updateTimeout(TIMEOUT_RECEIVE_INITIAL, true);
                        if (!client.serverKeepAliveEnabled()) // must be false
                            break;
                    } else {
                        if (Settings.DEBUG_NET)
                            L.a(Settings.TAG_TCPSTATEMACHINE, "start keepalive test (" + fd + ") (" + Integer.toHexString(thisId) + ")");

                        if (client.enableServerKeepAlive(true)) {
                            client.updateTimeout(TIMEOUT_KEEPALIVE_TEST, false);
                            break;
                        }
                    }
                }

                if (Settings.DEBUG_NET)
                    L.a(Settings.TAG_TCPSTATEMACHINE, "timeout expired (" + fd + ")");

                // TODO XXX if state == HALF_CLOSED_BY_SERVER and have data in sockReadBuf?
                return true; // timeout
            }
            while (false);
        } else if (((state == State.CONNECT_PENDING || state == State.CONNECTING_TO_SERVER) &&
                currentTime - connectStartTime >= TIMEOUT_PENDING_VALUE) ||
                (proxied && proxyConnectState < PROXY_STATE_OK &&
                        currentTime - proxyStateChangeTime >= TIMEOUT_STATE_LIMIT)) {
            // close connection if can't connect to server (or authenticate) in time limit

            if (Settings.DEBUG_NET || Settings.DEBUG_WGPROXY)
                L.a(Settings.TAG_TCPSTATEMACHINE, "Pending connection timed out. (" + Integer.toHexString(thisId) + ")");

            return true; // timeout
        } else if (state == State.HALF_CLOSED_BY_APP) {
            // time to close client connection expire

            client.sockClose(false);
            return true;
        } else {
            // time limit not exceeded, increase timeout
            //L.e(Settings.TAG_TCPSTATEMACHINE, "TIMEOUT, RETRANSMIT");

            if (sendRetransmit()) // maybe need client retransmit?
                keepalive = false;

            sameAckCount = 0;
            updateTimeout(timeout * 2L, false);
        }

        // always send keepalive to tun, because we don't now if server send it to client

        if (keepalive) {
            if (Settings.DEBUG_NET) {
                if (keepaliveCounter != 0)
                    L.a(Settings.TAG_TCPSTATEMACHINE, "keepalive counter " + keepaliveCounter + " (" + fd + ")");
                keepaliveCounter = 0;
            }

            sendKeepAlive(sockWriteBuf.remaining());
        }

        return false; // no timeout
    }

    /*
     * save current timeout value and update server client timeout
     * nosave - didn't change current timeout value (see onTimeout timeout * 2L)
     */
    private void updateTimeout(long timeout, boolean nosave) {
        if (state == State.HALF_CLOSED_BY_APP)
            return;

        if (!nosave)
            this.timeout = timeout;
        timeoutRestart = false;

        client.updateTimeout(timeout);
    }

    // function to reset initial recive/send timeout
    private void resetDataTimeout(boolean fast) {
        //if (fast)
        if (false) {
            if (!timeoutRestart) {
                timeoutRestart = true; // fast, update timeout on next timeout
                L.a(Settings.TAG_TCPSTATEMACHINE, "timeoutRestart in ", Integer.toHexString(thisId));
            }
            return;
        }

        updateTimeout(TIMEOUT_RECEIVE_INITIAL, false);
    }

    /*
     * Checks the data arrived to server
     * TODO XXX big and bloat function! rewrite some parts
     */
    private boolean checkAndWriteBuffer(ByteBuffer buffer) {
        //L.d(Settings.TAG_TCPSTATEMACHINE, "checkAndWriteBuffer in ", Integer.toHexString(thisId));
        int partialRequestSize = 0;
        byte[] partialRequest = null;
        int requestsSize = -1;

        if (isHttpProtocol && !wasPostDetected && policy != null)
            do {
                int ppos = buffer.position(); // Should be equal to 0.
                int dataSize = buffer.remaining();

                int nextReqPos = ppos;
                int startRequestCount = requests.size();
                int requestsInBuffer = 0;
                boolean isPostRequest = false;
                boolean hasDrop = false;
                String redirect = null;

                if (Settings.DEBUG) {
                    if (requests.size() > 0) {
                        L.w(Settings.TAG_TCPSTATEMACHINE, "Requests in queue: " + requests.size());
                        for (RequestHeader requestHeader : requests)
                            L.w(Settings.TAG_TCPSTATEMACHINE, requestHeader.getUrl());
                    }
                }

                int counter = 0;
                RequestHeader req;
                while ((req = RequestHeader.parse(buffer, serverIp, client.isBrowser(), client.getUid(), proxied, policy)) != null) {
                    counter++;

                    // A workaround for Opera Classic: when compression mode is enabled, it sends the referrer only the first time.

                    if (client.isOperaClassic()) // TODO XXX temporary check for CHARGEABLE sites detect with refferer problem
                    {
                        if (req.referer == null && lastReferrer != null) {
                            req.referer = lastReferrer;
                        } else if ((req.referer != null && req.referer.equals("-")) || (req.referer == null && lastReferrer == null)) {
                            lastReferrer = req.host;
                            req.referer = req.host;
                        } else if (req.referer != null) {
                            lastReferrer = req.referer;
                        }
                    }

                    // The end of the workaround.

                    PolicyRules rules = policy.getPolicy(req, client.getUid(), client.getPkg0(), client.isBrowser(), serverIp);
                    boolean drop = rules.hasPolicy(PolicyRules.DROP);
                    boolean wait = rules.hasPolicy(PolicyRules.WAIT);

                    if (!wait && !drop) {
                        if (req.isModified()) {
                            rules.addPolicy(PolicyRules.MODIFY);
                        }
                    }
                    req.policyRules = rules;

                    //L.d(Settings.TAG_TCPSTATEMACHINE, "Policy for ", req.getUrl(), " ", req.policyRules.toString());

                    if (rules.hasPolicy(PolicyRules.NOTIFY_USER)) {
                        IUserNotifier notifier = client.getUserNotifier();
                        if (notifier != null)
                            notifier.notify(rules, req.host, Utils.getDomain(req.referer));
                    }

                    if (drop) {
                        hasDrop = true; // TODO XXX and what about pipelining? (need tests)
                        redirect = rules.getRedirect();
                    }

                    // This is the case when there’s an incomplete request at the end of the buffer — we read it into a special buffer and then append to it later.
                    if (wait) {
                        if (Settings.DEBUG) {
                            L.w(Settings.TAG_TCPSTATEMACHINE, Integer.toString(requests.size()), " requests in WAIT");
                        }

                        buffer.clear();
                        buffer.position(dataSize);

                        L.w(Settings.TAG_TCPSTATEMACHINE, "Partial request, waiting for missing part");
                        return false;

                    } else {
                        // All other requests are saved in a list, then they will be sent sequentially through the socket buffer.
                        if (!requestsAllocated && requests.size() == 1) {
                            // increase capacity on pipelining first use
                            requestsAllocated = true;
                            requests.ensureCapacity(6);
                        }
                        requests.add(req);
                        isPostRequest = req.isPostRequest();
                    }

                    // When parsing the next request, we will know the position where the parsing started.
                    nextReqPos = buffer.position();
                    requestsInBuffer++;

                    if (Settings.DEBUG_HTTP)
                        L.a(Settings.TAG_TCPSTATEMACHINE, "Request: ", req.toString());
                } // while ((req

                if (counter == 0) {
                    isHttpProtocol = false; // disable http parsing in this connection
                    if (!isFirstRequest)
                        L.w(Settings.TAG_TCPSTATEMACHINE, "Unexpected binary data");
                    break;
                }
                if (isFirstRequest)
                    isFirstRequest = false;

                if (Settings.DEBUG_HTTP)
                    L.a(Settings.TAG_TCPSTATEMACHINE, "Sending ", Integer.toString(requests.size()), " requests");

                if (isPostRequest) {
                    wasPostDetected = true;
                    partialRequestSize = buffer.remaining();
                    partialRequest = new byte[partialRequestSize];
                    buffer.get(partialRequest, 0, partialRequestSize);
                }

                if (hasDrop && requests.size() == 1) {
                    // no pipelining, so send 404
                    //L.w(Settings.TAG_TCPSTATEMACHINE, "Sending 404 in ");

                    setAckNo(ackNo + sockWriteBuf.position());
                    sockWriteBuf.clear();
                    requests.clear();

                    send404(false, redirect);
                    is404 = true;

                    return false;
                }

                sockWriteBuf.clear();

                requestsSize = requests.size();
                if (requestsSize > 0) {
                    for (int i = startRequestCount; i < requestsSize; i++) {
                        RequestHeader r = requests.get(i);

                        if (Settings.DEBUG_HTTP)
                            L.a(Settings.TAG_TCPSTATEMACHINE, "Writing request to sockWriteBuf:\n", r.getHeaders());

                        if (!r.policyRules.hasPolicy(PolicyRules.DROP)) {
                            int size = r.getRequestWithData(CHECK_BUFFER);
                            //String s = new String(CHECK_BUFFER, 0, 0, size);
                            //L.w(Settings.TAG_TCPSTATEMACHINE, "Requested:\n", s);
                            sockWriteBuf.put(CHECK_BUFFER, 0, size);
                        }
                    }

                    if (isPostRequest && partialRequest != null) {
                        sockWriteBuf.put(partialRequest);
                        partialRequest = null;
                        partialRequestSize = 0;
                    }

                    sockWriteBuf.flip();

                    int size = sockWriteBuf.remaining();
                    if (dataSize != size) // maybe CHECK_BUFFER.length
                        setAckNo(ackNo - (size - dataSize));
                }
            }
            while (false); // if (isHttpProtocol)

        // send data (binary or http)

        if (requestsSize != 0) // ==-1 on binary data or >0 if need to send http data
        {
            try {
                int size = sockWriteBuf.remaining();
                int written = client.sockWrite(sockWriteBuf, false);

                if (Settings.DEBUG_TCP) debug_log("write " + size + " (" + written + ")", null);

                if (written < size)
                    // In theory, this shouldn’t happen; all data should have been sent.
                    sockWriteBuf.compact();
                else
                    sockWriteBuf.clear();

                setAckNo(ackNo + written);

                if (timeout > TIMEOUT_RECEIVE_INITIAL)
                    resetDataTimeout(true);
                if (written > 0)
                    sendAck();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // send data from POST request (http)
        if (partialRequestSize > 0) {
            sockWriteBuf.put(partialRequest, 0, partialRequestSize);
        }

        return true;
    }

    /*
     * checks the data arrived from server
     *
     * TODO XXX big and bloat function! rewrite some parts
     * TODO XXX may incorrect work with chunked and pipelining
     *
     * @param data	 - data from server
     * @param offset - offset of the data
     * @param size	 - size of the data
     * @return - < 0 on error, 0 if need more data, > 0 if the http-header was parsed fully, or anyway the data can be delivered to browser
     */
    private int checkIncomingData(byte[] data, int offset, int size) {
        //L.d(Settings.TAG_TCPSTATEMACHINE, "checkIncomingData in ", Integer.toHexString(thisId));
        // No requests were made; some unknown data arrived, or it might be something encoded.
        if (requests.size() == 0)
            return size;

        //L.d(Settings.TAG_TCPSTATEMACHINE, "To " + Utils.ipToString(localIp, localPort));

        RequestHeader req = requests.get(0);

        if (req.loadedLength == 0 && req.response == null) {
            //L.w(Settings.TAG_TCPSTATEMACHINE, "Got data for: ", req.toString(), " requests: " + requests.size());
            // The response to one of the requests has just started arriving.
            if (req.policyRules.hasPolicy(PolicyRules.DROP)) {
                // The request was not sent to the server; we send a 404 response to the browser and ignore the request.
                send404(false, req.policyRules.getRedirect());
                requests.remove(0);

                if (requests.size() == 0)
                    return 0;

                int s = checkIncomingData(data, offset, size);
                return s;
            }

            ResponseHeader resp = ResponseHeader.parse(data, offset, size);
            if (resp == null) {
                L.d(Settings.TAG_TCPSTATEMACHINE, "not http response");
                return size;
            } else if (!resp.full) {
                L.d(Settings.TAG_TCPSTATEMACHINE, "not full response header");
                return 0;
            }

            if (policy == null)
                return size;

            // parse response

            if (Settings.DEBUG)
                L.w(Settings.TAG_TCPSTATEMACHINE, "ResponseHeader: ", resp.toString());

            // TODO XXX and what about else? (see below)
            req.response = resp;
            PolicyRules rules = policy.getPolicy(req, resp, data, client.getUid(),
                    client.isBrowser(), proxied, client.getPkg0());
            req.policyRules = rules;

            scanDataBufferSize = policy.getScanDataBufferSize(req, resp);

            if (rules.hasPolicy(PolicyRules.HASH))
                hashData(req, resp, data, offset, size);

            if (rules.hasPolicy(PolicyRules.SCAN))
                scanData(req.response, data, offset, size);

            if (rules.hasPolicy(PolicyRules.DROP)) {
                send404(true, rules.getRedirect());
                sendFin(); // reset connection because have data from server (need to skip)

                if (rules.hasPolicy(PolicyRules.NOTIFY_USER)) {
                    IUserNotifier notifier = client.getUserNotifier();
                    if (notifier != null)
                        notifier.notify(rules, req.host);
                }
                requests.clear();

                return 0;
            }

            int dataSize = size - resp.headerLength;
            int nextResponseOffset = 0;

            if (Settings.DEBUG)
                L.w(Settings.TAG_TCPSTATEMACHINE, "dataSize in this packet: " + dataSize + " resp.headerLength = " + resp.headerLength);

            if (!resp.chunked) {
                int dataForCurrentFile = resp.contentLength - req.loadedLength;
                if (dataSize > dataForCurrentFile || dataForCurrentFile == 0) {
                    nextResponseOffset = offset + resp.headerLength + dataForCurrentFile;
                    dataSize = dataForCurrentFile;
                }

                if (dataSize > 0) {
                    req.loadedLength += dataSize;
                }

                if (resp.contentLength == 0 || req.loadedLength >= resp.contentLength) {
                    if (Settings.DEBUG)
                        L.w(Settings.TAG_TCPSTATEMACHINE, "Data loaded fully for ", req.toString());

                    requests.remove(0);
                }

                int res = dataSize + resp.headerLength;

                if (nextResponseOffset > 0 && dataSize > 0) {
                    if (Settings.DEBUG)
                        L.e(Settings.TAG_TCPSTATEMACHINE, "We have next response in this packet!");

                    int s = checkIncomingData(data, nextResponseOffset, size + offset - nextResponseOffset);
                    if (s < 0)
                        return s;

                    res += s;
                }

                return res;
            } else if (dataSize > 0) {
                if (Settings.DEBUG) L.d(Settings.TAG_TCPSTATEMACHINE, "Start of chunked response");

                chunkedReader.clear();
                if (!chunkedReader.read(data, offset + resp.headerLength, dataSize/*size-resp.headerLength*/))
                    return -1;

                if (chunkedReader.isAtEndOfFile()) {
                    if (Settings.DEBUG)
                        L.w(Settings.TAG_TCPSTATEMACHINE, "Data loaded fully for ", req.toString());

                    requests.remove(0);

                    int fileOffset = chunkedReader.getFileEndOffset();
                    int res = fileOffset - offset;
                    if (fileOffset < offset + size) {
                        if (Settings.DEBUG)
                            L.e(Settings.TAG_TCPSTATEMACHINE, "We have next response in this packet! (after chunked)");

                        int s = checkIncomingData(data, fileOffset, size + offset - fileOffset);
                        if (s < 0)
                            return s;

                        res += s;
                    }

                    return res;
                }

                // if use function return value as size of data to send (with header) this return are invalid
                // and other 'return res;' also
                return dataSize;
            } else {
                // Only the header was received, but it's chunked.
                requests.remove(0);
                return size;
            }
        } else {
            //L.w(Settings.TAG_TCPSTATEMACHINE, "Data continuation for " + req.getUrl());

            if (req.policyRules != null) {
                if (req.policyRules.hasPolicy(PolicyRules.HASH))
                    hashData(req, req.response, data, offset, size);

                if (req.policyRules.hasPolicy(PolicyRules.SCAN) && policy != null)
                    scanData(req.response, data, offset, size);

                // TODO XXX hmm, process rules.hasPolicy(PolicyRules.DROP) ?
            }

            if (!req.response.chunked) {
                int dataSize = size;
                int nextResponseOffset = 0;

                int dataForCurrentFile = req.response.contentLength - req.loadedLength;
                if (dataSize > dataForCurrentFile) {
                    nextResponseOffset = offset + dataForCurrentFile;
                    dataSize = dataForCurrentFile;
                }

                if (dataSize > 0)
                    req.loadedLength += dataSize;

                if (req.loadedLength >= req.response.contentLength) {
                    if (Settings.DEBUG)
                        L.w(Settings.TAG_TCPSTATEMACHINE, "Data loaded fully for ", req.toString());

                    requests.remove(0);
                }

                int res = dataSize;

                if (nextResponseOffset > 0 && dataSize > 0) {
                    if (Settings.DEBUG)
                        L.e(Settings.TAG_TCPSTATEMACHINE, "We have next response in this packet!");

                    int s = checkIncomingData(data, nextResponseOffset, size + offset - nextResponseOffset);
                    if (s < 0)
                        return s;

                    res += s;
                }

                return res;
            } else {
                if (Settings.DEBUG)
                    L.d(Settings.TAG_TCPSTATEMACHINE, "Continuation of chunked response");

                if (!chunkedReader.read(data, offset, size))
                    return -1;

                if (chunkedReader.isAtEndOfFile()) {
                    if (Settings.DEBUG)
                        L.w(Settings.TAG_TCPSTATEMACHINE, "Data loaded fully for ", req.toString());

                    requests.remove(0);

                    int fileOffset = chunkedReader.getFileEndOffset();
                    int res = fileOffset - offset;
                    if (fileOffset < offset + size) {
                        if (Settings.DEBUG)
                            L.e(Settings.TAG_TCPSTATEMACHINE, "We have next response in this packet! (after chunked)");

                        int s = checkIncomingData(data, fileOffset, size + offset - fileOffset);
                        if (s < 0)
                            return s;

                        res += s;
                    }

                    return res;
                }

                return size;
            }
        } // if (req.loadedLength == 0 && req.response == null) ... else
    }

    private void hashData(RequestHeader request, ResponseHeader response, byte[] data, int offset, int size) {
        if (hasher == null) {
            //L.d("TCP", "Creating hasher!");
            hasher = new Hasher((request != null) ? request.getUrl() : null, hasherListener);
            if (response.headerLength > 0)
                hasher.update(data, response.headerLength + offset, size - response.headerLength);
        } else {
            hasher.update(data, offset, size);
        }

        if (response.contentLength > 0) {
            if (response.contentLength == hasher.getSize()) {
                hasher.finish();
                hasher = null;
            }
        }
    }

    private void scanData(ResponseHeader response, byte[] data, int offset, int size) {
        L.d(Settings.TAG_TCPSTATEMACHINE, "ScanData");
        if (buffer == null) {
            //L.d("TCP", "Starting buffering for: " + request.getUrl());
            if (response.contentLength > 0)
                buffer = MemoryCache.alloc(response.headerLength + response.contentLength);
            else
                buffer = MemoryCache.alloc(0);
        }

        buffer.write(data, offset, size);

        //L.d("TCP", "Buffered data size: " + buffer.size() + " added " + size + " bytes " + " for:\n" + request.getUrl());

        if (response.chunked) {
            if (Utils.isEndOfChuncked(data, offset, size)) {
                scanDataBuffered = true;
                //L.d("TCP", "Buffered all chunked data for: " + request.getUrl());
                policy.scan(buffer, this);
            }
        } else if (buffer.size() >= response.headerLength + response.contentLength) {
            scanDataBuffered = true;
            //L.d("TCP", "Buffered all data for: " + request.getUrl());
            policy.scan(buffer, this);
        }
    }

    /*
     * process packet from tun
     * return false on error
     * TODO XXX big and bloat function! some TCP parts maybe wrong. rewrite some parts
     */
    public boolean onTunInput(Packet packet) {
        //L.d(Settings.TAG_TCPSTATEMACHINE, "onTunInput in ", Integer.toHexString(thisId), " flag = ", Integer.toString(packet.flag));
        //L.d(Settings.TAG_TCPSTATEMACHINE, "onTunInput, packet: " + packet, "\n in ", this.toString());
        if (Settings.DEBUG_TCP) debug_log("in", packet);
        if (Settings.DEBUG_PROFILE_NET) ev.onTunInput++;

        if ((packet.flag & Packet.TCP_FLAG_RST) != 0) {
            // rst, finishing
            L.i(Settings.TAG_TCPSTATEMACHINE, "Closing socket on RST");
            if (Settings.DEBUG_PROFILE_NET) ev.receivedRst++;

            forceClose(false);
            return false;
        }

        if (isBadState()) {
            forceClose(true);
            return false;
        }

        // processing 1 (in data)

        do {
            if (isFinReceived) {
                // packet after fin from app
                if (Settings.DEBUG_PROFILE_NET) ev.tunInputAfterFinReceived++;
                break;
            }

            if (!isSeqnoMatch(packet)) {
                // hmm, invalid SeqNo

                int pos = sockWriteBuf.position();
                if (packet.flag == Packet.TCP_FLAG_ACK && packet.seqNo == ackNo + pos - 1) // ackNo + pos
                {
                    // keepalive
                    sendAck();
                    return true;
                }

                if (Settings.DEBUG)
                    L.e(Settings.TAG_TCPSTATEMACHINE, "Invalid seqNo! Sending ack! in ", Integer.toHexString(thisId));

                if (onInvalidSeqno(packet, false)) // process seqno mismatch
                {
                    L.i(Settings.TAG_TCPSTATEMACHINE, "Too many invalid seqNo, closing socket!");
                    return false;
                }

                sendAck();
                break; // skip
            }

            // get data from packet

            if (Settings.DEBUG_PROFILE_NET) ev.receivedSeqnoMatch++;

            invalidSeqNoCounter = 0;
            int dataSize = packet.totalLen - packet.dataOfs;
            boolean timeUp = true;

            if (dataSize <= 0) {
                // empty packet
                if (Settings.DEBUG_PROFILE_NET) ev.receivedSeqnoHasNoData++;

                if (keepaliveSeqNo != 0) {
                    // keepalive response
                    if (keepaliveSeqNo + 1 == packet.ackNo) {
                        //L.d(Settings.TAG_TCPSTATEMACHINE, "keepalive resp");
                        keepaliveCounter--;
                        timeUp = false;
                    }
                    keepaliveSeqNo = 0;
                }
            } else {
                // have data, saving to buffer
                if (Settings.DEBUG_PROFILE_NET) ev.receivedSeqnoHasData++;

                // TODO XXX if buffer not enough to save all data, part of data dropped
                sockWriteBuf.put(packet.frame, packet.dataOfs, Math.min(sockWriteBuf.remaining(), dataSize));
                sendAck();

                if (!proxied || proxyConnectState == PROXY_STATE_INITED || proxyConnectState == PROXY_STATE_OK) {
                    try {
                        client.sockEnableWriteEvent(true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (is404) // if instead of previous request send 404 but didn't close connection - reset flag
                    is404 = false;
            }

            if (timeUp)
                resetDataTimeout(true);

            //

            if ((packet.flag & Packet.TCP_FLAG_FIN) != 0) {
                // app close connection

                isFinReceived = true;
                sendAck();
                setClosingState();

                // if have any data to server in buffer enable sending
                boolean has = (is404 || isFinSent) ? false : sockWriteBuf.position() > 0;
                try {
                    client.sockEnableWriteEvent(has);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (!isFinSent && !has)
                    sendFin();
                if (tryClose())
                    return false;
            }

        }
        while (false);

        // processing 2 (update seqNo)

        do {
            int delta = packet.ackNo - seqNo; // Ack?
            sockReadBuf.flip();
            int remaining = sockReadBuf.remaining();

            if (delta < 0 || delta > remaining + 1) {
                boolean close = onInvalidSeqno(packet, true); // process invalid ack
                if (close && !isFinSent) {
                    L.i(Settings.TAG_TCPSTATEMACHINE, "Too many invalid Acks, closing socket!");
                    return false;
                }
                break;
            }

            int pos = 0;

            if (isFinSent && delta == remaining + 1) {
                // receive ack for our fin

                pos = delta - 1; // TODO XXX if !isReadyToClose and have data to server?
                isFinAckReceived = true;

                if (tryClose())
                    return false;
            } else {
                pos = delta;
            }

            // TODO XXX hmm .IllegalArgumentException: Bad position (limit 0): 1
            sockReadBuf.position((pos > sockReadBuf.limit()) ? 0 : pos);
            setSeqNo(seqNo + pos);
            break;
        }
        while (false);

        // processing 3 (retransmit)

        sockReadBuf.compact();

        if (hasReceivedDupAck(packet)) {
            if (onDupAck()) // process dup ack
            {
                L.i(Settings.TAG_TCPSTATEMACHINE, "Too many Dup Acks, closing socket!");
                return false;
            }

            L.i(Settings.TAG_TCPSTATEMACHINE, "DupAck - retransmit");
            if (Settings.DEBUG_PROFILE_NET) ev.retransmit++;

            sendRetransmit();
        }

        //

        // disable read if no free space in read buffer or enable
        int remaining = sockReadBuf.remaining();
        try {
            if (remaining <= 0)
                client.sockEnableReadEvent(false);
            else if (!isFinSent)
                client.sockEnableReadEvent(true); // TODO XXX maybe add here variable that read disabled?
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true; // all ok
    }

    // return true if too many SeqNo mismatch or invalid Ack
    private boolean onInvalidSeqno(Packet packet, boolean isAck) {
        if (Settings.DEBUG_PROFILE_NET) {
            if (isAck) ev.invalidAckNo++;
            else ev.receivedSeqnoMismatch++;
        }
        if (Settings.DEBUG_TCP) debug_log((isAck) ? "invAck" : "invSeq", null);

        invalidSeqNoCounter++;

        if (Settings.DEBUG) {
            int remaining = sockReadBuf.remaining();
            Object[] logBuf;
            logBuf = new Object[3];
            logBuf[0] = seqNo;
            logBuf[1] = ((seqNo + remaining) + 1);
            logBuf[2] = packet.ackNo;
            L.i(Settings.TAG_TCPSTATEMACHINE, String.format("Invalid ackNo or seqNo! seqNo=%08X-%08X, but ack is %08X", logBuf));
        }

        if (invalidSeqNoCounter > TCP_INVALID_SEQNO_MAX)
            return true;

        return false;
    }

    // return true if too many DupAcks
    private boolean onDupAck() {
        dupAckCounter++;

        if (Settings.DEBUG_TCP) debug_log("dupAck", null);
        L.i(Settings.TAG_TCPSTATEMACHINE, "dupAck!");

        if (dupAckCounter > TCP_DUPACK_MAX)
            return true;

        return false;
    }

    // close server and client connects. don't use on processing error.
    private void forceClose(boolean sendRst) {
        // it's no need to trigger timeout (or set isDead) to clean up client (TCPClient.destroy)
        // because this func called from functions that called from main cycle (ProxyWorker.run)
        // and all this functions exit with fail after forceClose()

        client.sockClose(true); // TODO XXX close without pool?
        if (sendRst)
            sendRst();

        freeBuffers();
    }

    // return true if connection was gracefully closed
    private boolean tryClose() {
        if (!isReadyToClose())
            return false;

        if (Settings.DEBUG_PROFILE_NET) ev.closeGracefully++;

        client.sockClose(false);
        freeBuffers();

        return true;
    }

    private boolean tunWrite(Packet packet) {
        if (isBadState()) {
            // TODO XXX setDead only because we don't check write state and use buffers
            //forceClose(true);
            client.setDead();
            return false;
        }

        client.tunWrite(packet);
        return true;
    }

    // TODO XXX rewrite this to use sendText or sendBuffer
    public void send404(boolean isClose, String redirect) {
        if (Settings.DEBUG)
            L.d(Settings.TAG_TCPSTATEMACHINE, "send404 in ", Integer.toHexString(thisId));

        String content = null;
        if (client.isBrowser()) {
            // return HTML version only for browsers
            // some browsers will not display custom error pages unless they are larger than 512 bytes
            //
            // may be add JS redirect timeout?
            // http://stackoverflow.com/questions/16873323/javascript-sleep-wait-before-continuing

            content = "<html><head><meta name=\"description\" content=\"Not Found																																																																																							   \"></head><body>";
            if (redirect == null || redirect.length() > REDIRECT_MAX_LENGTH) {
                // TODO XXX make static to exclude dynamic +
                content += "<div id=\"content\">&nbsp;</div> <script language=\"JavaScript\">if (window.self === window.top) { var dv = document.getElementById(\"content\"); dv.innerHTML = \"<H1>blocked by " + Settings.APP_NAME + "</H1>\"; }</script></body></html>";
            } else {
                content += "&nbsp;<script language=\"JavaScript\">if (window.self === window.top) { self.location=\"" + redirect + "\"; }</script></body></html>";
            }
        }
        int dataLength = (content == null) ? 0 : content.getBytes().length;

        // may be disable browser cache?
        // Cache-Control: no-cache, no-store, must-revalidate
        // Pragma: no-cache
        // Expires: 0

        String header = "HTTP/1.1 404 Not Found\r\n";
        if (isClose)
            header += "Connection: close\r\n";
        if (dataLength > 0)
            header += "Content-Type: text/html; charset=utf-8\r\nContent-Length: " + dataLength + "\r\n\r\n" + content;
        else
            header += "\r\n";

        byte[] headerBytes = header.getBytes();
        Packet packet = PacketPool.alloc(TCP_HEADER_LENGTH + headerBytes.length);
        System.arraycopy(headerBytes, 0, packet.frame, TCP_HEADER_LENGTH, headerBytes.length);

        //if (DEBUG) L.d("TCP", "Sending 404 header buffer of " + headerBytes.length + " bytes");

        int pos = sockReadBuf.position();
        sockReadBuf.clear();
        sockReadBuf.put(headerBytes);

        sendData(seqNo + pos, sockWriteBuf.remaining(), packet, headerBytes.length);

    }


    private void sendData(int seqNo, int windowSize, Packet packet, int dataSize) {
        if (Settings.DEBUG_TCP) debug_log("send " + dataSize, null);

        if (policy != null) // if we can modify something at all
            windowSize = Math.max(windowSize - TCP_WINDOW_HOLE, 0);

        int ack = calcAckToSend();
        packet.addIpTcpHeader(serverIp, serverPort, localIp, localPort, seqNo, ack, 0x18, windowSize, dataSize);

        tunWrite(packet);
    }

    private void sendSyn() {
        if (Settings.DEBUG_TCP) debug_log("syn", null);

        Packet packet = PacketPool.alloc(PacketPool.POOL1_PACKET_SIZE);
        int windowSize = sockWriteBuf.remaining();
        packet.addIpTcpHeader(serverIp, serverPort, localIp, localPort, seqNo, ackNo, 0x12, windowSize, 0);

        if (Settings.DEBUG) L.w(Settings.TAG_TCPSTATEMACHINE, "sendSyn packet: " + packet);

        tunWrite(packet);
    }

    private void sendAck() {
        sendAck(sockWriteBuf.remaining());
    }

    private void sendAck(int windowSize) {
        if (Settings.DEBUG_TCP) debug_log("ack", null);

//		  L.d(Settings.TAG_TCPSTATEMACHINE, "sendAck " + windowSize);
        int seq = seqNo + sockReadBuf.position();

        if (isFinSent)
            seq++;

        // if we can modify something at all
        if (policy != null)
            windowSize = Math.max(windowSize - TCP_WINDOW_HOLE, 0);

        Packet packet = PacketPool.alloc(PacketPool.POOL1_PACKET_SIZE);
        int ack = calcAckToSend();
        packet.addIpTcpHeader(serverIp, serverPort, localIp, localPort, seq, ack, 0x10, windowSize, 0);

        tunWrite(packet);
    }

    // send Fin, Ack to app
    private void sendFin() {
        sendFin(sockWriteBuf.remaining());
    }

    private void sendFin(int windowSize) {
        if (Settings.DEBUG_TCP) debug_log("fin", null);

        if (policy != null) // if we can modify something at all
            windowSize = Math.max(windowSize - TCP_WINDOW_HOLE, 0);

        //L.d(Settings.TAG_TCPSTATEMACHINE, "sendFin " + winSize);

        Packet packet = PacketPool.alloc(PacketPool.POOL1_PACKET_SIZE);
        int seq = seqNo + sockReadBuf.position();
        int ack = calcAckToSend();
        packet.addIpTcpHeader(serverIp, serverPort, localIp, localPort, seq, ack, 0x11, windowSize, 0);

        tunWrite(packet);

        isFinSent = true;
        setClosingState();
    }

    private boolean sendRetransmit() {
        int pos = sockReadBuf.position();
        if (pos <= 0 || isFinSent)
            return false;

        if (Settings.DEBUG_TCP) debug_log("ret", null);
        //L.d(Settings.TAG_TCPSTATEMACHINE, "sendRetransmit() in ", client.toString());

        int limit, count, size;
        Packet packet;
        sockReadBuf.flip();
        limit = sockReadBuf.limit();
        count = 0;

        while (count < limit) {
            //L.d(Settings.TAG_TCPSTATEMACHINE, "sendRetransmit data");
            packet = PacketPool.alloc(TCP_HEADER_LENGTH + limit - count);
            size = Math.min(limit - count, packet.frame.length - TCP_HEADER_LENGTH);
            sockReadBuf.get(packet.frame, TCP_HEADER_LENGTH, size);

            sendData(seqNo + count, sockWriteBuf.remaining(), packet, size);

            count += size;
        }

        sockReadBuf.clear();
        sockReadBuf.position(limit);
        lastRetransmitTime = System.currentTimeMillis();

        return true;
    }

    // send RST with ACK (all packets after the initial SYN packet sent by the client should have this flag set)
    public void sendRst() {
        if (Settings.DEBUG_TCP) debug_log("rst", null);
        //L.d(Settings.TAG_TCPSTATEMACHINE, "sendRst");

        if (sockReadBuf == null)
            return;

        Packet packet;
        int seq = seqNo + sockReadBuf.position();

        if (isFinSent)
            seq++;

        packet = PacketPool.alloc(PacketPool.POOL1_PACKET_SIZE);
        int ack = calcAckToSend();
        packet.addIpTcpHeader(serverIp, serverPort, localIp, localPort, seq, ack, 0x14, 0, 0);

        client.tunWrite(packet); // to avoid recursion from TCPStateMachine.tunWrite
    }

    // prepare RST packet to send on bad packet (known connection), see ProxyWorker
    public static Packet getRst(Packet packet) {
        //L.d(Settings.TAG_TCPSTATEMACHINE, "getRst " + packet.srcPort);

        Packet packetRst = PacketPool.alloc(PacketPool.POOL1_PACKET_SIZE);
        packetRst.addIpTcpHeader(packet.dstIp, packet.dstPort, packet.srcIp, packet.srcPort, 0,
                packet.seqNo + 1, 0x14, 0, 0);

        return packetRst;
    }

    // prepare RST packet to send on bad packet (unknown connection), see ProxyWorker
    public static Packet getRst0(Packet packet) {
        //L.d(Settings.TAG_TCPSTATEMACHINE, "getRst0 " + packet.srcPort);

        Packet packetRst = PacketPool.alloc(PacketPool.POOL1_PACKET_SIZE);
        packetRst.addIpTcpHeader(packet.dstIp, packet.dstPort, packet.srcIp, packet.srcPort,
                packet.ackNo, 0, 4, 0, 0);

        return packetRst;
    }

    // send keepalive, see sendAck
    public void sendKeepAlive(int windowSize) {
        if (isFinSent)
            return;

        if (Settings.DEBUG_TCP) debug_log("ka", null);
        //L.d(Settings.TAG_TCPSTATEMACHINE, "keepalive ack");

        // if we can modify something at all
        if (policy != null)
            windowSize = Math.max(windowSize - TCP_WINDOW_HOLE, 0);

        Packet packet = PacketPool.alloc(PacketPool.POOL1_PACKET_SIZE);
        int seq = seqNo + sockReadBuf.position() - 1;
        int ack = calcAckToSend();
        packet.addIpTcpHeader(serverIp, serverPort, localIp, localPort, seq, ack, 0x10, windowSize, 0);

        keepaliveSeqNo = seq;
        keepaliveCounter++;

        tunWrite(packet);
    }

    private void debug_log(String msg, Packet packet) {
        String info = "";
        if (packet != null) {
            info = " (";
            info += ((packet.flag & Packet.TCP_FLAG_RST) != 0) ? "R" : "";
            info += ((packet.flag & Packet.TCP_FLAG_FIN) != 0) ? "F" : "";
            info += (packet.flag == Packet.TCP_FLAG_ACK) ? "A" : "";
            info += (packet.totalLen - packet.dataOfs > 0) ? "D" : "";
            info += ")";
        }

        L.a(Settings.TAG_TCPSTATEMACHINE, thisId + " " + localPort + " -> " + serverPort + ": " + msg + info);
    }

    public void dump() {
        if (Settings.DEBUG) {
            String[] r3;
            int i0, i1;
            r3 = this.toString().split("\n");
            i0 = r3.length;
            i1 = 0;

            while (i1 < i0) {
                L.i(Settings.TAG_TCPSTATEMACHINE, r3[i1].trim());
                i1 = i1 + 1;
            }
        }
    }

    public String toString() {
        if (Settings.DEBUG) {
            StringBuilder r6, r29, r42, r75, r88, r117, r132, r145;
            Object[] r7, r31, r44, r77, r90, r119, r134, r147;
            String r26, r40, r51, r86, r99, r100, r107, r115, r130, r143;

            r6 = (new StringBuilder()).append("");
            r7 = new Object[10];
            r7[0] = localIp[0] & 0xFF;
            r7[1] = localIp[1] & 0xFF;
            r7[2] = localIp[2] & 0xFF;
            r7[3] = localIp[3] & 0xFF;
            r7[4] = localPort;
            r7[5] = serverIp[0] & 0xFF;
            r7[6] = serverIp[1] & 0xFF;
            r7[7] = serverIp[2] & 0xFF;
            r7[8] = serverIp[3] & 0xFF;
            r7[9] = serverPort;
            r26 = r6.append(String.format("  src=%d.%d.%d.%d:%d dst=%d.%d.%d.%d:%d\n", r7)).toString();

            if (sockWriteBuf == null) {
                r130 = r26 + "sockWriteBuf: null\n";
                r132 = new StringBuilder().append(r130);
                r134 = new Object[2];
                r134[0] = ackNo;
                r134[1] = ackNo;
                r51 = r132.append(String.format("ack: base=0x%08X(%d)(=sockWriteBuf[0])\n", r134)).toString();
            } else {
                r29 = (new StringBuilder()).append(r26);
                r31 = new Object[3];
                r31[0] = sockWriteBuf.position();
                r31[1] = sockWriteBuf.limit();
                r31[2] = sockWriteBuf.capacity();
                r40 = r29.append(String.format("sockWriteBuf pos=%d(=bytes not sent) limit=%d cap=%d\n", r31)).toString();
                r42 = (new StringBuilder()).append(r40);
                r44 = new Object[4];
                r44[0] = ackNo;
                r44[1] = ackNo;
                r44[2] = this.calcAckToSend();
                r44[3] = this.calcAckToSend();
                r51 = r42.append(String.format("ack: base=0x%08X(%d)(=sockWriteBuf[0]), cur=0x%08X(%d)(base+writeBufPos)\n", r44)).toString();
            }

            if (sockReadBuf == null) {
                r143 = r51 + "sockReadBuf: null\n";
                r145 = new StringBuilder().append(r143);
                r147 = new Object[2];
                r147[0] = seqNo;
                r147[1] = seqNo;
                r99 = r145.append(String.format("seq: base=0x%08X(%d)(=sockReadBuf[0])\n", r147)).toString();
            } else {
                r75 = (new StringBuilder()).append(r51);
                r77 = new Object[3];
                r77[0] = sockReadBuf.position();
                r77[1] = sockReadBuf.limit();
                r77[2] = sockReadBuf.capacity();
                r86 = r75.append(String.format("sockReadBuf pos=%d(=bytes not ACKed by app) limit=%d cap=%d\n", r77)).toString();
                r88 = (new StringBuilder()).append(r86);
                r90 = new Object[4];
                r90[0] = seqNo;
                r90[1] = seqNo;
                r90[2] = seqNo + sockReadBuf.position();
                r90[3] = seqNo + sockReadBuf.position();
                r99 = r88.append(String.format("seq: base=0x%08X(%d)(=sockReadBuf[0]) cur=0x%08X(%d)(base+readBufPos)\n", r90)).toString();
            }

            r100 = r99 + "isFinReceived: " + isFinReceived + "\n";
            r107 = r100 + "isFinSent: " + isFinSent + "\n";
            r115 = r107 + "isFinAckReceived: " + isFinAckReceived + "\n";
            //r72 = r64;// + "isShutdownSent: " + isShutdownSent + "(=is SockClose or SockShutdown called)\n";

            r117 = new StringBuilder().append(r115);
            r119 = new Object[3];
            r119[0] = lastReceivedAck;
            r119[1] = lastReceivedAck;
            r119[2] = sameAckCount;
            return r117.append(String.format("lastReceivedAck: 0x%08X(%d), sameAckCount=%d\n", r119)).toString();
        }

        return "";
    }
}
