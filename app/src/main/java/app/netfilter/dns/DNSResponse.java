package app.netfilter.dns;

import java.util.ArrayList;

import app.common.LibNative;
import app.common.debug.L;
import app.internal.Settings;
import app.netfilter.proxy.DNSCache;
import app.netfilter.proxy.Packet;
import app.netfilter.proxy.PacketPool;

public class DNSResponse {

    private static final boolean NORMALIZE_DATA = true; // convert to lowercase domain names

    public static final int NO_ERROR = 0x8180;
    public static final int ERROR_NO_SUCH_NAME = 0x8183;

    private final byte[] clientIp;
    private final int clientPort;
    private final byte[] serverIp;
    private final int serverPort;
    public boolean parsed = false;
    public boolean modified = false;
    public boolean readNameError = false;
    private int transactionID = 0; // 16-bit field identifying a specific DNS transaction
    private int flags = 0;
    private int questions = 0;
    private int answerNum = 0;
    private int authorities = 0;
    private int additional = 0;
    private boolean hasIpV6answers = false;
    private String[] domains = null;
    private int[] types = null;
    private int[] classes = null;
    private ArrayList<DNSAnswer> answers = null;
    private DNSBuffer buffer;

    public DNSResponse(int transactionId, byte[] serverIp, int serverPort, byte[] clientIp, int clientPort) {
        this.transactionID = transactionId;
        this.clientIp = clientIp;
        this.clientPort = clientPort;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public DNSResponse(int transactionId, String[] domains, byte[] serverIp, int serverPort, byte[] clientIp, int clientPort) {
        this.transactionID = transactionId;
        this.domains = domains;
        this.clientIp = clientIp;
        this.clientPort = clientPort;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public DNSResponse(Packet packet) {
        if (!packet.parsed()) {
            if (!packet.parseFrame()) {
                serverIp = null;
                serverPort = 0;
                clientIp = null;
                clientPort = 0;
                return;
            }
        }

        serverIp = packet.srcIp;
        serverPort = packet.srcPort;
        clientIp = packet.dstIp;
        clientPort = packet.dstPort;

        byte[] buf = packet.getData();
        if (buf != null && buf.length > 12) {
            buffer = new DNSBuffer(buf);
            transactionID = buffer.readWord();
            flags = buffer.readWord();
            questions = buffer.readWord();
            answerNum = buffer.readWord();
            authorities = buffer.readWord();
            additional = buffer.readWord();

            if ((flags & 0x0f) == 0) {
                try {
                    if (!parseQuestions())
                        readNameError = true;
                    else
                        parsed = parseAnswers();
                } catch (Throwable e) {

                    e.printStackTrace();

                    if (Settings.DEBUG)
                        L.f("dns.cap", packet.frame); // hmm, may be send to server?

                    readNameError = true;
                }
            } else {
                if (Settings.DEBUG) {
                    L.e(Settings.TAG_DNSRESPONSE, "transactionID = ", Integer.toHexString(transactionID));
                    L.e(Settings.TAG_DNSRESPONSE, "DNS Error: flags = ", Integer.toHexString(flags),
                            " questions = ", Integer.toString(questions),
                            " answers = ", Integer.toString(answerNum));
                }
            }
        }
    }

    public void addAnswer(DNSAnswer answer) {
        if (answers == null)
            answers = new ArrayList<DNSAnswer>();
        answers.add(answer);
        modified = true;
    }

    public void clearAnswers() {
        answers = new ArrayList<DNSAnswer>();
        modified = true;
    }

    // TODO XXX compare with DNSRequest.parseQuestions
    private boolean parseQuestions() {
        domains = new String[questions];
        types = new int[questions];
        classes = new int[questions];

        for (int i = 0; i < questions; i++) {
            domains[i] = buffer.readName();
            if (domains[i] == null)
                return false;

            if (NORMALIZE_DATA)
                domains[i] = LibNative.asciiToLower(domains[i]);

            types[i] = buffer.readWord();
            classes[i] = buffer.readWord();
        }

        return true;
    }

    private boolean parseAnswers() {
        answers = new ArrayList<DNSAnswer>(answerNum);
        for (int i = 0; i < answerNum; i++) {
            String domain = DNSLabel.parse(buffer, buffer.getData()); //buffer.readName();
            if (domain == null) // if we didn't parse the first answer than we have a problem
                return (i != 0);

            if (NORMALIZE_DATA)
                domain = LibNative.asciiToLower(domain);

            int type = buffer.readWord();
            int classType = buffer.readWord();

            // TTL
            int ttl = buffer.readInt();

            if ((type == 1 || type == 5) && ttl < 300) {
                ttl = 300;
                modified = true;
            }

            int dataLength = buffer.readWord();
            if (Settings.DEBUG_DNS)
                L.a(Settings.TAG_DNSRESPONSE, "domain = " + domain + " type = " + type + " dataLength = " + dataLength);

            byte[] ip = null;
            DNSAnswer answer = null;
            boolean skip = false;

            // TODO XXX if type == 1 and dataLength != 4 (type == 28 && dataLength != 16) ??? we add answer!

            if (type == 1 && dataLength == 4) {
                // ipv4 address

                ip = new byte[]{(byte) buffer.readByte(), (byte) buffer.readByte(),
                        (byte) buffer.readByte(), (byte) buffer.readByte()};
                answer = new DNSAnswer(domain, ip, type, classType, ttl);
            } else if (type == 5) {
                // alias

                String cname = buffer.readName(dataLength);
                if (NORMALIZE_DATA)
                    cname = LibNative.asciiToLower(cname);

                answer = new DNSAnswer(domain, cname, type, classType, ttl);
            } else if (type == 28 && dataLength == 16) {
                // ipv6 address

                hasIpV6answers = true;
                if (Settings.DNS_SANITIZE_IPV6) {
                    buffer.skip(dataLength); // remove ipv6 answers
                    skip = true;
                    modified = true;
                }
            }

            if (answer == null && !skip) {
                // didn't parse answer data, use not parsed

                byte[] buf = new byte[dataLength];
                buffer.readFully(buf);
                answer = new DNSAnswer(domain, type, classType, ttl);
                answer.data = buf;
            }

            if (answer != null) {
                answers.add(answer);
                if (Settings.DNS_USE_CACHE)
                    DNSCache.addAnswer(answer);
            }

            if (Settings.DEBUG_DNS) {
                if (answer != null)
                    L.a(Settings.TAG_DNSRESPONSE, answer.toString() + " type = " + type + " dataLength = " + dataLength);
            }
        }

        //L.d(Settings.TAG_DNSRESPONSE, "Has ipv6 answers: " + hasIpV6answers);
        return true;
    }

    public ArrayList<DNSAnswer> getAnswers() {
        return answers;
    }

    public Packet getPacket() {
        // TODO XXX if it can not write into small packet it reallocs bigger and bigger

        Packet p = PacketPool.alloc(PacketPool.POOL1_PACKET_SIZE);

        Packet res = writePacket(p);
        if (res == null) {
            PacketPool.release(p);
            p = PacketPool.alloc(PacketPool.POOL2_PACKET_SIZE);

            res = writePacket(p);
            if (res == null) {
                PacketPool.release(p);
                p = PacketPool.alloc(PacketPool.POOL3_PACKET_SIZE);

                res = writePacket(p);
                if (res == null) {
                    PacketPool.release(p);
                    p = PacketPool.alloc(PacketPool.POOL4_PACKET_SIZE);

                    res = writePacket(p);
                }
            }
        }

        return res;
    }

    private Packet writePacket(Packet p) {
        try {
            // transactionID, flags, questions, answers, authorities, additional
            int off = 28;
            p.frame[off] = (byte) ((transactionID & 0xFF00) >> 8);
            p.frame[off + 1] = (byte) (transactionID & 0xFF);

            int flags = ERROR_NO_SUCH_NAME;
            if (answers != null && answers.size() > 0)
                flags = NO_ERROR;

            p.frame[off + 2] = (byte) ((flags & 0xFF00) >> 8);
            p.frame[off + 3] = (byte) (flags & 0xFF);

            p.frame[off + 4] = (byte) 0;
            p.frame[off + 5] = (byte) 1;

            p.frame[off + 6] = (byte) 0;
            p.frame[off + 7] = (byte) (flags == NO_ERROR ? answers.size() : 0);

            p.frame[off + 8] = (byte) 0;
            p.frame[off + 9] = (byte) 0;

            p.frame[off + 10] = (byte) 0;
            p.frame[off + 11] = (byte) 0;

            for (int i = 0; i < domains.length; ++i) {
                if (domains[i] != null)
                    off = writeQuestion(p.frame, domains[i], types[i], classes[i]);
            }
            if (flags == NO_ERROR) {
                int size = answers.size();
                for (int i = 0; i < size; i++) {
                    DNSAnswer answer = answers.get(i);
                    off = writeAnswer(p.frame, off, answer);
                }
            }

            p.addIpUdpHeader(serverIp, serverPort, clientIp, clientPort, off - 28);

            return p;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private int writeAnswer(byte[] frame, int off, DNSAnswer answer) {
        if (answer.type != 1 && answer.type != 5)
            return off;

        off = writeLabel(frame, off, answer.domain);

        frame[off++] = (byte) ((answer.type & 0xFF00) >> 8);
        frame[off++] = (byte) (answer.type & 0xFF);

        frame[off++] = (byte) ((answer.clazz & 0xFF00) >> 8);
        frame[off++] = (byte) (answer.clazz & 0xFF);

        frame[off++] = (byte) ((answer.ttl & 0xFF000000) >> 24);
        frame[off++] = (byte) ((answer.ttl & 0x00FF0000) >> 16);
        frame[off++] = (byte) ((answer.ttl & 0x0000FF00) >> 8);
        frame[off++] = (byte) (answer.ttl & 0xFF);

        if (answer.ip != null) {
            byte[] answerIp = answer.ip;
            frame[off++] = 0;
            frame[off++] = 4;

            frame[off++] = answerIp[0];
            frame[off++] = answerIp[1];
            frame[off++] = answerIp[2];
            frame[off++] = answerIp[3];
        } else if (answer.cname != null) {
            final int fullLen = answer.cname.length() + 2;
            frame[off++] = (byte) ((fullLen & 0xFF00) >> 8);
            frame[off++] = (byte) (fullLen & 0xFF);

            off = writeLabel(frame, off, answer.cname);
        } else if (answer.data != null) {
            final int len = answer.data.length;
            frame[off++] = (byte) ((len & 0xFF00) >> 8);
            frame[off++] = (byte) (len & 0xFF);
            System.arraycopy(answer.data, 0, frame, off, len);
            off += len;
        }

        return off;
    }

    private int writeLabel(byte[] frame, int off, String s) {
        String[] parts = s.split("\\.");

        for (String part : parts) {
            final int len = part.length();
            frame[off++] = (byte) len;

            for (int i = 0; i < len; ++i) {
                char c = part.charAt(i);
                frame[off++] = (byte) c;
            }
        }

        frame[off++] = 0;

        return off;
    }

    private int writeQuestion(byte[] frame, String domain, int type, int clazz) {
        String[] parts = domain.split("\\.");
        int offset = 28 + 12;

        for (String part : parts) {
            frame[offset++] = (byte) part.length();
            for (int i = 0; i < part.length(); i++)
                frame[offset++] = (byte) part.charAt(i);
        }
        // end of domain name
        frame[offset++] = 0;

        // record type A
        frame[offset++] = (byte) ((type & 0xFF00) >> 8);
        frame[offset++] = (byte) (type & 0xFF);

        // network class INET
        frame[offset++] = (byte) ((clazz & 0xFF00) >> 8);
        frame[offset++] = (byte) (clazz & 0xFF);

        return offset;
    }

    public long getHash() {
        long hash = clientPort * 100000 + transactionID;
        return hash;
    }
}
