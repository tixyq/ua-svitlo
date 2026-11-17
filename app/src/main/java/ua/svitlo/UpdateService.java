package ua.svitlo;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/** Используется только на Android до 3.0, где нет goAsync(). */
public class UpdateService extends IntentService {
    private static PowerManager.WakeLock lock;

    public UpdateService() { super("svitlo-update"); }

    /** Пока служба стартует и ходит в сеть, устройство не должно уснуть (не больше минуты). */
    static synchronized void start(Context c) {
        try {
            if (lock == null) {
                lock = ((PowerManager) c.getSystemService(Context.POWER_SERVICE))
                        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "svitlo");
                lock.setReferenceCounted(false);
            }
            lock.acquire(60000);
        } catch (Throwable t) {
            // без блокировки сна работает как раньше
        }
        try {
            c.startService(new Intent(c, UpdateService.class));
        } catch (Throwable t) {
            release();
        }
    }

    static synchronized void release() {
        try {
            if (lock != null && lock.isHeld()) lock.release();
        } catch (Throwable t) {
            // уже отпущена
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            Updater.run(getApplicationContext());
        } finally {
            release();
        }
    }
}
