package app.netfilter.proxy;

public abstract interface IThreadEventListener {
    public abstract void onThreadFinished(boolean isError);
}
