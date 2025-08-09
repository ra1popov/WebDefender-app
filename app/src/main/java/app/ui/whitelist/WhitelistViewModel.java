package app.ui.whitelist;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import app.application.ApplicationInfo;


public class WhitelistViewModel extends ViewModel {

    public final MutableLiveData<List<ApplicationInfo>> list = new MutableLiveData<>();
    public final MutableLiveData<Boolean> browsers = new MutableLiveData<>(false);

    public WhitelistViewModel() {

    }

    public void setList(List<ApplicationInfo> list) {
        this.list.postValue(list);
    }

    public void setInternetForBrowsersOnly(boolean enabled) {
        this.browsers.postValue(enabled);
    }
}
