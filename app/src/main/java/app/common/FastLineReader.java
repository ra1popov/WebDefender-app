package app.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class FastLineReader {

    private static byte[] commonBuf = null;
    private byte[] buf = null;
    private int bytes = 0;
    private int readPos = 0;

    private static final Object lock = new Object();

    public FastLineReader(InputStream stream) throws IOException {
        buf = null;
        bytes = 0;
        readPos = 0;

        init(stream);
    }

    public FastLineReader(String fileName) {
        buf = null;
        bytes = 0;
        readPos = 0;
        FileInputStream stream = null;

        try {
            stream = new FileInputStream(fileName);
            init(stream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public FastLineReader(byte[] data, int offset, int size) {
        synchronized (lock) {
            commonBuf = null;
            buf = data;
            bytes = offset + size;
            readPos = offset;
        }
    }

    public FastLineReader(byte[] data, int offset) {
        synchronized (lock) {
            commonBuf = null;
            buf = data;
            bytes = buf.length;
            readPos = offset;
        }
    }

    public static void setCommonBufSize(int size) {
        byte[] buf = new byte[size];
        commonBuf = buf;
    }

    public void close() {
        synchronized (lock) {
            if (commonBuf == null)
                commonBuf = buf;
        }
    }

    public void init(InputStream stream) throws IOException {
        synchronized (lock) {
            if (commonBuf == null) {
                buf = new byte[262144];
            } else {
                buf = commonBuf;
                commonBuf = null;
            }
        }

        while (true) {
            int i = stream.read(buf, bytes, buf.length - bytes);
            if (i == -1)
                break;

            bytes = (i + bytes);
            if (bytes >= buf.length)
                buf = Arrays.copyOf(buf, 2 * buf.length);
        }
    }

    public String readLine() {
        String res = null;

        int start = readPos;
        if (readPos < bytes) {
            int length = 0;
            while (readPos < bytes) {
                byte b = buf[readPos++];

                if (b == 13 || b == 10) {
                    if ((readPos < bytes) && (buf[readPos] == 10))
                        readPos = (readPos + 1);

                    break;
                }

                length += 1;
            }
            res = new String(buf, 0, start, length);
        }

        return res;
    }

    public int getReadPos() {
        return readPos;
    }
}
