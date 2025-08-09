package app.netfilter.dns;

import app.common.debug.L;
import app.internal.Settings;

public class DNSBuffer {

    private byte[] data;
    private int offset;

    public DNSBuffer(byte[] data, int offset) {
        this.data = data;
        this.offset = offset;
    }

    public DNSBuffer(byte[] data) {
        this.data = data;
        this.offset = 0;
    }

    public int readByte() {
        int res = data[offset] & 0xff;
        offset++;
        return res;
    }

    public int readWord() {
        int res = ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff);
        offset += 2;
        return res;
    }

    public int readInt() {
        int res = ((0xFF & data[offset]) << 24) | ((0xFF & data[offset + 1]) << 16) | ((0xFF & data[offset + 2]) << 8) | (0xFF & data[offset + 3]);
        offset += 4;
        return res;
    }

    public String readString(int length) {
        String res = new String(data, 0, offset, length);
        offset += length;
        return res;
    }

    // TODO XXX profile StringBuilder.append and StringBuilder.toString
    public String readName() {
        StringBuilder sb = new StringBuilder(32);
        boolean done = false;

        int len;
        try {
            while (!done) {
                len = readByte();
                if (len == 0xC0) {
                    offset--;
                    int pointer = readWord() & 0x3FFF;
                    int saveOffset = offset;
                    offset = pointer;
                    if (sb.length() > 0)
                        sb.append('.').append(readName());
                    else
                        sb.append(readName());
                    offset = saveOffset;
                    done = true;
                } else {
                    if (len > 0) {
                        if (sb.length() > 0)
                            sb.append('.').append(readString(len));
                        else
                            sb.append(readString(len));
                    } else
                        done = true;
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }

        return sb.toString();
    }

    public String readName(int maxSize) {
        int off = offset;
        String res = readName();

        if (Settings.DEBUG)
            if (offset - off > maxSize) L.d(Settings.TAG_DNSBUFFER, "Name and size do not match!");

        return res;
    }

    public void readFully(byte[] buf) {
        System.arraycopy(data, offset, buf, 0, buf.length);
        offset += buf.length;
    }

    public void skip(int length) {
        offset += length;
    }

    public byte[] getData() {
        return data;
    }

    public int getOffset() {
        return offset;
    }
}
