package app.jobmanager.impl;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import java.io.IOException;

import app.jobmanager.Data;
import app.jobmanager.JsonUtils;

@Keep
public class JsonDataSerializer implements Data.Serializer {

    @Override
    @NonNull
    public String serialize(@NonNull Data data) {
        try {
            return JsonUtils.toJson(data);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    @NonNull
    public Data deserialize(@NonNull String serialized) {
        try {
            return JsonUtils.fromJson(serialized, Data.class);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

}
