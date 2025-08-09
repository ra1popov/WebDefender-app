package app.ui.proxy;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Collections;
import java.util.List;


public class ProxyViewModel extends ViewModel {

    public final MutableLiveData<List<Proxy>> list = new MutableLiveData<>();
    public final MutableLiveData<Boolean> show = new MutableLiveData<>(false);

    public ProxyViewModel() {
        this.list.postValue(Collections.emptyList());
    }

    public void setList(List<Proxy> list) {
        this.list.postValue(list);
    }

    public void setShow(boolean show) {
        this.show.postValue(show);
    }

}
