package ua.svitlo;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/** Локации пользователя и кэш графиков. Загрузка стартует при запуске приложения. */
final class Repo {
    static final class Loc {
        long id;
        String name, region, group, sub;
        boolean pinned;
        String label() { return group + "." + sub; }
    }

    static final class Diff {
        Loc loc;
        Schedule old, nw;
    }

    interface Listener { void onData(); }

    private static Repo I;

    static synchronized Repo get(Context c) {
        if (I == null) I = new Repo(c.getApplicationContext());
        return I;
    }

    final Store st;
    private final ArrayList<Loc> locs = new ArrayList<Loc>();
    private final HashMap<Long, Schedule> cache = new HashMap<Long, Schedule>();
    private final ArrayList<Listener> ls = new ArrayList<Listener>();
    private final Object refreshLock = new Object();
    private volatile boolean busy;

    private Repo(Context c) {
        st = Store.get(c);
        try {
            String s = st.getS("locs");
            if (s != null) {
                JSONArray a = new JSONArray(s);
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    Loc l = new Loc();
                    l.id = o.getLong("id");
                    l.name = o.getString("name");
                    l.region = o.getString("region");
                    l.group = o.getString("group");
                    l.sub = o.getString("sub");
                    l.pinned = o.optBoolean("pinned", false);
                    locs.add(l);
                }
            }
        } catch (Exception e) {
            // битые данные: начинаем с пустого списка
        }
    }

    private void saveLocs() {
        try {
            JSONArray a = new JSONArray();
            for (int i = 0; i < locs.size(); i++) {
                Loc l = locs.get(i);
                JSONObject o = new JSONObject();
                o.put("id", l.id);
                o.put("name", l.name);
                o.put("region", l.region);
                o.put("group", l.group);
                o.put("sub", l.sub);
                o.put("pinned", l.pinned);
                a.put(o);
            }
            st.putS("locs", a.toString());
        } catch (Exception e) {
            // ignore
        }
    }

    synchronized ArrayList<Loc> list() {
        ArrayList<Loc> r = new ArrayList<Loc>(locs);
        Collections.sort(r, new Comparator<Loc>() {
            public int compare(Loc a, Loc b) {
                if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
                return a.id < b.id ? -1 : a.id > b.id ? 1 : 0;
            }
        });
        return r;
    }

    synchronized Loc byId(long id) {
        for (int i = 0; i < locs.size(); i++) if (locs.get(i).id == id) return locs.get(i);
        return null;
    }

    synchronized Loc add(String name, String region, String group, String sub) {
        Loc l = new Loc();
        long id = System.currentTimeMillis();
        while (byId(id) != null) id++;
        l.id = id; l.name = name; l.region = region; l.group = group; l.sub = sub;
        locs.add(l);
        saveLocs();
        return l;
    }

    synchronized void remove(long id) {
        Loc l = byId(id);
        if (l == null) return;
        locs.remove(l);
        cache.remove(id);
        st.remove("sch_" + id);
        st.remove("ntf_" + id);
        saveLocs();
    }

    synchronized void setPinned(long id, boolean p) {
        Loc l = byId(id);
        if (l != null) { l.pinned = p; saveLocs(); }
    }

    synchronized Schedule schedule(long id) {
        if (cache.containsKey(id)) return cache.get(id);
        Schedule s = Schedule.parse(st.getS("sch_" + id));
        cache.put(id, s);
        return s;
    }

    // ---------- загрузка ----------

    /** Синхронно обновляет все локации. Одинаковые регион/группа запрашиваются один раз. */
    ArrayList<Diff> refreshAllSync() {
        ArrayList<Diff> diffs = new ArrayList<Diff>();
        synchronized (refreshLock) {
            HashMap<String, Api.Result> got = new HashMap<String, Api.Result>();
            ArrayList<Loc> copy = list();
            for (int i = 0; i < copy.size(); i++) {
                Loc l = copy.get(i);
                String key = l.region + "/" + l.group + "/" + l.sub;
                Api.Result r = got.get(key);
                if (r == null) {
                    r = Api.fetch(l.region, l.group, l.sub);
                    got.put(key, r);
                }
                if (r.error) continue; // сеть/сервер недоступны: оставляем старые данные
                Schedule old = schedule(l.id);
                Schedule nw = r.code == 200 ? Schedule.parse(r.body) : null;
                if (nw != null) st.putS("sch_" + l.id, r.body);
                else st.remove("sch_" + l.id);
                synchronized (this) { cache.put(l.id, nw); }
                if (old != null && nw != null && !old.raw.trim().equals(nw.raw.trim())) {
                    Diff d = new Diff();
                    d.loc = l; d.old = old; d.nw = nw;
                    diffs.add(d);
                }
            }
            st.putL("lastRefresh", System.currentTimeMillis());
        }
        return diffs;
    }

    void refreshAsync() {
        if (busy) return;
        busy = true;
        new Thread() {
            public void run() {
                try { refreshAllSync(); } finally { busy = false; }
                fire();
            }
        }.start();
    }

    // ---------- слушатели (главный поток) ----------

    void addListener(Listener l) { synchronized (ls) { if (!ls.contains(l)) ls.add(l); } }
    void removeListener(Listener l) { synchronized (ls) { ls.remove(l); } }

    void fire() {
        Ui.H.post(new Runnable() {
            public void run() {
                ArrayList<Listener> copy;
                synchronized (ls) { copy = new ArrayList<Listener>(ls); }
                for (int i = 0; i < copy.size(); i++) copy.get(i).onData();
            }
        });
    }
}
