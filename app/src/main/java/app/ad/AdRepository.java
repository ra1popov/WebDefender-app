package app.ad;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

public class AdRepository {

    private final Context context;
    private final AdLiveData ad;

    public AdRepository(@NonNull Context context) {
        this.context = context;
        this.ad = new AdLiveData(context);
    }

    public AdLiveData getAd() {
        return ad;
    }

    public void requestAd() {
        ad.requestAd();
    }

    @NonNull
    public static Uri getAdPreview(@NonNull Ad ad) {
        return Uri.parse(ad.image);
    }

}
