package app.receivers;

import static app.updater.UpdaterService.START_EXACT;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import app.updater.UpdaterService;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        UpdaterService.startUpdate(intent.getIntExtra("startId", START_EXACT));
    }

}
