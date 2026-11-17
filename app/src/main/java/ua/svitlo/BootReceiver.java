package ua.svitlo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** После перезагрузки, смены времени/пояса и обновления приложения заново ставит будильник. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        Scheduler.schedule(c.getApplicationContext());
    }
}
