package app.netfilter.http;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import app.common.LibNative;
import app.common.Utils;
import app.common.debug.L;
import app.internal.Settings;
import app.netfilter.IFilterVpnPolicy;
import app.security.PolicyRules;

public class RequestHeader {

    public String host = null;
    public String url = null;
    public String cookie = null;
    public String userAgent = null;
    public String referer = null;
    public String xForwardedFor = null;
    private String fullUrl = null;
    public boolean isRangeRequest = false;
    public boolean parsedAsHttp = false;
    private ArrayList<String> headers = null;
    public int loadedLength = 0;
    public ResponseHeader response = null;
    // Used in POST requests
    private byte[] requestData = null;
    private boolean modified = false;
    private boolean partial = false;
    private boolean postRequest = false;
    public PolicyRules policyRules = null;

    private RequestHeader() {
        // private constructor, use factory method parse()
    }

    // return RequestHeader
    // check parsedAsHttp == true
    // if parsedAsHttp check partial == false
    public static RequestHeader parse(byte[] data, int size, byte[] ip, boolean isBrowser, int uid, boolean proxyUsed,
                                      IFilterVpnPolicy policy) {
        RequestHeader res = new RequestHeader();
        res.split(data, size); // set partial if \r\n\r\n missed

        parseHeaders(ip, isBrowser, uid, proxyUsed, policy, res); // set res.parsedAsHttp if found HTTP request
        if (!res.parsedAsHttp)
            res.partial = false;

        return res;
    }

    // return null or RequestHeader with parsedAsHttp == true if HTTP request
    // check partial == false
    public static RequestHeader parse(ByteBuffer buf, byte[] ip, boolean isBrowser, int uid, boolean proxyUsed,
                                      IFilterVpnPolicy policy) {
        int pos = buf.position();

        //long t = System.currentTimeMillis();
        ArrayList<String> lines = new ArrayList<String>();
        //lines.ensureCapacity(15);
        boolean full = readHttpLines(buf, lines);
        //L.d(Settings.TAG_REQUESTHEADER, "readLines time: ", Long.toString(System.currentTimeMillis()-t));

        if (lines.size() > 0) {

            RequestHeader res = new RequestHeader();
            res.headers = lines;

            // TODO XXX if read only few first bytes and can't check for HTTP ?!
            if (!full && Utils.isHttpRequestLine(lines.get(0))) {
                res.parsedAsHttp = true;
                res.partial = true;
                return res;
            } else {
                parseHeaders(ip, isBrowser, uid, proxyUsed, policy, res);
                if (res.parsedAsHttp) {
                    return res;
                }
            }
        }

        buf.position(pos);
        return null;
    }

    private static void parseHeaders(byte[] ip, boolean isBrowser, int uid, boolean proxyUsed,
                                     IFilterVpnPolicy policy, RequestHeader request) {
        boolean httpRequest = false;
        int refererIndex = -1;

        if (request.headers == null)
            return;

        int size = request.headers.size();
        for (int i = 0; i < size; i++) {
            String line = request.headers.get(i);
            final int len = line.length();

            if (i == 0 && Utils.isHttpRequestLine(line)) {
                final int firstPos = line.indexOf(' ');
                if (firstPos > 0) {
                    final String method = line.substring(0, firstPos);
                    request.postRequest = (method.equals("POST") || method.equals("PUT"));

                    final int lastPos = line.indexOf(' ', firstPos + 1);
                    if (lastPos > firstPos)
                        request.url = line.substring(firstPos, lastPos).trim();
                } else {
                    //L.e("RequestHeader", "Get line is: " + line);
                    request.url = line.trim();
                }
                httpRequest = true;

                if (LibNative.asciiStartsWith("POST ", line) || LibNative.asciiStartsWith("PUT ", line))
                    request.postRequest = true;
            } else if (LibNative.asciiStartsWith("Host:", line) && len > 5) {
                request.host = line.substring(5).trim();
                //httpRequest = true;
            } else if (LibNative.asciiStartsWith("Cookie:", line) && len > 7) {
                request.cookie = line.substring(7).trim();
            } else if (LibNative.asciiStartsWith("User-Agent:", line) && len > 11) {
                request.userAgent = line.substring(11).trim();
                //L.d(Settings.TAG_REQUESTHEADER, "User-Agent: " + request.userAgent);

                if (policy != null && policy.changeUserAgent() && isBrowser &&
                        LibNative.asciiIndexOf("ndroid", request.userAgent) >= 0) {
                    String ua = policy.getUserAgent();
                    if (ua != null) {
                        request.modified = true;
                        request.headers.set(i, "User-Agent: " + ua);
                        //L.d(Settings.TAG_REQUESTHEADER, "Changing to: " + policy.getUserAgent());
                    }
                }
            } else if (LibNative.asciiStartsWith("Referer:", line) && len > 8) {
                request.referer = line.substring(8).trim();
                refererIndex = i;
            } else if (LibNative.asciiStartsWith("X-Forwarded-For:", line) && len > 16) {
                request.xForwardedFor = line.substring(16).trim();
            } else if (LibNative.asciiStartsWith("Content-Range:", line)) {
                request.isRangeRequest = true;
            } else if (LibNative.asciiStartsWith("X-Requested-With", line)) {
                //L.d(Settings.TAG_REQUESTHEADER, line);
                if (policy != null && policy.changeUserAgent() && isBrowser &&
                        !LibNative.asciiEndsWith("XMLHttpRequest", line)) {
                    request.headers.set(i, null);
                }
            }
        } // for

        if (httpRequest) {
            request.parsedAsHttp = true;

            if (request.host == null) // ipToString
                request.host = (ip[0] & 0xff) + "." + (ip[1] & 0xff) + "." + (ip[2] & 0xff) + "." + (ip[3] & 0xff);

            if (request.url == null)
                request.url = "/";

            if (policy != null && isBrowser) {
                // TODO XXX ???

                if (policy.changeReferer() && refererIndex >= 0 && !request.isSameDomain()) {
                    request.modified = true;
                    request.headers.set(refererIndex, null);
                }

                //policy.addRequestHeaders(request);
            }

            if (policy != null && proxyUsed /*policy.isProxyUsed(isBrowser, uid)*/)
                request.makeUrlForProxy();

            //L.d(Settings.TAG_REQUESTHEADER, "Headers:\n" + res.getHeaders());
            //L.d(Settings.TAG_REQUESTHEADER, "Requesting: "+res.getUrl());
        }
    }

    // fill strings list with header lines (return true if was \r\n\r\n at the end)
    public static boolean readHttpLines(ByteBuffer buf, ArrayList<String> strings) {

        if (!buf.hasRemaining()) {
            return false;
        }

        while (buf.hasRemaining()) {

            String s = readHttpLine(buf);

            if (Settings.DEBUG_HTTP) {
                L.a(Settings.TAG_REQUESTHEADER, s);
            }

            if (s == null) {
                break;
            } else if (s.length() == 0 && strings.size() == 0) {
                break;
            } else if (s.equals("\r\n")) {
                return true;
            }

            strings.add(s);

            if (!LibNative.asciiEndsWith("\r\n", s)) {
                break;
            }

        }

        return false;

    }

    private static String readHttpLine(ByteBuffer buf) {
        int bufPos = buf.position();
        int bufLimit = buf.limit();
        byte[] bufArr = buf.array();

        int count = bufPos;
        boolean nl = false;

        while (count < bufLimit) {
            char ch = (char) (bufArr[count++] & 0xFF);

            if (ch == '\r') {
                nl = true;
            } else if (nl) {
                break;
            }
        }

        buf.position(count);
        count -= bufPos;

        if (count > 0) {
            return (new String(bufArr, 0, bufPos, count));
        } else {
            return null;
        }

    }

    public boolean isPostRequest() {
        return postRequest;
    }

    public void addHeader(String header) {
        headers.add(header);
    }

    public void makeUrlForProxy() {
        String header = headers.get(0);
        String res;

        int pos = header.indexOf(' '); // TODO XXX may start with space? may be check length?
        if (pos > 0) {
            res = header.substring(0, pos + 1) + getUrl() + " ";
            if (LibNative.asciiIndexOf("HTTP/1.1", header) >= 0)
                res += "HTTP/1.1";
            else if (LibNative.asciiIndexOf("HTTP/1.0", header) >= 0)
                res += "HTTP/1.0";
            // TODO XXX and if not 1.1 or 1.0?

            headers.set(0, res);
            L.d(Settings.TAG_REQUESTHEADER, "Request = ", res);
            modified = true;
        }
    }

    public String getHeaders() {
        StringBuilder sb = new StringBuilder();
        if (headers != null && headers.size() > 0) {
            for (String line : headers) {
                if (line != null) {
                    if (LibNative.asciiEndsWith("\r\n", line))
                        sb.append(line);
                    else
                        sb.append(line).append("\r\n");
                }
            }
            sb.append("\r\n");
        }

        return sb.toString();
    }

    private void split(byte[] data, int size) {
        int off = -1;
        for (int i = 0; i < size - 3; i++) {
            if (data[i] == '\r' && data[i + 1] == '\n' && data[i + 2] == '\r' && data[i + 3] == '\n') {
                off = i;
                break;
            }
        }

        if (off < 0) {
            off = size;
            partial = true;
        }

        if (off > 0) {
            String s = new String(data, 0, 0, off);
            String[] lines = s.split("\r\n");
            headers = new ArrayList<String>();
            Collections.addAll(headers, lines);

            if (size > off + 4)
                requestData = Arrays.copyOfRange(data, off + 4, size);
        }
    }

    public int getRequestWithData(byte[] buf) {
        byte[] hh = getHeaders().getBytes(); // TODO optimize this, do write to the supplied buffer
        if (requestData != null) {
            System.arraycopy(hh, 0, buf, 0, hh.length);
            System.arraycopy(requestData, 0, buf, hh.length, requestData.length);

            return hh.length + requestData.length;
        } else {
            System.arraycopy(hh, 0, buf, 0, hh.length);
            return hh.length;
        }
    }

    public boolean isModified() {
        return modified;
    }

    // check if url main domain and referer main domain are same
    public boolean isSameDomain() {
        if (referer == null)
            return true;

        String ref_domain = Utils.getMainDomain(Utils.getDomain(referer));
        return isSameDomain(ref_domain);
    }

    // check if url main domain and domain are same
    public boolean isSameDomain(String domain) {
        String url_domain = Utils.getMainDomain(Utils.getDomain(getUrl()));
        return url_domain.equalsIgnoreCase(domain); // TODO XXX move in native
    }

    @Override
    public String toString() {
        return getUrl();
    }

    public String getFilename() {
        String url = getUrl();

        if (url != null) {
            int pos = url.indexOf('?');
            if (pos > 0) // TODO XXX may be >= ?
                url = url.substring(0, pos);

            if (url.indexOf('/') > 0)
                url = url.substring(url.lastIndexOf('/'));

            return url;
        }

        return null;
    }

    /**
     * Returns full http url from host and requested resource
     *
     * @return full url
     */
    public String getUrl() {
        if (fullUrl != null)
            return fullUrl;

        if (url != null && LibNative.asciiStartsWith("http://", url)) {
            fullUrl = url;
            return url;
        }

        if (host != null && url != null) {
            fullUrl = "http://" + host + url;
            return fullUrl;
        }

        return null;
    }

    public boolean isPartial() {
        return partial;
    }

    public boolean isHttp() {
        return parsedAsHttp;
    }
}
