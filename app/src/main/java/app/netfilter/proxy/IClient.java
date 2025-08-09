package app.netfilter.proxy;

import java.nio.channels.SelectionKey;

public abstract interface IClient {

    public abstract boolean allowConnection();

    public abstract boolean denyConnection();

    public abstract void destroy(boolean silent);

    public abstract boolean isDead();

    public abstract void setDead();

    public abstract void dump();

    public abstract byte[] getLocalIp();

    public abstract int getLocalPort();

    public abstract String[] getPkgs();

    public abstract int getUid();

    public abstract int getProtocolNo();

    public abstract byte[] getServerIp();

    public abstract int getServerPort();

    public abstract boolean isPending();

    public abstract boolean isProxied();

    public abstract boolean onSockEvent(SelectionKey selectionKey);

    public abstract boolean onTimeout(long currentTime);

    public abstract void clearTimeout();

    public abstract long getTimeout();

    public abstract boolean onTunInput(Packet packet);

}
