package app.recovery;

import static app.recovery.RecoveryNativeService.COMMAND;
import static app.recovery.RecoveryNativeService.Command.ping;
import static app.recovery.RecoveryNativeService.Command.stop;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import app.dependencies.ApplicationDependencies;
import app.internal.Settings;
import app.util.Toolbox;
import app.util.Util;

public class RecoveryService extends Service {

    private final ServiceConnection recoveryNativeServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messengerRecoveryNativeService = new Messenger(service);
            notifyServiceReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

    };

    private class RecoveryHandler extends Handler {

        public RecoveryHandler() {
            super(Looper.getMainLooper());
        }

        public RecoveryHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PING_FROM_VPN:
                    Util.cancelRunnableOnMain(lostPingFromVpnNotify);
                    Util.postDelayedToMain(lostPingFromVpnNotify, 100000);
                    Util.postDelayedToMain(lostPingFromNativeNotify, 5000);

                    runOnRecoveryNativeService(() -> ping);
                    break;

                case MSG_PING_FROM_NATIVE:
                    Util.cancelRunnableOnMain(lostPingFromNativeNotify);
                    break;
            }

        }

    }

    public static final int MSG_PING_FROM_VPN = 1;
    public static final int MSG_PING_FROM_NATIVE = 2;
    public static final int JOB_ID = 1351;
    public static final int CMD_STOP = 1;
    public static final String EXTRA_CMD = "cmd";


    private final Runnable restartNotify = new Runnable() {
        @Override
        public void run() {
            startService(RecoveryService.this);
            startForeground();
        }
    };

    private final Runnable lostPingFromVpnNotify = new Runnable() {
        @Override
        public void run() {
            ApplicationDependencies.getVpnHelper().startVpn();
        }
    };

    private final Runnable lostPingFromNativeNotify = new Runnable() {
        @Override
        public void run() {
            RecoveryNativeService.bind(RecoveryService.this, recoveryNativeServiceConnection);
        }
    };

    private final Messenger messengerRecoveryService = new Messenger(new RecoveryHandler());
    private Messenger messengerRecoveryNativeService;
    private final List<Callable<RecoveryNativeService.Command>> serviceReadyCallbacks = new ArrayList<>();
    private boolean isStoped;

    @Override
    public void onCreate() {
        super.onCreate();

        startForeground();

        RecoveryNativeService.bind(this, recoveryNativeServiceConnection);

        /*
         Android does not always call onStartCommand after onCreate for a restarted process.
         Therefore, we manually help Android start our service if it fails to do so.
         */
        Util.postDelayedToMain(restartNotify, 2000);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Util.cancelRunnableOnMain(restartNotify);

        startForeground();

        if (isStoped || (intent != null && intent.getIntExtra(EXTRA_CMD, -1) == CMD_STOP)) {
            isStoped = true;
            Util.cancelRunnableOnMain(lostPingFromVpnNotify);
            Util.cancelRunnableOnMain(lostPingFromNativeNotify);
            runOnRecoveryNativeService(() -> stop);
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messengerRecoveryService.getBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        RecoveryNativeService.unbind(this, recoveryNativeServiceConnection);

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static void startService(@NonNull Context context) {
        ContextCompat.startForegroundService(context, new Intent(context, RecoveryService.class));
    }

    public static void stopService(@NonNull Context context) {
        ContextCompat.startForegroundService(context, new Intent(context, RecoveryService.class).putExtra(EXTRA_CMD, CMD_STOP));
    }

    public static synchronized void bind(@NonNull Context context, @NonNull ServiceConnection serviceConnection) {
        _unbind(context, serviceConnection);
        Intent intent = new Intent(context, RecoveryService.class);
        context.bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    public static synchronized void unbind(@NonNull Context context, @NonNull ServiceConnection serviceConnection) {
        _unbind(context, serviceConnection);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void runOnRecoveryNativeService(@NonNull Callable<RecoveryNativeService.Command> callable) {
        if (messengerRecoveryNativeService == null) {
            serviceReadyCallbacks.add(callable);
        } else {
            try {
                RecoveryNativeService.Command command = callable.call();
                sendMessage(command);
            } catch (Exception ignored) {
            }
        }
    }

    private void notifyServiceReady() {
        for (Callable<RecoveryNativeService.Command> callable : serviceReadyCallbacks) {
            try {
                RecoveryNativeService.Command command = callable.call();
                sendMessage(command);
            } catch (Exception ignored) {
            }
        }
        serviceReadyCallbacks.clear();
    }

    private void sendMessage(@NonNull RecoveryNativeService.Command command) {
        Message message = Message.obtain();
        try {
            Bundle data = new Bundle();
            data.putSerializable(COMMAND, command);
            message.setData(data);
            message.replyTo = messengerRecoveryService;
            messengerRecoveryNativeService.send(message);
        } catch (Exception ignored) {
        }
    }

    private static void _unbind(@NonNull Context context, @NonNull ServiceConnection serviceConnection) {
        try {
            context.unbindService(serviceConnection);
        } catch (Exception ignored) {
        }
    }

    private void startForeground() {
        startForeground(JOB_ID, Toolbox.getNotification(ApplicationDependencies.getApplication(), Settings.APP_NAME + " Recovery", Settings.APP_PACKAGE + ":recovery"));
    }

}
