package ua.svitlo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

/** Один будильник на всё: ближайшее из «следующее обновление» и «пора предупредить о смене света». */
final class Scheduler {
    private Scheduler() {}

    private static PendingIntent pi(Context c) {
        return PendingIntent.getBroadcast(c, 1, new Intent(c, AlarmReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | Compat.immutable());
    }

    static void schedule(Context c) {
        Repo r = Repo.get(c);
        Store st = r.st;
        ArrayList<Repo.Loc> ls = r.list();
        PendingIntent pi = pi(c);
        if (ls.isEmpty()) {
            ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(pi);
            return;
        }
        long now = System.currentTimeMillis();
        long next = st.lastRefresh() + st.bgMin() * 60000L;
        if (next < now + 2000) next = now + 2000;
        for (int i = 0; i < ls.size(); i++) {
            Repo.Loc l = ls.get(i);
            Schedule s = r.schedule(l.id);
            if (s == null) continue;
            Schedule.Change nc = s.nextChange(now);
            if (nc == null) continue;
            long before = (nc.toOn ? st.onMin() : st.offMin()) * 60000L;
            if (before <= 0) continue;
            long t = nc.time - before;
            if (t <= now) {
                if (st.getL("ntf_" + l.id, 0) != nc.time) next = Math.min(next, now + 2000);
            } else if (t < next) {
                next = t;
            }
        }
        Compat.setAlarm(c, next, pi);
    }
}
