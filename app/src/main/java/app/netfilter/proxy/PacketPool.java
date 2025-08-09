package app.netfilter.proxy;

import java.util.ArrayDeque;

import app.common.debug.L;
import app.internal.Settings;

// this pool used to manage memory for TCP/UDP packets that we writes to tun

public class PacketPool {

    private static final int POOL1_CAPACITY = 400; // TODO XXX test and calibrate this numbers
    public static final int POOL1_PACKET_SIZE = 512;
    private static final int POOL1_MIN_CAPACITY = 20;

    private static final int POOL2_CAPACITY = 200;
    public static final int POOL2_PACKET_SIZE = 1536;
    private static final int POOL2_MIN_CAPACITY = 20;

    private static final int POOL3_CAPACITY = 100;
    public static final int POOL3_PACKET_SIZE = 4096; // max mtu now
    private static final int POOL3_MIN_CAPACITY = 20;

    private static final int POOL4_CAPACITY = 10;
    public static final int POOL4_PACKET_SIZE = 31744;
    //public static final int  POOL4_PACKET_SIZE  = 32768;
    //public static final int  POOL4_PACKET_SIZE  = 40960; // with this tun write throw ENOBUFS exception (frameLen == 32768). 31kb max?
    private static final int POOL4_MIN_CAPACITY = 3;

    private static final ArrayDeque<Packet> pool1 = new ArrayDeque<Packet>(POOL1_CAPACITY);
    private static final ArrayDeque<Packet> pool2 = new ArrayDeque<Packet>(POOL2_CAPACITY);
    private static final ArrayDeque<Packet> pool3 = new ArrayDeque<Packet>(POOL3_CAPACITY);
    private static final ArrayDeque<Packet> pool4 = new ArrayDeque<Packet>(POOL4_CAPACITY);

    public static Packet alloc(int size) {
        if (size <= POOL1_PACKET_SIZE)
            return alloc(pool1, POOL1_PACKET_SIZE);

        if (size <= POOL2_PACKET_SIZE)
            return alloc(pool2, POOL2_PACKET_SIZE);

        if (size <= POOL3_PACKET_SIZE)
            return alloc(pool3, POOL3_PACKET_SIZE);

        //L.a(Settings.TAG_PACKETPOOL, "alloc big " + pool4.size());
        return alloc(pool4, POOL4_PACKET_SIZE); // TODO XXX if > POOL4_PACKET_SIZE ???
    }

    private static Packet alloc(final ArrayDeque<Packet> pool, int packetSize) {
        synchronized (pool) {
            Packet packet = pool.pollFirst();
            if (packet == null) {
                try {
                    packet = new Packet(packetSize);
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                    L.e(Settings.TAG_PACKETPOOL, ": OutOfMemoryError in PacketPool");

                    if (Settings.EVENTS_LOG) {

                    }
                    System.gc();
                    Thread.yield();

                    packet = new Packet(packetSize);
                }
            }
            return packet;
        }
    }

    public static void release(Packet packet) {
        switch (packet.frame.length) {
            case POOL1_PACKET_SIZE:
                release(pool1, packet, POOL1_CAPACITY);
                break;

            case POOL2_PACKET_SIZE:
                release(pool2, packet, POOL2_CAPACITY);
                break;

            case POOL3_PACKET_SIZE:
                release(pool3, packet, POOL3_CAPACITY);
                break;

            case POOL4_PACKET_SIZE:
                //L.a(Settings.TAG_PACKETPOOL, "release big " + pool4.size());
                release(pool4, packet, POOL4_CAPACITY);
                break;
        }
    }

    private static void release(final ArrayDeque<Packet> pool, Packet packet, final int poolCapacity) {
        synchronized (pool) {
            if (pool.size() < poolCapacity) {
                packet.clear();
                pool.addLast(packet);
                //L.d(Settings.TAG_PACKETPOOL, "PoolSize = " + pool.size());
            } else {
                if (Settings.DEBUG)
                    L.e(Settings.TAG_PACKETPOOL, "Deleting packet with size of ", Integer.toString(packet.frame.length), " bytes");

                packet = null;
            }
        }
    }

    public static void compact() {
        //L.e(Settings.TAG_PACKETPOOL, "Pool sizes: " + pool1.size() + " " + pool2.size() + " " + pool3.size() + " " + pool4.size());

        synchronized (pool1) {
            int size = pool1.size();
            while (size-- > POOL1_MIN_CAPACITY)
                pool1.removeLast();
        }

        synchronized (pool2) {
            int size = pool2.size();
            while (size-- > POOL2_MIN_CAPACITY)
                pool2.removeLast();
        }

        synchronized (pool3) {
            int size = pool3.size();
            while (size-- > POOL3_MIN_CAPACITY)
                pool3.removeLast();
        }

        synchronized (pool4) {
            int size = pool4.size();
            while (size-- > POOL4_MIN_CAPACITY)
                pool4.removeLast();
        }
    }
}
