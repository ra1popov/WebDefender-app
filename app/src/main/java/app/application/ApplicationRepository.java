package app.application;

import android.content.Context;

import androidx.annotation.NonNull;

public class ApplicationRepository {

    private final Context context;
    private final ApplicationLiveData list;

    public ApplicationRepository(@NonNull Context context) {
        this.context = context;
        this.list = new ApplicationLiveData(context);
    }

    public ApplicationLiveData getList() {
        return list;
    }

}
