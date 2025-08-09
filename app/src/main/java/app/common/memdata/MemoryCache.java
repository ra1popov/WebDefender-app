package app.common.memdata;

import java.util.ArrayDeque;

public class MemoryCache {
    public static final int CAPACITY = 32;

    private static ArrayDeque<MemoryBuffer> buffers = new ArrayDeque<MemoryBuffer>(CAPACITY);

    public static MemoryBuffer alloc(int capacity) {
        synchronized (buffers) {
            MemoryBuffer buf = null;
            int count = buffers.size();
            for (int i = 0; i < count; i++) {
                MemoryBuffer testBuf = buffers.pollFirst();
                if (testBuf.capacity() >= capacity) {
                    buf = testBuf;
                    break;
                }
                buffers.addLast(testBuf);
            }

            if (buf != null)
                return buf;

            return new MemoryBuffer(capacity);
        }
    }

    public static void release(MemoryBuffer buffer) {
        buffer.free();
        synchronized (buffers) {
            if (buffer.inMemory()) {
                if (buffers.size() < CAPACITY)
                    buffers.addLast(buffer);
            }
        }
    }

    private static boolean releaseSmallestBuffer() {
        synchronized (buffers) {
            MemoryBuffer buf = null;
            int count = buffers.size();
            int smallestSize = Integer.MAX_VALUE;
            int smallestIndex = -1;
            for (int i = 0; i < count; i++) {
                MemoryBuffer testBuf = buffers.pollFirst();
                if (testBuf.capacity() < smallestSize) {
                    smallestSize = testBuf.capacity();
                    smallestIndex = i;
                    buf = testBuf;
                }
                buffers.addLast(testBuf);
            }

            if (smallestIndex >= 0) {
                buffers.removeFirstOccurrence(buf);
                return true;
            }
        }
        return false;
    }
}
