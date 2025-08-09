package app.netfilter;

import java.nio.ByteBuffer;

import app.common.memdata.MemoryBuffer;
import app.internal.ProxyBase;
import app.netfilter.dns.DNSRequest;
import app.netfilter.dns.DNSResponse;
import app.netfilter.http.RequestHeader;
import app.netfilter.http.ResponseHeader;
import app.netfilter.proxy.Packet;
import app.netfilter.proxy.TCPStateMachine;
import app.security.PolicyRules;


public interface IFilterVpnPolicy {

    void reload();

    PolicyRules getPolicy(Packet packet);

    PolicyRules getPolicy(Packet packet, int uid);

    PolicyRules getPolicy(int uid);

    PolicyRules getPolicy(String domain);

    PolicyRules getPolicy(DNSResponse response);

    PolicyRules getPolicy(DNSRequest request);

    PolicyRules getPolicy(RequestHeader request, ResponseHeader response, byte[] data, int uid, boolean isBrowser, boolean isProxyUsed, String pkgName);

    PolicyRules getPolicy(RequestHeader request, int uid, String pkgname, boolean isBrowser, byte[] servIp);

    int getScanDataBufferSize(RequestHeader requestHeader, ResponseHeader responseHeader);

    boolean isBrowser(String[] packs);

    boolean isBrowser(int uid);

    void addRequestHeaders(RequestHeader header);

    boolean changeUserAgent();

    boolean needToAddHeaders();

    boolean changeReferer();

    void scan(MemoryBuffer buffer, TCPStateMachine tcpStateMachine);

    String getUserAgent();

    boolean isProxyUse(byte[] serverIp, int serverPort, int uid, boolean isBrowser);

    boolean isProxyCryptUse();

    ProxyBase.ProxyServer getProxyHost();

    int getProxyPort();

    PolicyRules getPolicyForData(RequestHeader request, ByteBuffer buf);

}
