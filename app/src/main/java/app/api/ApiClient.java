package app.api;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.Config;
import app.ad.Ad;
import app.dependencies.ApplicationDependencies;
import app.internal.Settings;
import app.preferences.Preferences;
import app.util.InstallId;
import app.util.Toolbox;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private final Context context;
    private final InstallId installId;
    private final IApiClient apiClient;

    public ApiClient(@NonNull Context context, @NonNull String url) {
        this.context = context;
        this.installId = new InstallId();

        final Cache cache = new Cache(context.getCacheDir(), 10 * 1024 * 1024);

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .cache(cache)
                .build();

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        this.apiClient = retrofit.create(IApiClient.class);
    }

    @WorkerThread
    @Nullable
    public Ad getAd() {
        try {
            final Preferences preferences = ApplicationDependencies.getPreferences();
            ApiTimes apiTimes = preferences.getApiTimes();

            long now = System.currentTimeMillis() / 1000;

            if (now - apiTimes.timestamp < apiTimes.maxAge) {
                return null;
            }

            Response<Ad> response = apiClient.getAd(Toolbox.getAppVersion(context), Build.VERSION.RELEASE, Build.MANUFACTURER, Build.MODEL, Settings.PUBLISHER, installId.getInstallIdStr(), Toolbox.getCurrentLanguage(), Config.DEBUG, apiTimes.lastTime).execute();

            String _lastTime = response.headers().get("Last-Time"); // The time of last modification in the list of themes on the server
            String _cacheControl = response.headers().get("Cache-Control");

            long _maxAge = 0;

            if (_cacheControl != null) {
                Pattern pattern = Pattern.compile("^max-age=([0-9]+)$");
                Matcher matcher = pattern.matcher(_cacheControl);
                if (matcher.matches()) {
                    MatchResult result = matcher.toMatchResult();
                    _maxAge = Long.parseLong(result.group(1));
                }
            }

            apiTimes.timestamp = now;
            apiTimes.maxAge = _maxAge;

            if (_lastTime != null) {
                apiTimes.lastTime = Long.parseLong(_lastTime);
            }

            preferences.setApiTimes(apiTimes);

            Ad ad = response.body();
            if (ad == null || ad.id == null) {
                return null;
            }

            return response.body();
        } catch (IOException ignored) {
        }
        return null;
    }

    @WorkerThread
    public void log(String msg) {
        try {
            apiClient.log(Toolbox.getAppVersion(context), Build.VERSION.RELEASE, Build.MANUFACTURER, Build.MODEL, Settings.PUBLISHER, installId.getInstallIdStr(), Toolbox.getCurrentLanguage(), Config.DEBUG, msg).execute();
        } catch (IOException ignored) {
        }
    }

}

