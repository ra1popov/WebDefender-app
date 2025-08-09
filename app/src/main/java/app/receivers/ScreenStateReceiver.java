package app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import app.App;
import app.internal.Preferences;
import app.internal.Settings;

public class ScreenStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!App.isLibsLoaded()) {
            return;
        }

        String action = intent.getAction();
        boolean screenOn = Intent.ACTION_SCREEN_ON.equals(action);

        if (!screenOn) {
            boolean need = Preferences.get(Settings.PREF_CLEARCACHES_NEED);
            if (need) {
                App.cleanCaches(true, false, false, false); // clean+kill was requested
            }
        }
    }

}
