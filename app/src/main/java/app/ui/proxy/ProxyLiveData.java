package app.ui.proxy;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import app.App;
import app.internal.Preferences;
import app.internal.ProxyBase;
import app.internal.Settings;

public class ProxyLiveData extends ProxyProviderLiveData<List<Proxy>> {

    private final List<ProxyObserver> observers = new ArrayList<>();

    public ProxyLiveData(@NonNull Context context) {
        super(context);
    }

    @Override
    protected List<Proxy> getContentProviderValue() {
        final String packageName = App.packageName();
        final Context c = App.getContext();
        final Resources res = c.getResources();

        List<Proxy> list = new ArrayList<>();

        String curServ = Preferences.get_s(Settings.PREF_PROXY_COUNTRY);

        for (CharSequence country : ProxyBase.getAvailableServers()) {
            String title = c.getString(res.getIdentifier("proxy_country_" + country, "string", packageName));
            int icon = res.getIdentifier("ic_country_" + country, "drawable", packageName);
            boolean selected = country.equals(curServ);
            list.add(new Proxy((String) country, title, icon, selected));
        }

        return list;
    }

    @Override
    protected void registerObserver(ProxyObserver observer) {
        observers.add(observer);
    }

    @Override
    protected void unregisterObserver(ProxyObserver observer) {
        observers.remove(observer);
    }

    public void requestProxy() {
        for (ProxyObserver observer : observers) {
            observer.onChange();
        }
    }

}
