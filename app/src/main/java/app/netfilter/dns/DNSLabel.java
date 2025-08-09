package app.netfilter.dns;

import java.net.IDN;
import java.util.HashSet;

public class DNSLabel {

    public static String parse(DNSBuffer buf, byte data[]) {
        int c = buf.readByte();
        if ((c & 0xc0) == 0xc0) {
            c = ((c & 0x3f) << 8) + buf.readByte();
            HashSet<Integer> jumps = new HashSet<Integer>();
            jumps.add(c);
            String s = parse(data, c, jumps);

            return s;
        }

        if (c == 0)
            return "";

        byte b[] = new byte[c];
        buf.readFully(b);
        String s = IDN.toUnicode(new String(b, 0, 0, b.length));
        String t = parse(buf, data);
        if (t.length() > 0)
            s = s + "." + t;

        return s;
    }

    public static String parse(byte data[], int offset, HashSet<Integer> jumps) {
        int c = data[offset] & 0xff;
        if ((c & 0xc0) == 0xc0) {
            c = ((c & 0x3f) << 8) + (data[offset + 1] & 0xff);
            if (jumps.contains(c))
                throw new IllegalStateException("Cyclic offsets detected.");
            jumps.add(c);
            String s = parse(data, c, jumps);

            return s;
        }

        if (c == 0)
            return "";

        String s = new String(data, 0, offset + 1, c);
        String t = parse(data, offset + 1 + c, jumps);
        if (t.length() > 0)
            s = s + "." + t;

        return s;
    }
}
