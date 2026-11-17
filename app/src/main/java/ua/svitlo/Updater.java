package ua.svitlo;

import android.content.Context;

import java.util.ArrayList;

/** Фоновая работа по будильнику: обновить графики, сообщить об изменениях и о скорой смене света. */
final class Updater {
    private Updater() {}

    static void run(Context c) {
        try {
            Repo r = Repo.get(c);
            long now = System.currentTimeMillis();
            if (now - r.st.lastRefresh() >= r.st.bgMin() * 60000L - 15000L) {
                ArrayList<Repo.Diff> diffs = r.refreshAllSync();
                now = System.currentTimeMillis();
                for (int i = 0; i < diffs.size(); i++) {
                    Repo.Diff d = diffs.get(i);
                    String txt = changeText(d.old, d.nw, now);
                    if (txt != null) Compat.post(c, id(d.loc.id, 2), Compat.K_NEUTRAL, d.loc.name, S.t("changed") + " " + txt);
                }
                r.fire();
            }
            transitions(c, r, now);
        } catch (Throwable t) {
            // фон не должен падать
        } finally {
            Scheduler.schedule(c);
        }
    }

    private static int id(long locId, int k) {
        return (int) (locId % 1000000L) * 4 + k;
    }

    private static void transitions(Context c, Repo r, long now) {
        ArrayList<Repo.Loc> ls = r.list();
        for (int i = 0; i < ls.size(); i++) {
            Repo.Loc l = ls.get(i);
            Schedule s = r.schedule(l.id);
            if (s == null) continue;
            Schedule.Change nc = s.nextChange(now);
            if (nc == null) continue;
            long before = (nc.toOn ? r.st.onMin() : r.st.offMin()) * 60000L;
            if (before <= 0) continue;
            if (now >= nc.time - before && now < nc.time && r.st.getL("ntf_" + l.id, 0) != nc.time) {
                r.st.putL("ntf_" + l.id, nc.time);
                long mins = (nc.time - now + 59999) / 60000;
                String txt = S.t(nc.toOn ? "will_on" : "will_off") + " " + S.durLong(mins);
                Compat.post(c, id(l.id, nc.toOn ? 1 : 0), nc.toOn ? Compat.K_ON : Compat.K_OFF, l.name, txt);
            }
        }
    }

    /** Что изменилось в будущих слотах; null, если ничего существенного. */
    static String changeText(Schedule old, Schedule nw, long now) {
        String today = Schedule.dateOfOffset(now, 0), tomorrow = Schedule.dateOfOffset(now, 1);
        int nowSlot = Schedule.cal(now).get(java.util.Calendar.HOUR_OF_DAY) * 2
                + Schedule.cal(now).get(java.util.Calendar.MINUTE) / 30;
        StringBuilder sb = new StringBuilder();
        for (int d = 0; d < nw.dates.length; d++) {
            String ds = nw.dates[d];
            if (ds.compareTo(today) < 0 || !nw.hasData(d)) continue;
            int o = old.dateIndex(ds);
            boolean changed = false;
            if (o < 0) {
                changed = true; // опубликован новый день
            } else {
                for (int s = ds.equals(today) ? nowSlot : 0; s < 48; s++) {
                    if (nw.v[d][s] != 0 && nw.v[d][s] != old.v[o][s]) { changed = true; break; }
                }
            }
            if (!changed) continue;
            String label = ds.equals(today) ? S.t("today") : ds.equals(tomorrow) ? S.t("tomorrow")
                    : ds.substring(8) + "." + ds.substring(5, 7);
            String sum = nw.outageSummary(d);
            if (sb.length() > 0) sb.append("; ");
            sb.append(label).append(": ").append(sum.length() == 0 ? S.t("no_outages_short") : sum);
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
