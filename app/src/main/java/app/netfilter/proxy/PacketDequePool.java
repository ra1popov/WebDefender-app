package app.netfilter.proxy;

import java.util.ArrayDeque;

import app.common.debug.L;
import app.internal.Settings;

public class PacketDequePool {
    private static final int POOL_CAPACITY = 100; // TODO XXX test and calibrate this numbers
    public static final int DEQUE_CAPACITY = 10;

    private static final ArrayDeque<ArrayDeque<Packet>> pool = new ArrayDeque<ArrayDeque<Packet>>(POOL_CAPACITY);

    public static ArrayDeque<Packet> alloc() {
        synchronized (pool) {
            ArrayDeque<Packet> deque = pool.pollFirst();
            if (deque == null)
                deque = new ArrayDeque<Packet>(DEQUE_CAPACITY);

            return deque;
        }
    }

    public static void release(ArrayDeque<Packet> deque) {
        synchronized (pool) {
            if (pool.size() < POOL_CAPACITY)
                pool.addLast(deque);
            else
                L.w(Settings.TAG_PACKETDEQUEPOOL, "Deleting deque!");
        }
    }
}
