package app.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import app.Config;
import app.api.ApiTimes;
import app.internal.Settings;
import app.preferences.model.Stat;
import app.util.Util;

public class Preferences {

    private static final String PREF_POWER = Settings.PREF_ACTIVE;
    private static final String PREF_STAT = "pref_stat";
    private static final String PREF_API_TIMES = "pref_api_times";
    private static final String PREF_LAST_TIME_REVIEW = "pref_last_time_review";
    private static final String PREF_LAST_TIME_AD = "pref_last_time_ad";
    private static final String PREF_LAST_TIME_SETUP = "pref_last_time_setup";
    private static final String PREF_LAST_TIME_BUY = "pref_last_time_buy";
    private static final String PREF_MADE_FIRST_SETUP = "pref_made_first_setup";
    private static final String PREF_PRO = "pref_pro";

    private final Context context;
    private final List<PreferencesObserver> observers = new ArrayList<>();

    public Preferences(Context context) {
        this.context = context;
    }

    public void registerObserver(PreferencesObserver observer) {
        observers.add(observer);
    }

    public void unregisterObserver(PreferencesObserver observer) {
        observers.remove(observer);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isPower() {
        return getBooleanPreference(PREF_POWER, false);
    }

    public void setPower(boolean on) {
        setBooleanPreference(PREF_POWER, on);
        notifyPreferencesObserver();
    }

    public Stat getStat() {
        Stat stat = new Gson().fromJson(getStringPreference(PREF_STAT, ""), Stat.class);
        if (stat == null) {
            return new Stat();
        }
        return stat;
    }

    public void setStat(Stat stat) {
        setStringPreference(PREF_STAT, new Gson().toJson(stat));
        notifyPreferencesObserver();
    }

    public void setApiTimes(ApiTimes apiTimes) {
        setStringPreference(PREF_API_TIMES, new Gson().toJson(apiTimes));
    }

    public ApiTimes getApiTimes() {
        ApiTimes apiTimes = new Gson().fromJson(getStringPreference(PREF_API_TIMES, ""), ApiTimes.class);
        if (apiTimes == null) {
            return new ApiTimes();
        }
        return apiTimes;
    }

    public boolean isNeedShowReviewDialog() {
        long lastTime = getLongPreference(PREF_LAST_TIME_REVIEW, 0);
        if (Config.DEBUG) {
            return true;
        } else {
            long now = System.currentTimeMillis();
            if (lastTime == 0 || now > lastTime + 86400000L) { // show review dialog after 24 hours
                return true;
            }
            return false;
        }
    }

    public void updateLastTimeReview() {
        setLongPreference(PREF_LAST_TIME_REVIEW, System.currentTimeMillis());
    }

    public boolean isNeedShowAdDialog() {
        long lastTime = getLongPreference(PREF_LAST_TIME_AD, 0);
        if (Config.DEBUG) {
            return true;
        } else {
            long now = System.currentTimeMillis();
            if (lastTime == 0 || now > lastTime + 86400000L) { // show ad dialog after 24 hours
                return true;
            }
            return false;
        }
    }

    public void updateLastTimeAd() {
        setLongPreference(PREF_LAST_TIME_AD, System.currentTimeMillis());
    }

    public boolean isNeedShowSetupDialog() {
        long lastTime = getLongPreference(PREF_LAST_TIME_SETUP, 0);
        if (Config.DEBUG) {
            return true;
        } else {
            long now = System.currentTimeMillis();
            if (lastTime == 0 || now > lastTime + 10800000L) { // show setup dialog after 3 hours
                return true;
            }
            return false;
        }
    }

    public void updateLastTimeSetup() {
        setLongPreference(PREF_LAST_TIME_SETUP, System.currentTimeMillis());
    }

    public boolean isNeedShowBuyDialog() {
        if (app.internal.Preferences.get(Settings.PREF_APP_ADBLOCK)) {
            return _isNeedShowBuyDialog2();
        }
        return _isNeedShowBuyDialog1(); // fix for Xiaomi to pass the test
    }

    private boolean _isNeedShowBuyDialog1() {
        long lastTime = getLongPreference(PREF_LAST_TIME_BUY, 0);
        if (Config.DEBUG) {
            return true;
        } else {
            long now = System.currentTimeMillis();
            if (lastTime == 0 || now > lastTime + 86400000L) { // show buy dialog on first launch and after 24 hours
                return true;
            }
            return false;
        }
    }

    private boolean _isNeedShowBuyDialog2() {
        long now = System.currentTimeMillis();
        long lastTime = getLongPreference(PREF_LAST_TIME_BUY, 0);
        if (lastTime == 0) {
            lastTime = now;
            setLongPreference(PREF_LAST_TIME_BUY, lastTime);
        }
        if (Config.DEBUG) {
            return true;
        } else {
            if (now > lastTime + 86400000L) { // show buy dialog after 24 hours
                return true;
            }
            return false;
        }
    }

    public void updateLastTimeBuy() {
        setLongPreference(PREF_LAST_TIME_BUY, System.currentTimeMillis());
    }

    public boolean isMadeFirstSetup() {
        return getBooleanPreference(PREF_MADE_FIRST_SETUP, false);
    }

    public void forceFirstSetup() {
        setBooleanPreference(PREF_MADE_FIRST_SETUP, true);
    }

    public void hideSetupDialog() {
        setLongPreference(PREF_LAST_TIME_SETUP, System.currentTimeMillis() + 31536000000L); // hide dialog (show setup dialog after 1 year)
    }

    public boolean isPro() {
        if ("amazon".equals(Settings.PUBLISHER)
                || "huawei".equals(Settings.PUBLISHER)
                || "samsung".equals(Settings.PUBLISHER)) {
            return getBooleanPreference(PREF_PRO, true);
        } else if ("xiaomi".equals(Settings.PUBLISHER)) {
            return getBooleanPreference(PREF_PRO, false);
        }
        return getBooleanPreference(PREF_PRO, false);
    }

    public void setPro(boolean on) {
        setBooleanPreference(PREF_PRO, on);
    }

    private void notifyPreferencesObserver() {
        for (PreferencesObserver observer : observers) {
            observer.onChange();
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static SharedPreferences getSharedPreferences(@NonNull Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    @SuppressLint("ApplySharedPref")
    private void setStringPreference(@NonNull String key, String value) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit().putString(key, value);
        if (Util.isMainThread()) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

    private String getStringPreference(@NonNull String key, @Nullable String defaultValue) {
        return getSharedPreferences(context).getString(key, defaultValue);
    }

    @SuppressLint("ApplySharedPref")
    private void setBooleanPreference(@NonNull String key, boolean value) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit().putBoolean(key, value);
        if (Util.isMainThread()) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

    private boolean getBooleanPreference(@NonNull String key, boolean defaultValue) {
        return getSharedPreferences(context).getBoolean(key, defaultValue);
    }

    private long getLongPreference(@NonNull String key, long defaultValue) {
        return getSharedPreferences(context).getLong(key, defaultValue);
    }

    @SuppressLint("ApplySharedPref")
    private void setLongPreference(@NonNull String key, long value) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit().putLong(key, value);
        if (Util.isMainThread()) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

    private int getIntegerPreference(@NonNull String key, int defaultValue) {
        return getSharedPreferences(context).getInt(key, defaultValue);
    }

    @SuppressLint("ApplySharedPref")
    private void setIntegerPreference(@NonNull String key, int value) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit().putInt(key, value);
        if (Util.isMainThread()) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

}
