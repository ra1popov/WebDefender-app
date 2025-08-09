package app.common.memdata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MemoryBuffer {
    public static final int MIN_MEMORY_CAPACITY = 262144;
    public static final int MAX_MEMORY_CAPACITY = 1048576 * 10;

    private ByteBuffer buffer = null;
    private File file = null;
    private FileInputStream fis = null;
    private FileOutputStream fos = null;
    private boolean needToFlip = false;
    private int size = 0;

    public MemoryBuffer(int capacity) {
        if (capacity <= 0)
            capacity = MIN_MEMORY_CAPACITY;

        if (capacity <= MAX_MEMORY_CAPACITY)
            buffer = ByteBuffer.allocate(capacity);
        else
            createFile();

        //L.d(Settings.TAG_MEMORYBUFFER, "Created buffer with capacity of " + capacity + " bytes, remaining = " + buffer.remaining());
    }

    private void createFile() {
        try {
            file = File.createTempFile("mem", null);
            fis = new FileInputStream(file);
            fos = new FileOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int size() {
        return size;
    }

    public int capacity() {
        if (buffer != null)
            return buffer.capacity();

        try {
            if (fis != null)
                return fis.available();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public boolean inMemory() {
        return buffer != null;
    }

    public void rewind() {
        if (buffer != null) {
            buffer.flip();
            needToFlip = false;
        }

        if (fis != null) {
            try {
                fis.reset();
            } catch (IOException e) {
                //L.d(Settings.TAG_MEMORYBUFFER, "Size = " + size + " buffer = " + buffer);
                e.printStackTrace();
            }
        }
    }

    public void free() {
        if (buffer != null) {
            buffer.clear();
            buffer.limit(buffer.capacity());
            //buffer = null;
        }

        try {
            if (fis != null) {
                fis.close();
                fis = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (fos != null) {
                fos.close();
                fos = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file != null) {
            file.delete();
            file = null;
        }

        size = 0;
    }

    private void swapToDisk() {
        createFile();
        try {
            fos.write(buffer.array());
            buffer = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int write(byte[] data, int offset, int count) {
        //L.d(Settings.TAG_MEMORYBUFFER, "Writing " + count + " bytes, size = " + size + " buffer.capacity() = " + buffer.capacity());

        if (buffer != null && size + count > buffer.capacity()) {
            int cap = (buffer.capacity() << 1);
            while (cap < count)
                cap = (cap << 1);

            if (cap <= MAX_MEMORY_CAPACITY) {
                ByteBuffer buf = ByteBuffer.allocate(cap);
                buffer.flip();
                buf.put(buffer);
                buffer = buf;
            } else {
                //L.d(Settings.TAG_MEMORYBUFFER, "Cap = " + cap + " (count = " + count + ", buffer.capacity = " + buffer.capacity() + ") swapping to disk!");
                swapToDisk();
            }
        }

        if (buffer != null) {
            buffer.put(data, offset, count);
            needToFlip = true;
            size += count;
        } else {
            try {
                fos.write(data, offset, count);
                size += count;
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        }

        return count;
    }

    public int write(byte[] data) {
        return write(data, 0, data.length);
    }

    public int read(byte[] data, int offset, int count) {
        if (buffer != null) {
            if (needToFlip) {
                buffer.flip();
                needToFlip = false;
            }
            if (count > buffer.remaining())
                count = buffer.remaining();
            if (count > 0)
                buffer.get(data, offset, count);
        } else {
            try {
                count = fis.read(data, offset, count);
            } catch (IOException e) {
                e.printStackTrace();
                count = -1;
            }
        }

        return count;
    }

    public int read(byte[] data) {
        return read(data, 0, data.length);
    }

    public byte[] readAll() {
        byte[] buf = new byte[size];

        read(buf);

        return buf;
    }

}
