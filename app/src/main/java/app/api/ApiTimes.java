package app.api;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class ApiTimes implements Serializable {

    public long timestamp;
    public long lastTime;
    public long maxAge;

    public ApiTimes() {

    }

}
