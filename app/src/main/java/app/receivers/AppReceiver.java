package app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

import app.App;
import app.security.Browsers;
import app.security.Firewall;
import app.security.Processes;
import app.workers.AppUidWorker;

public class AppReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Fix the problem with endless calls until the protection in the app is turned on.
        // This problem was observed in the Android 33 emulator, but it is not known if the problem is on real devices
        String data = intent.getDataString();
        if (data == null || Objects.equals("package:" + context.getPackageName(), data)) {
            return;
        }

        if (!App.isLibsLoaded()) {
            return;
        }

        AppUidWorker.hashAllUid(context); // We start the service that gathers UID information for all apps (to determine the packageName by UID on Android 7 and above, since Google has restricted access to /proc).

        Processes.clearCaches();                        // reset processes uids info
        Browsers.clearCaches();                         // reset browsers uids info
        Firewall.clearCaches(true, true);   // reset firewall uids info
    }

}
