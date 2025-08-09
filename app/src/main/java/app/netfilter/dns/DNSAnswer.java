package app.netfilter.dns;

import app.common.Utils;

public class DNSAnswer {

    public byte[] ip = null;
    public String domain = null;
    public String cname = null;
    public int type = 0;
    public int clazz = 0;
    public int ttl = 0;
    public byte[] data = null;

    public DNSAnswer(String domain, byte[] ip, int type, int clazz, int ttl) {
        this.ip = ip;
        this.domain = domain;
        this.ttl = ttl;
        this.type = type;
        this.clazz = clazz;
    }

    public DNSAnswer(String domain, String cname, int type, int clazz, int ttl) {
        this.cname = cname;
        this.domain = domain;
        this.ttl = ttl;
        this.type = type;
        this.clazz = clazz;
    }

    public DNSAnswer(String domain, int type, int clazz, int ttl) {
        this.domain = domain;
        this.ttl = ttl;
        this.type = type;
        this.clazz = clazz;
    }

    public boolean equalsIgnoreIp(DNSAnswer answer) {
        if (answer.type != type) return false;
        if (answer.ip != null && ip == null) return false;
        if (answer.ip == null && ip != null) return false;
        if (cname == null && answer.cname != null) return false;
        if (answer.cname == null && cname != null) return false;
        if (cname != null && !cname.equals(answer.cname)) return false;
        if (domain != null && answer.domain == null) return false;
        if (domain == null && answer.domain != null) return false;
        if (domain != null && answer.domain != null && !domain.equals(answer.domain)) return false;

        return true;
    }

    @Override
    public String toString() {
        if (ip != null)
            return "DNS {" + domain + ", " + Utils.ipToString(ip, 0) + ", " + ttl + "}";
        else
            return "DNS {" + domain + ", " + cname + ", " + ttl + "}";
    }
}
