package app.common;

import java.util.Arrays;

public final class ByteArrayWrapper {
    private final byte[] data;

    public ByteArrayWrapper(byte[] data) {
        super();

        if (data != null) {
            this.data = data;
        } else {
            throw new NullPointerException();
        }
    }

    public boolean equals(Object arrayWrapper) {
        if (arrayWrapper instanceof ByteArrayWrapper) {
            return Arrays.equals(data, ((ByteArrayWrapper) arrayWrapper).data);
        }

        return false;
    }

    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
