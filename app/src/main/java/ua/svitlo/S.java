package ua.svitlo;

import java.util.HashMap;
import java.util.Locale;

/** Строки интерфейса (uk/en) без ресурсов: язык переключается на лету. */
final class S {
    private static final HashMap<String, String[]> M = new HashMap<String, String[]>();

    private static void p(String k, String uk, String en) {
        M.put(k, new String[]{uk, en});
    }

    static {
        p("title", "Світло?", "Svitlo?");
        p("until_off", "До відключення електроенергії:", "Until power outage:");
        p("until_on", "До увімкнення електроенергії:", "Until power is back:");
        p("no_outage", "Відключень немає до кінця дня", "No outages until end of day");
        p("off_rest", "Світла немає до кінця дня", "No power until end of day");
        p("no_data", "Даних немає", "No data");
        p("empty", "Натисніть +, щоб додати локацію", "Tap + to add a location");
        p("pin", "Закріпити локацію", "Pin location");
        p("unpin", "Відкріпити локацію", "Unpin location");
        p("delete", "Видалити локацію", "Delete location");
        p("choose_region", "Виберіть регіон", "Choose region");
        p("choose_group", "Виберіть групу", "Choose group");
        p("searching", "Пошук груп…", "Searching groups…");
        p("net_error", "Помилка мережі", "Network error");
        p("group", "Група", "Group");
        p("name_label", "Вкажіть назву локації", "Enter location name");
        p("name_hint", "Мій дім", "My home");
        p("save", "Зберегти", "Save");
        p("settings", "Налаштування", "Settings");
        p("bg_update", "Оновлювати розклад у фоні кожні", "Update schedule in background every");
        p("off_title", "Попереджати про вимкнення світла за", "Warn before power goes off by");
        p("on_title", "Попереджати про увімкнення світла за", "Warn before power comes back by");
        p("off_label", "Вимкнено", "Off");
        p("notif_settings", "Налаштування сповіщень", "Notification settings");
        p("notif_sub", "Системні налаштування застосунку", "App system settings");
        p("language", "Мова", "Language");
        p("lang_system", "Системна", "System");
        p("lang_uk", "Українська", "Українська");
        p("lang_en", "English", "English");
        p("will_off", "Світло вимкнуть через", "Power will go off in");
        p("will_on", "Світло увімкнуть через", "Power will be back in");
        p("changed", "Графік змінено:", "Schedule changed:");
        p("today", "Сьогодні", "Today");
        p("tomorrow", "Завтра", "Tomorrow");
        p("no_outages_short", "відключень немає", "no outages");
        p("ch_on", "Попередження про увімкнення", "Power-on alerts");
        p("ch_off", "Попередження про вимкнення", "Power-off alerts");
        p("ch_changes", "Зміни розкладу", "Schedule changes");
    }

    static boolean uk() {
        Store s = Store.I;
        String l = s == null ? "system" : s.lang();
        if ("uk".equals(l)) return true;
        if ("en".equals(l)) return false;
        return "uk".equals(Locale.getDefault().getLanguage());
    }

    static String t(String k) {
        String[] v = M.get(k);
        return v == null ? k : v[uk() ? 0 : 1];
    }

    /** «3 год 37 хв» */
    static String durLong(long min) {
        if (min < 0) min = 0;
        long h = min / 60, m = min % 60;
        String H = uk() ? "год" : "h", Mn = uk() ? "хв" : "min";
        if (h == 0) return m + " " + Mn;
        if (m == 0) return h + " " + H;
        return h + " " + H + " " + m + " " + Mn;
    }

    /** «3г 30хв» */
    static String durShort(int min) {
        int h = min / 60, m = min % 60;
        String H = uk() ? "г" : "h", Mn = uk() ? "хв" : "m";
        if (h == 0) return m + Mn;
        if (m == 0) return h + H;
        return h + H + " " + m + Mn;
    }

    static String[] days() {
        return uk() ? new String[]{"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд"}
                : new String[]{"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
    }
}
