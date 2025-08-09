package app.netfilter.proxy;

import java.nio.channels.SocketChannel;

public class ChannelData {

    public SocketChannel channel;
    public int fd = 0;

    public int encryptKey1Pos;
    public int encryptKey2Pos;
    public int decryptKey1Pos;
    public int decryptKey2Pos;
    public byte[] key1;
    public byte[] key2;
    public int key1Len;
    public int key2Len;

    public long poolAddTime;
    public long lastPingTime = 0;
    public long failedPingCount = 0;

    public ChannelData(SocketChannel channel, int fd,
                       int encryptKey1Pos, int encryptKey2Pos, int decryptKey1Pos, int decryptKey2Pos,
                       byte[] key1, byte[] key2, int key1Len, int key2Len) {
        this.channel = channel;
        this.fd = fd;

        this.encryptKey1Pos = encryptKey1Pos;
        this.encryptKey2Pos = encryptKey2Pos;
        this.decryptKey1Pos = decryptKey1Pos;
        this.decryptKey2Pos = decryptKey2Pos;
        this.key1 = key1;
        this.key2 = key2;
        this.key1Len = key1Len;
        this.key2Len = key2Len;

        this.poolAddTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return ((channel != null) ? channel.toString() : null);
    }
}
