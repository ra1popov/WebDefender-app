package app.api;

import androidx.annotation.Keep;

import app.ad.Ad;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@Keep
public interface IApiClient {

    @GET("/config/")
    Call<Ad> getAd(
            @Query("app_version") String appVersion,
            @Query("android_version") String androidVersion,
            @Query("vendor_name") String vendorName,
            @Query("model_name") String modelName,
            @Query("publisher") String publisher,
            @Query("install_id") String installId,
            @Query("language") String language,
            @Query("debug") Boolean debug,
            @Query("last_time") Long lastTime
    );

    @GET("/trace/")
    Call<Void> log(
            @Query("app_version") String appVersion,
            @Query("android_version") String androidVersion,
            @Query("vendor_name") String vendorName,
            @Query("model_name") String modelName,
            @Query("publisher") String publisher,
            @Query("install_id") String installId,
            @Query("language") String language,
            @Query("debug") Boolean debug,
            @Query("msg") String msg
    );

}
