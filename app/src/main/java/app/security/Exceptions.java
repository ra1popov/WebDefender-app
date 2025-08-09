package app.security;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;

import app.App;
import app.common.Utils;

public class Exceptions {

    private static HashSet<String> statsExceptions = new HashSet<String>();
    private static HashSet<String> compressed = new HashSet<String>();

    public static boolean load(String version) {
        boolean res = false;
        String fileName = App.getContext().getFilesDir().getAbsolutePath() + "/" + version + "/" + "exceptions.db";

        FileInputStream fis = null;
        DataInputStream dis = null;

        try {
            fis = new FileInputStream(fileName);
            dis = new DataInputStream(fis);

            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[2048];
            int read;

            while ((read = dis.read(buf)) > 0)
                sb.append(new String(buf, 0, read));

            JSONArray items = new JSONArray(sb.toString());
            for (int i = 0; i < items.length(); ++i) {
                JSONObject obj = items.getJSONObject(i);

                if (obj.has("stats") && !obj.getBoolean("stats"))
                    statsExceptions.add(obj.getString("domain"));
                if (obj.has("compressed") && obj.getBoolean("compressed"))
                    compressed.add(obj.getString("domain"));
            }

            res = true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if (dis != null)
                try {
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return res;
    }

    public static boolean load() {
        String version = Database.getCurrentVersion();
        return load(version);
    }

    public static boolean exceptFromStats(String domain) {
        if (domain == null)
            return false;

        final String main = Utils.getMainDomain(domain);
        if (statsExceptions.contains(main))
            return true;

        final String third = Utils.getThirdLevelDomain(domain);
        return statsExceptions.contains(third);
    }

    public static boolean isCompressed(String domain) {
        return compressed.contains(domain);
    }

    public static String[] getCompressed() {
        String[] buf = new String[compressed.size()];
        compressed.toArray(buf);
        return buf;
    }
}
