package app.netfilter.http;

import app.common.LibNative;
import app.common.debug.L;
import app.internal.Settings;

public class ChunkedReader {

    private int lastChunkSize = -1;
    private int lastUnreadSize = 0;
    private int fileEndOffset = -1;
    private String lastLineStart = null;

    public ChunkedReader() {
    }

    // The reader can be reused many times, for many files through this connection
    public void clear() {
        lastChunkSize = -1;
        lastUnreadSize = 0;
        fileEndOffset = -1;
        lastLineStart = null;
    }

    public boolean read(byte[] buf, int offset, int size) {
        if (Settings.DEBUG) L.e(Settings.TAG_CHUNKEDREADER, "offset = " + offset + " size = " + size);
        final int start = offset;

        if (Settings.DEBUG) L.d(Settings.TAG_CHUNKEDREADER, "lastUnreadSize 1 = " + lastUnreadSize);

        if (lastUnreadSize > 0) {
            if (lastUnreadSize <= size) {
                offset += lastUnreadSize;
                lastUnreadSize = 0;
            } else {
                lastUnreadSize -= size;
                if (Settings.DEBUG) L.d(Settings.TAG_CHUNKEDREADER, "lastUnreadSize 2 = " + lastUnreadSize);

                return true;
            }
        }

        if (Settings.DEBUG) L.d(Settings.TAG_CHUNKEDREADER, "lastUnreadSize 3 = " + lastUnreadSize);

        while (true) {
            String s = readChunkSize(buf, offset, size - (offset - start), lastLineStart);

            // If the string ends with \r\n
            if (LibNative.asciiEndsWith("\r\n", s)) {
                if (Settings.DEBUG) L.d(Settings.TAG_CHUNKEDREADER, "line: ", s);

                // We shift by the length of the data read, and if part was read earlier, we subtract the length of the previous portion.
                offset += (s.length() - (lastLineStart != null ? lastLineStart.length() : 0));
                lastLineStart = null;

                if (s.length() > 2) {
                    int chunkSize;
                    try {
                        chunkSize = Integer.parseInt(s.trim().split(";")[0], 16);
                    } catch (NumberFormatException e) {
                        if (Settings.EVENTS_LOG) {

                        }
                        if (Settings.DEBUG) L.d(Settings.TAG_CHUNKEDREADER, e.toString());

                        return false;
                    }

                    if (Settings.DEBUG)
                        L.w(Settings.TAG_CHUNKEDREADER, "chunkSize = " + chunkSize + " newOffset = " + (offset + chunkSize + 2) +
                                " max = " + (start + size));

                    if (chunkSize == 0) {
                        offset += chunkSize + 2;
                        lastChunkSize = chunkSize;
                        lastUnreadSize = 0;
                        fileEndOffset = offset;
                        break;
                    } else if (offset + chunkSize + 2 <= start + size) {
                        String chunk = new String(buf, 0, offset, chunkSize + 2);
                        //L.d(Settings.TAG_CHUNKEDREADER, "chunk: ", chunk);
                        offset += chunkSize + 2;
                        lastChunkSize = chunkSize;
                        lastUnreadSize = 0;
                    } else {
                        lastUnreadSize = chunkSize + 2 - (start + size - offset);
                        if (Settings.DEBUG) L.e(Settings.TAG_CHUNKEDREADER, "not full chunk, must read " + lastUnreadSize + " bytes more");
                        break;
                    }
                } else {
                    fileEndOffset = offset;
                    break;
                }
            } else {
                // We’ve read a chunk of the string; the ending will arrive in the next packet.
                lastLineStart = s;
                if (Settings.DEBUG) L.d(Settings.TAG_CHUNKEDREADER, "not full line: " + s);
                break;
            }
        }

        return true;
    }

    private static String readChunkSize(byte[] buf, int offset, int size, String lastLineStart) {
        int pos = offset;
        int rnCount = 0;

        StringBuffer sb = new StringBuffer();
        if (lastLineStart != null) {
            sb.append(lastLineStart);
            if (lastLineStart.indexOf('\r') >= 0)
                rnCount++;
        }

        while (pos < offset + size) {
            char ch = (char) (buf[pos++] & 0xFF);

            sb.append(ch);

            if (ch == '\r' || ch == '\n') {
                rnCount++;
                if (rnCount >= 2)
                    break;
            }
        }

        return sb.toString();
    }

    public boolean isAtEndOfFile() {
        return lastChunkSize == 0;
    }

    public int getLastChunkSize() {
        return lastChunkSize;
    }

    public int getFileEndOffset() {
        return fileEndOffset;
    }
}
