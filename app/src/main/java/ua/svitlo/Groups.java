package ua.svitlo;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Группы региона: один запрос списка к серверу (/<регион>/), кэш на 7 дней, один запрос на регион одновременно.
 * Запасной путь для старого сервера без списков: перебор «группа × подгруппа» (HEAD, стоп после 5 пустых групп).
 */
final class Groups {
    static final long TTL = 7L * 24 * 3600 * 1000;
    private static final int MAX_GROUP = 99, MAX_MISS = 5;

    interface CB { void on(List<String> list, boolean done, boolean error); }

    private static final HashMap<String, ArrayList<CB>> running = new HashMap<String, ArrayList<CB>>();

    private static List<String> cached(Store st, String region) {
        ArrayList<String> r = new ArrayList<String>();
        String s = st.getS("grp_" + region);
        if (s != null && s.length() > 0) {
            String[] p = s.split(",");
            for (int i = 0; i < p.length; i++) r.add(p[i]);
        }
        return r;
    }

    static void get(Context c, final String region, CB cb) {
        final Store st = Store.get(c);
        List<String> cached = cached(st, region);
        long t = st.getL("grpT_" + region, 0);
        if (t > 0 && System.currentTimeMillis() - t < TTL) {
            cb.on(cached, true, false);
            return;
        }
        if (cached.size() > 0) cb.on(cached, false, false);
        synchronized (running) {
            ArrayList<CB> l = running.get(region);
            if (l != null) { l.add(cb); return; }
            l = new ArrayList<CB>();
            l.add(cb);
            running.put(region, l);
        }
        new Thread() {
            public void run() { discover(st, region); }
        }.start();
    }

    static void cancel(CB cb) {
        if (cb == null) return;
        synchronized (running) {
            for (ArrayList<CB> l : running.values()) l.remove(cb);
        }
    }

    private static void post(final String region, final List<String> found, final boolean done, final boolean err) {
        Ui.H.post(new Runnable() {
            public void run() {
                ArrayList<CB> copy;
                synchronized (running) {
                    ArrayList<CB> l = running.get(region);
                    copy = l == null ? new ArrayList<CB>() : new ArrayList<CB>(l);
                    if (done) running.remove(region);
                }
                for (int i = 0; i < copy.size(); i++) copy.get(i).on(found, done, err);
            }
        });
    }

    private static void discover(Store st, String region) {
        Api.Result r = Api.list(region);
        if (r.error && r.code == 0) { // сети нет: перебирать бессмысленно
            post(region, new ArrayList<String>(), true, true);
            return;
        }
        if (r.code == 404) { // у сервера со списками это «данных по региону нет»
            finish(st, region, new ArrayList<String>());
            return;
        }
        if (r.code == 200 && r.body != null) {
            ArrayList<String> found = parseList(r.body);
            if (!found.isEmpty()) { finish(st, region, found); return; }
        }
        bruteForce(st, region); // старый сервер без списков: как раньше, перебором
    }

    /** "42<TAB>1,2" -> "42.1", "42.2". Строки не по формату пропускаются. */
    static ArrayList<String> parseList(String body) {
        ArrayList<String> out = new ArrayList<String>();
        String[] lines = body.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0) continue;
            String[] p = line.split("\t");
            String g = p[0].trim();
            if (!digits(g)) continue;
            if (p.length < 2) { out.add(g + ".1"); continue; }
            String[] subs = p[1].split(",");
            for (int k = 0; k < subs.length; k++) {
                String s = subs[k].trim();
                if (digits(s)) out.add(g + "." + s);
            }
        }
        return out;
    }

    private static boolean digits(String s) {
        if (s.length() == 0 || s.length() > 3) return false;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) < '0' || s.charAt(i) > '9') return false;
        return true;
    }

    private static void finish(Store st, String region, ArrayList<String> found) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < found.size(); i++) { if (i > 0) sb.append(','); sb.append(found.get(i)); }
        st.putS("grp_" + region, sb.toString());
        st.putL("grpT_" + region, System.currentTimeMillis());
        post(region, found, true, false);
    }

    private static void bruteForce(Store st, String region) {
        ArrayList<String> found = new ArrayList<String>();
        boolean err = false;
        int miss = 0;
        for (int g = 1; g <= MAX_GROUP && miss < MAX_MISS; g++) {
            boolean any = false;
            for (int s = 1; s <= 2; s++) {
                int code = Api.probe(region, String.valueOf(g), String.valueOf(s));
                if (code == 200) { found.add(g + "." + s); any = true; }
                else if (code != 404) { err = true; break; }
            }
            if (err) break;
            miss = any ? 0 : miss + 1;
            if (any) post(region, new ArrayList<String>(found), false, false);
        }
        if (err) post(region, found, true, true);
        else finish(st, region, found);
    }
}
