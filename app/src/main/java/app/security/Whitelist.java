package app.security;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import app.App;
import app.common.Utils;

public class Whitelist {

    public static final String APPS_WHITELIST = "apps_whitelist";

    private static final HashSet<String> allowed = new HashSet<>();

    private static boolean inited = false;
    private static final String whitelistFileName;
    private static final Object lock = new Object();

    static {
        Context context = App.getContext();
        whitelistFileName = context.getFilesDir().getAbsolutePath() + "/whitelist.json";
    }

    public static void init() {
        synchronized (lock) {
            if (inited) {
                return;
            }

            clear();
            load();
            inited = true;
        }
    }

    private static void clear() {
        allowed.clear();
    }

    private static void load() {
        synchronized (lock) {
            try {
                // load
                String str = Utils.getFileContents(whitelistFileName);
                if (str == null) {
                    return;
                }

                try {
                    JSONObject obj0 = new JSONObject(str);
                    JSONArray arr = obj0.optJSONArray(APPS_WHITELIST);
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            if (obj == null) {
                                continue;
                            }
                            String pkgName = obj.optString("packageName");
                            if (TextUtils.isEmpty(pkgName)) {
                                continue;
                            }
                            allowed.add(pkgName);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        synchronized (lock) {
            if (!inited) {
                return;
            }

            JSONObject obj = new JSONObject();
            try {
                JSONArray arr = getAllowedApps();
                obj.put(APPS_WHITELIST, arr);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // save
            byte[] data = obj.toString().getBytes();
            try {
                Utils.saveFile(data, whitelistFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static JSONArray getAllowedApps() {
        JSONArray arr = new JSONArray();
        for (String pkgName : allowed) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("packageName", pkgName);
                arr.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return arr;
    }

    public static boolean isAllowed(String pkgName) {
        return allowed.contains(pkgName);
    }

    public static void setAllowed(String pkgName, boolean allow) {
        if (allow) {
            allowed.add(pkgName);
        } else {
            allowed.remove(pkgName);
        }
    }

    public static List<String> getAllowed() {
        return new ArrayList<>(allowed);
    }

}
