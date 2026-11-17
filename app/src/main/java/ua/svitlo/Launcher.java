package ua.svitlo;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Название под иконкой зависит от языка приложения: в манифесте три алиаса лаунчера
 * (системный, украинский, английский), включён всегда ровно один.
 */
final class Launcher {
    private Launcher() {}

    static void sync(Context c) {
        try {
            doSync(c);
        } catch (Throwable t) {
            // остаётся как есть: название под иконкой не критично
        }
    }

    private static void doSync(Context c) {
        String l = Store.get(c).lang();
        boolean uk = "uk".equals(l), en = "en".equals(l);
        boolean[] want = {!uk && !en, uk, en};
        String[] names = {"AliasSystem", "AliasUk", "AliasEn"};
        boolean[] def = {true, false, false};
        // сначала включаем нужный, потом выключаем остальные: иконка не пропадает
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < names.length; i++) {
                if (want[i] == (pass == 0)) set(c, names[i], want[i], def[i]);
            }
        }
    }

    private static void set(Context c, String name, boolean want, boolean defEnabled) {
        PackageManager pm = c.getPackageManager();
        ComponentName cn = new ComponentName(c.getPackageName(), c.getPackageName() + "." + name);
        int s = pm.getComponentEnabledSetting(cn);
        boolean cur = s == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                || (s == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && defEnabled);
        if (cur != want) {
            pm.setComponentEnabledSetting(cn, want ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }
}
