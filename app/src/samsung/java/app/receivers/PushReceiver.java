package app.receivers;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import app.App;
import app.dependencies.ApplicationDependencies;
import app.internal.Preferences;

public class PushReceiver extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        if (!App.isLibsLoaded()) {
            return;
        }

        if (Preferences.isActive()) {
            ApplicationDependencies.getVpnHelper().startVpn();
        }
    }

}
