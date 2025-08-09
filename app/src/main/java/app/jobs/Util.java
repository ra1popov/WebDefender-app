package app.jobs;

import androidx.annotation.NonNull;

import java.util.Map;

public class Util {

    public static <K, V> V getOrDefault(@NonNull Map<K, V> map, K key, V defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }

}
