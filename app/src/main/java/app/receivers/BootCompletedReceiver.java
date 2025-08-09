package app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import app.App;
import app.dependencies.ApplicationDependencies;
import app.internal.Preferences;
import app.internal.Settings;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!App.isLibsLoaded()) {
            return;
        }

        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)
                && !"com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }

        if (Preferences.isActive()) {
            ApplicationDependencies.getVpnHelper().startVpn();

            boolean need = Preferences.get(Settings.PREF_CLEARCACHES_NEED);
            if (need) {
                App.cleanCaches(true, false, true, false); // clean+kill was requested (make only clean)
            }
        }

    }

}
