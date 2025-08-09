package app.netfilter.proxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import app.netfilter.dns.DNSAnswer;

public class DNSCache {

    private static final ArrayList<DNSAnswer> answers = new ArrayList<DNSAnswer>(50);
    private static final Set<byte[]> ips = new HashSet<byte[]>(50);

    public static DNSAnswer getForIp(byte[] ip) {
        synchronized (answers) {
            for (DNSAnswer answer : answers) {
                if (Arrays.equals(answer.ip, ip))
                    return answer;
            }
        }

        return null;
    }

    public static DNSAnswer getForDomain(String domain) {
        synchronized (answers) {
            for (DNSAnswer answer : answers) {
                if (answer.domain != null && answer.domain.equals(domain))
                    return answer;
            }
        }

        return null;
    }

    public static void addAnswer(DNSAnswer answer) {
        synchronized (answers) {
            if (!ips.contains(answer.ip)) {
                answers.add(answer);
                ips.add(answer.ip);
            }
        }
    }

    public static void clear() {
        synchronized (answers) {
            answers.clear();
        }
    }
}
