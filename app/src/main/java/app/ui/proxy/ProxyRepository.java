package app.ui.proxy;

import android.content.Context;

import androidx.annotation.NonNull;


public class ProxyRepository {

    private final Context context;
    private final ProxyLiveData proxy;

    public ProxyRepository(@NonNull Context context) {
        this.context = context;
        this.proxy = new ProxyLiveData(context);
    }

    public ProxyLiveData getProxy() {
        return proxy;
    }

    public void requestProxy() {
        proxy.requestProxy();
    }

}
