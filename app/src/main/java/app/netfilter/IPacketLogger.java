package app.netfilter;

import app.netfilter.proxy.Packet;

public interface IPacketLogger {
    void log(Packet packet, boolean parsed);
}
