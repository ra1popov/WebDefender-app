package app.common.memdata;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;

import app.common.debug.L;
import app.internal.Settings;

public class ByteBufferPool {
    private static final int CAPACITY = 64;

    public static final int BUFFER_SIZE_SMALL = 16384;
    public static final int BUFFER_SIZE_BIG = 49152;

    private static final ArrayDeque<ByteBuffer> pool = new ArrayDeque<ByteBuffer>(CAPACITY);
    private static final ArrayDeque<ByteBuffer> pool2 = new ArrayDeque<ByteBuffer>(CAPACITY);

    ByteBufferPool() {
        super();
    }

    public static ByteBuffer alloc(int capacity) {
        try {
            ByteBuffer buf;
            if (capacity < BUFFER_SIZE_BIG) {
                synchronized (pool2) {
                    buf = pool2.pollFirst();
                }
            } else {
                synchronized (pool) {
                    buf = pool.pollFirst();
                }
            }

            if (buf == null)
                buf = ByteBuffer.allocate(capacity);

            return buf;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            L.e(Settings.TAG_BYTEBUFFERPOOL, ": OutOfMemoryError in ByteBufPool");

            System.gc();
            Thread.yield();

            return null;
        }
    }

    public static void release(ByteBuffer buf) {
        buf.clear();

        if (buf.capacity() < BUFFER_SIZE_BIG) {
            synchronized (pool2) {
                int size = pool2.size();
                if (size < CAPACITY)
                    pool2.addLast(buf);
            }
        } else {
            synchronized (pool) {
                int size = pool.size();
                if (size < CAPACITY)
                    pool.addLast(buf);
            }
        }
    }

    public static void clear() {
        synchronized (pool) {
            pool.clear();
        }

        synchronized (pool2) {
            pool2.clear();
        }
    }

}
