package app.ui.firewall;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import app.application.ApplicationInfo;

public class FirewallViewModel extends ViewModel {

    public final MutableLiveData<List<ApplicationInfo>> list = new MutableLiveData<>();
    public final MutableLiveData<Boolean> browsers = new MutableLiveData<>(false);

    public FirewallViewModel() {

    }

    public void setList(List<ApplicationInfo> list) {
        this.list.postValue(list);
    }

    public void setInternetForBrowsersOnly(boolean enabled) {
        this.browsers.postValue(enabled);
    }

}
