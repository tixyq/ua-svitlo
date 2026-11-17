package ua.svitlo;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

final class SettingsScreen extends Screen {
    private static final int[] BG_MIN = {5, 10, 15, 30, 60};
    private static final int[] ALERT_MIN = {0, 5, 10, 15, 30, 60}; // 0 = не предупреждать
    private static final String[] LANGS = {"system", "uk", "en"};
    private static final int ROWS = 5;

    private LinearLayout col;

    SettingsScreen(MainActivity a) { super(a); }

    @Override
    View build() {
        FrameLayout f = new FrameLayout(a);
        f.setBackgroundColor(Ui.BG);

        ScrollView sv = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 0, 0, Ui.px(40));
        sv.addView(col, new ViewGroup.LayoutParams(-1, -2));
        f.addView(sv, Ui.fill(0, 132, 0, Ui.MATCH));

        View back = Ui.circle(a, Icons.BACK, new View.OnClickListener() {
            public void onClick(View v) { a.pop(); }
        });
        f.addView(Ui.header(a, S.t("settings"), 146, back), Ui.fill(0, 0, 0, 132));
        return f;
    }

    @Override
    void refresh() {
        col.removeAllViews();
        final Store st = a.repo.st;

        col.addView(row(0, S.t("bg_update"), S.durLong(st.bgMin()), false, new View.OnClickListener() {
            public void onClick(View v) {
                Ui.pick(a, labels(BG_MIN), indexOf(BG_MIN, st.bgMin()), new Ui.Pick() {
                    public void pick(int i) { st.setBgMin(BG_MIN[i]); Scheduler.schedule(a); refresh(); }
                });
            }
        }));

        col.addView(row(1, S.t("off_title"), alertValue(st.offMin()), false, new View.OnClickListener() {
            public void onClick(View v) {
                Ui.pick(a, labels(ALERT_MIN), indexOf(ALERT_MIN, st.offMin()), new Ui.Pick() {
                    public void pick(int i) { st.setOffMin(ALERT_MIN[i]); Scheduler.schedule(a); refresh(); }
                });
            }
        }));
        col.addView(row(2, S.t("on_title"), alertValue(st.onMin()), false, new View.OnClickListener() {
            public void onClick(View v) {
                Ui.pick(a, labels(ALERT_MIN), indexOf(ALERT_MIN, st.onMin()), new Ui.Pick() {
                    public void pick(int i) { st.setOnMin(ALERT_MIN[i]); Scheduler.schedule(a); refresh(); }
                });
            }
        }));

        col.addView(row(3, S.t("notif_settings"), S.t("notif_sub"), true, new View.OnClickListener() {
            public void onClick(View v) { Compat.openNotifSettings(a); }
        }));

        col.addView(row(4, S.t("language"), langName(st.lang()), false, new View.OnClickListener() {
            public void onClick(View v) {
                String[] items = {S.t("lang_system"), S.t("lang_uk"), S.t("lang_en")};
                int cur = 0;
                for (int i = 0; i < LANGS.length; i++) if (LANGS[i].equals(st.lang())) cur = i;
                Ui.pick(a, items, cur, new Ui.Pick() {
                    public void pick(int i) {
                        st.setLang(LANGS[i]);
                        Launcher.sync(a); // название приложения в лаунчере
                        a.rebuild();
                    }
                });
            }
        }));
    }

    private static String alertValue(int min) {
        return min <= 0 ? S.t("off_label") : S.durLong(min);
    }

    private static String[] labels(int[] m) {
        String[] r = new String[m.length];
        for (int i = 0; i < r.length; i++) r[i] = m[i] == 0 ? S.t("off_label") : S.durLong(m[i]);
        return r;
    }

    private static int indexOf(int[] m, int v) {
        for (int i = 0; i < m.length; i++) if (m[i] == v) return i;
        return -1;
    }

    private static String langName(String l) {
        return S.t("uk".equals(l) ? "lang_uk" : "en".equals(l) ? "lang_en" : "lang_system");
    }

    /** Строка высотой 150: заголовок на 27, подпись на 80; углы 40 у первой/последней, 12 между. */
    private View row(int i, String label, String value, boolean ext, View.OnClickListener l) {
        float top = i == 0 ? 40 : 12, bottom = i == ROWS - 1 ? 40 : 12;
        FrameLayout r = new FrameLayout(a);
        Ui.bg(r, Ui.press(Ui.CARD, top, top, bottom, bottom));
        r.setOnClickListener(l);
        r.addView(Ui.text(a, label, 36, Ui.TEXT), Ui.fill(40, 27, ext ? 100 : 40, 46));
        r.addView(Ui.text(a, value, 30, Ui.TEXT2), Ui.fill(40, 80, ext ? 100 : 40, 40));
        if (ext) r.addView(new IconView(a, Icons.EXT, Ui.ICON2), Ui.atR(42, 53, 44, 44));
        r.setLayoutParams(Ui.lin(26, i == 0 ? 0 : 4, 26, 0, Ui.MATCH, 150));
        return r;
    }
}
