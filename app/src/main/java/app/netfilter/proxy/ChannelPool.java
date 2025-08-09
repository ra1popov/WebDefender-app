package app.netfilter.proxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Iterator;

import app.common.debug.L;
import app.internal.Settings;

public class ChannelPool {

    private static final int CAPACITY = 0;//10;				  // max connects in pool

    private static final int TIMEOUT_ADD = 2 * 60 * 1000; // 2 min, timeout to keep connection in pool (proxy settings)
    private static final int TIMEOUT_PING = 5 * 1000;      // 5 sec, timeout to check connect with ping
    private static final int MAX_FAILED_PINGS = 0;          // see clear

    private static final byte[] pingData = new byte[]{1, 2, 3, 4};
    private static final ByteBuffer pingDataBuf = ByteBuffer.allocate(4);

    private static final ArrayDeque<ChannelData> pool = new ArrayDeque<>(CAPACITY);
    private static int count = 0;

    static {
        pingDataBuf.put(pingData);
    }

    // keep connects to our proxy
    // TODO XXX channels to proxy will be in pool until VPN stop, proxy change or call to alloc()

    public static ChannelData alloc() {
        synchronized (pool) {
            clear(true); // clean pool (remove with timeout or bad)

            ChannelData data = pool.pollFirst();
            if (data != null) {
                if (Settings.DEBUG)
                    L.d(Settings.TAG_CHANNELPOOL, "Get proxy connect", data.toString(), " " + data.channel.isConnected(),
                            " " + data.channel.isOpen());

                incCount();
            }

            return data;
        }
    }

    // put connection to our proxy in pull
    public static void release(SocketChannel channel, int fd,
                               int encryptKey1Pos, int encryptKey2Pos, int decryptKey1Pos, int decryptKey2Pos,
                               byte[] key1, byte[] key2, int key1Len, int key2Len) {
        boolean ok = false;

        synchronized (pool) {
            clear(true); // clean pool

            int size = pool.size();
            if (size < CAPACITY) {
                ChannelData data = new ChannelData(channel, fd, encryptKey1Pos, encryptKey2Pos, decryptKey1Pos, decryptKey2Pos,
                        key1, key2, key1Len, key2Len);

                if (Settings.DEBUG) L.d(Settings.TAG_CHANNELPOOL, "Saving proxy connect ", data.toString());

                pool.addLast(data);
                ok = true;
            }

            if (!ok) {
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                channel = null;
            }

            decCount();
        }
    }

    public static void clear(boolean onlyBad) {

        synchronized (pool) {
            int size = pool.size();
            if (size == 0)
                return;

            final long time = System.currentTimeMillis();
            final long timeoutAdd = time - TIMEOUT_ADD;
            final long timeoutPing = time - TIMEOUT_PING;

            ChannelData data;
            Iterator<ChannelData> it = pool.iterator();

            while (it.hasNext()) {
                data = it.next();

                if (!onlyBad || data.poolAddTime < timeoutAdd || isBad(data, time, timeoutPing)) {
                    // delete bad channel if have bad pings or delete all
                    if (Settings.DEBUG) if (onlyBad) L.d(Settings.TAG_CHANNELPOOL, "Proxy bad connect cleared");

                    try {
                        data.channel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    data.channel = null;

                    it.remove();
                }
            }
        }
    }

    // check if connect to server bad and need to close it
    private static boolean isBad(ChannelData data, final long time, final long timeoutPingAbs) {
        int res = 0;

        //if (timeoutPingAbs == 0 || data.lastPingTime < timeoutPingAbs)
        if (data.lastPingTime == 0 || data.lastPingTime <= timeoutPingAbs) {
            res = sendPing(data.channel, data.fd);
            data.lastPingTime = time;

            if (res == 0)
                data.failedPingCount++;
            else if (res > 0 && data.failedPingCount != 0)
                data.failedPingCount = 0;
        }

        if (!data.channel.isConnected())
            return true;
        else if (res == -1 || data.failedPingCount > MAX_FAILED_PINGS)
            return true;

        return false;
    }

    /*
     * send ping packet to our proxy (check if connect alive)
     * -1 - socket dead, 0 - ping error, 1 - all ok
     *
     * TODO XXX add fd == 0 support
     */
    private static int sendPing(SocketChannel channel, int fd) {
        boolean sendOk = false;
        try {
            pingDataBuf.flip();
            int written = channel.write(pingDataBuf);
            if (written == pingData.length)
                sendOk = true;
        } catch (IOException e) {
        }

        if (!sendOk)
            return -1;

        //L.a(Settings.TAG_CHANNELPOOL, "ping " + fd + " ?" + haveData + " ?" + sendOk);

        return 1;
    }

    // for debug
    public static int size() {
        synchronized (pool) {
            return pool.size();
        }
    }

    public static void incCount() {
        if (Settings.DEBUG) {
            synchronized (pool) {
                count++;
                L.e(Settings.TAG_CHANNELPOOL, "PROXY CONNECT COUNT 1: " + count + " ( " + pool.size() + " )");
            }
        }
    }

    public static void decCount() {
        if (Settings.DEBUG) {
            synchronized (pool) {
                count--;
                L.e(Settings.TAG_CHANNELPOOL, "PROXY CONNECT COUNT 2: " + count + " ( " + pool.size() + " )");
            }
        }
    }
}
