package app.recovery;

import android.content.Context;
import android.content.ServiceConnection;

import androidx.annotation.NonNull;

public class RecoveryService {

    public static final int MSG_PING_FROM_VPN = 1;

    public static void startService(@NonNull Context context) {
    }

    public static void stopService(@NonNull Context context) {
    }

    public static synchronized void bind(@NonNull Context context, @NonNull ServiceConnection serviceConnection) {

    }

    public static synchronized void unbind(@NonNull Context context, @NonNull ServiceConnection serviceConnection) {
    }

}
