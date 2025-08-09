package app.netfilter.http;

import app.common.FastLineReader;
import app.common.LibNative;
import app.common.Utils;

public class ResponseHeader {

    public int responseCode = 0;
    public int contentLength = 0;
    public int headerLength = 0;
    public boolean chunked = false;
    public String cookie = null;
    public String contentType = null;
    public String contentEncoding = null;
    public String fileName = null;
    public boolean full = false;

    private ResponseHeader() {
        // private constructor, use factory method parse() instead
    }

    public static ResponseHeader parse(byte[] data, int offset, int size) {
        ResponseHeader res = new ResponseHeader();

        String line;
        FastLineReader reader = new FastLineReader(data, offset, size);

        while ((line = reader.readLine()) != null) {
            if (line.equals("")) {
                res.full = true;
                res.headerLength = reader.getReadPos() - offset;
                break;
            }
            //L.d(Settings.TAG_RESPONSEHEADER, line);

            if (LibNative.asciiStartsWith("HTTP", line)) {
                int codePos = line.indexOf(' ');
                if (codePos > 0) {
                    final int space = line.indexOf(' ', codePos + 1);
                    if (space > codePos) {
                        String code = line.substring(codePos, space).trim();
                        // TODO XXX may be replace isNumber with catch NumberFormatException?
                        if (code.length() == 3 && Utils.isNumber(code))
                            res.responseCode = Integer.parseInt(code);
                    }
                }
            } else if (res.responseCode > 0) {
                int len = line.length();

                if (LibNative.asciiStartsWith("Content-Type:", line) && len > 13) {
                    res.contentType = line.substring(13).trim();
                } else if (LibNative.asciiStartsWith("Set-Cookie:", line) && len > 11) {
                    res.cookie = line.substring(11).trim();
                } else if (LibNative.asciiStartsWith("Content-Length:", line) && len > 15) {
                    try {
                        res.contentLength = Integer.parseInt(line.substring(15).trim());
                    } catch (NumberFormatException e) { /* ata-ta! TODO XXX */ }
                } else if (LibNative.asciiStartsWith("Content-Encoding:", line) && len > 17) {
                    res.contentEncoding = line.substring(17).trim();
                } else if (LibNative.asciiStartsWith("Transfer-Encoding:", line) && len > 18) {
                    res.chunked = line.substring(18).trim().equals("chunked");
                } else if (LibNative.asciiStartsWith("Content-Disposition: attachment;", line)) {
                    int pos = LibNative.asciiIndexOf("filename=", line);
                    if (pos > 0 && len > pos + 9) {
                        res.fileName = line.substring(pos + 9);
                        res.fileName = res.fileName.replace("\"", "");
                        res.fileName = res.fileName.replace("\'", "");
                    }
                }
            }

            // if response code didn't get parsed then it is encoded somehow
            if (res.responseCode == 0)
                return null;
        }

        return res;
    }

    @Override
    public String toString() {
        String s = "Response code: " + responseCode;
        s += ", Content-Type: " + contentType;
        s += ", Content-Encoding: " + contentEncoding;
        s += ", Chunked: " + chunked;
        if (!chunked)
            s += ", Content-Length: " + contentLength;
        if (fileName != null)
            s += ", FileName: " + fileName;
        if (!full)
            s += "Partial headers";
        return s;
    }
}
