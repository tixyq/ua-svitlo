package ua.svitlo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

/** Тонкая обёртка над SharedPreferences. */
final class Store {
    static Store I;
    private final SharedPreferences sp;

    static synchronized Store get(Context c) {
        if (I == null) I = new Store(c.getApplicationContext());
        return I;
    }

    private Store(Context c) {
        sp = c.getSharedPreferences("svitlo", 0);
    }

    private static void save(SharedPreferences.Editor e) {
        if (Build.VERSION.SDK_INT >= 9) e.apply(); else e.commit();
    }

    String getS(String k) { return sp.getString(k, null); }
    void putS(String k, String v) { save(sp.edit().putString(k, v)); }
    long getL(String k, long d) { return sp.getLong(k, d); }
    void putL(String k, long v) { save(sp.edit().putLong(k, v)); }
    void remove(String k) { save(sp.edit().remove(k)); }

    int bgMin() { return sp.getInt("bgMin", 5); }
    void setBgMin(int v) { save(sp.edit().putInt("bgMin", v)); }
    /** За сколько минут предупреждать о вимкненні / увімкненні (0 – не предупреждать). */
    int offMin() { return sp.getInt("offMin", 15); }
    void setOffMin(int v) { save(sp.edit().putInt("offMin", v)); }
    int onMin() { return sp.getInt("onMin", 15); }
    void setOnMin(int v) { save(sp.edit().putInt("onMin", v)); }
    String lang() { return sp.getString("lang", "system"); }
    void setLang(String v) { save(sp.edit().putString("lang", v)); }
    boolean firstRunDone() { return sp.getBoolean("firstRun", false); }
    void setFirstRunDone() { save(sp.edit().putBoolean("firstRun", true)); }
    long lastRefresh() { return sp.getLong("lastRefresh", 0); }
}
