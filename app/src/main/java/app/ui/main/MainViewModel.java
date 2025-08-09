package app.ui.main;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import app.internal.Preferences;
import app.internal.Settings;
import app.preferences.model.Stat;

public class MainViewModel extends ViewModel {

    public final MutableLiveData<Boolean> power = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> ad = new MutableLiveData<>(false);
    public final MutableLiveData<Stat> stat = new MutableLiveData<>();

    public MainViewModel() {
    }

    public void setPower(boolean on) {
        this.power.postValue(on);
    }

    public void setStat(Stat stat) {
        this.stat.postValue(stat);
    }

    public void updateState() {
        this.ad.postValue(Preferences.get(Settings.PREF_APP_ADBLOCK));
    }

}
