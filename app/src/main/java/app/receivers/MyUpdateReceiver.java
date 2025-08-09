package app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import app.App;
import app.updater.UpdaterService;
import app.workers.AppUidWorker;

public class MyUpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!App.isLibsLoaded()) {
            return;
        }

        AppUidWorker.hashAllUid(context); // We start a service that collects UID information for all apps (to determine packageName by UID on Android 7 and above, since Google has restricted access to /proc).
        UpdaterService.startUpdate(UpdaterService.START_FORCE); // check updates
    }

}
