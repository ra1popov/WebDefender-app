package app.recovery;

import static app.recovery.RecoveryService.MSG_PING_FROM_NATIVE;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Locale;

import app.Config;
import app.common.LibSelf;
import app.dependencies.ApplicationDependencies;
import app.internal.Settings;
import app.util.InstallId;
import app.util.Toolbox;

public class RecoveryNativeService extends Service {

    private class RecoveryNativeHandler extends Handler {

        public RecoveryNativeHandler() {
            super(Looper.getMainLooper());
        }

        public RecoveryNativeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle build = msg.getData();
            Command command = (Command) build.getSerializable(COMMAND);

            switch (command) {
                case stop:
                    runOnProcessCommandNative(() -> {
                        if (pingNativeThread != null) {
                            pingNativeThread.interrupt();
                            pingNativeThread = null;
                        }
                        LibSelf.power(false);
                    });
                    break;

                case ping:
                    runOnProcessCommandNative(() -> LibSelf.ping(SystemClock.elapsedRealtime()));

                    // We send a response confirming the request was received and the service is alive (if the native daemon is running).
                    try {
                        msg.replyTo.send(Message.obtain(null, MSG_PING_FROM_NATIVE));
                    } catch (RemoteException ignored) {
                    }
                    break;
            }
        }

    }

    public enum Command {
        stop,
        ping
    }

    public static final String COMMAND = "command";

    private final Messenger messengerRecoveryNativeService = new Messenger(new RecoveryNativeHandler());
    private InstallId installId;
    private String language;
    private HandlerThread pingNativeThread;
    private Handler processCommandNativeHandler;
    private String pushToken;


    @Override
    public void onCreate() {
        super.onCreate();

        installId = new InstallId();
        language = Locale.getDefault().getLanguage();

        HandlerThread processCommandNativeThread = new HandlerThread("processCommandNativeThread");
        processCommandNativeThread.start();
        processCommandNativeHandler = new Handler(processCommandNativeThread.getLooper());

        runOnProcessCommandNative(() -> LibSelf.go(Config.PING_BASE_URL, Toolbox.getAppVersion(ApplicationDependencies.getApplication()), Build.VERSION.RELEASE, Build.MANUFACTURER, Build.MODEL, Settings.PUBLISHER, installId.getInstallIdStr(), language));
        runOnProcessCommandNative(() -> LibSelf.power(true));

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                pushToken = task.getResult();
                runOnProcessCommandNative(() -> LibSelf.setup(Config.PING_BASE_URL, pushToken, Toolbox.getAppVersion(ApplicationDependencies.getApplication()), Build.VERSION.RELEASE, Build.MANUFACTURER, Build.MODEL, Settings.PUBLISHER, installId.getInstallIdStr(), language));
            }
        });

        pingNativeThread = new HandlerThread("pingNativeThread");
        pingNativeThread.start();

        new Handler(pingNativeThread.getLooper()).post(() -> {
            while (!Thread.interrupted()) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    break;
                }

                boolean isAlive = LibSelf.isAlive();

                // Restart the native daemon if it has been killed.
                if (!isAlive) {
                    runOnProcessCommandNative(() -> {
                        LibSelf.stop();
                        LibSelf.go(Config.PING_BASE_URL, Toolbox.getAppVersion(ApplicationDependencies.getApplication()), Build.VERSION.RELEASE, Build.MANUFACTURER, Build.MODEL, Settings.PUBLISHER, installId.getInstallIdStr(), language);
                        LibSelf.power(true);
                        LibSelf.setup(Config.PING_BASE_URL, pushToken, Toolbox.getAppVersion(ApplicationDependencies.getApplication()), Build.VERSION.RELEASE, Build.MANUFACTURER, Build.MODEL, Settings.PUBLISHER, installId.getInstallIdStr(), language);
                    });
                }
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messengerRecoveryNativeService.getBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (pingNativeThread != null) {
            pingNativeThread.interrupt();
            pingNativeThread = null;
        }
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////

    public static synchronized void bind(@NonNull Context context, @NonNull ServiceConnection serviceConnection) {
        _unbind(context, serviceConnection);
        Intent intent = new Intent(context, RecoveryNativeService.class);
        context.bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    public static synchronized void unbind(@NonNull Context context, @NonNull ServiceConnection serviceConnection) {
        _unbind(context, serviceConnection);
    }

    private static void _unbind(@NonNull Context context, @NonNull ServiceConnection serviceConnection) {
        try {
            context.unbindService(serviceConnection);
        } catch (Exception ignored) {
        }
    }

    private void runOnProcessCommandNative(@NonNull Runnable runnable) {
        processCommandNativeHandler.post(runnable);
    }

}
