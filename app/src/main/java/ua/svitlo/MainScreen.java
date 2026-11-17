package ua.svitlo;

import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

final class MainScreen extends Screen {
    private LinearLayout list;
    private TextView empty;

    MainScreen(MainActivity a) { super(a); }

    @Override
    View build() {
        FrameLayout f = new FrameLayout(a);
        f.setBackgroundColor(Ui.BG);

        ScrollView sv = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 0, 0, Ui.px(40));
        sv.addView(list, new ViewGroup.LayoutParams(-1, -2));
        f.addView(sv, Ui.fill(0, 132, 0, Ui.MATCH));

        empty = Ui.text(a, S.t("empty"), 34, Ui.TEXT2);
        empty.setGravity(Gravity.CENTER);
        f.addView(empty, new FrameLayout.LayoutParams(-1, -1));

        View gear = Ui.circle(a, Icons.GEAR, new View.OnClickListener() {
            public void onClick(View v) { a.push(new SettingsScreen(a)); }
        });
        View plus = Ui.circle(a, Icons.PLUS, new View.OnClickListener() {
            public void onClick(View v) { a.push(new RegionScreen(a)); }
        });
        FrameLayout h = Ui.header(a, S.t("title"), 27, null, gear, plus);
        // 10 быстрых тапов по названию: пробные уведомления со всеми тремя звуками
        View title = h.getChildAt(h.getChildCount() - 1);
        // Не click-listener: вид остаётся «некликабельным» (без звука нажатия, вибрации, подсветки и подсказок TalkBack).
        title.setSoundEffectsEnabled(false);
        title.setHapticFeedbackEnabled(false);
        title.setOnTouchListener(new View.OnTouchListener() {
            private int n;
            private long last;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    long now = System.currentTimeMillis();
                    n = now - last > 2000 ? 1 : n + 1;
                    last = now;
                    if (n >= 10) { n = 0; testSounds(); }
                }
                return true;
            }
        });
        f.addView(h, Ui.fill(0, 0, 0, 132));
        return f;
    }

    /** По порядку: нейтральное, «увімкнуть» (весёлое), «вимкнуть» (грустное); пауза, чтобы каждый звук доиграл. */
    private void testSounds() {
        final Context c = a.getApplicationContext();
        final String t = S.t("title");
        final String[] txt = {
                S.t("changed") + " " + S.t("today") + ": " + S.t("no_outages_short"),
                S.t("will_on") + " " + S.durLong(15),
                S.t("will_off") + " " + S.durLong(15)};
        final int[] kind = {Compat.K_NEUTRAL, Compat.K_ON, Compat.K_OFF};
        for (int i = 0; i < 3; i++) {
            final int k = i;
            Ui.H.postDelayed(new Runnable() {
                public void run() { Compat.post(c, 2100000001 + k, kind[k], t, txt[k]); }
            }, k * 1500L);
        }
    }

    @Override
    void refresh() {
        list.removeAllViews();
        ArrayList<Repo.Loc> ls = a.repo.list();
        empty.setVisibility(ls.isEmpty() ? View.VISIBLE : View.GONE);
        long now = System.currentTimeMillis();
        for (int i = 0; i < ls.size(); i++) list.addView(card(ls.get(i), now));
    }

    private View card(final Repo.Loc l, long now) {
        // 0 – нет данных, 1 – таймер до смены, 2 – одна строка
        int mode = 0;
        String lead = null, time = null, simple = null;
        Schedule s = a.repo.schedule(l.id);
        if (s != null) {
            int st = s.stateAt(now);
            Schedule.Change nc = s.nextChange(now);
            if (st != 0) {
                if (nc != null) {
                    mode = 1;
                    lead = S.t(st == 1 ? "until_off" : "until_on");
                    time = S.durLong((nc.time - now + 59999) / 60000);
                } else {
                    mode = 2;
                    simple = S.t(st == 1 ? "no_outage" : "off_rest");
                }
            }
        }

        FrameLayout c = new FrameLayout(a);
        Ui.bg(c, Ui.press(Ui.CARD, 40, 40, 40, 40));
        c.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.push(new LocationScreen(a, l.id)); }
        });
        c.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) { menu(l); return true; }
        });

        // плитка с пином и название
        FrameLayout tile = new FrameLayout(a);
        Ui.bg(tile, Ui.shape(Ui.ACCENT, 26));
        c.addView(tile, Ui.at(35, 34, 124, 124));
        c.addView(new IconView(a, Icons.PIN, Ui.ON_ACCENT, true), Ui.at(65, 64, 64, 64));
        c.addView(Ui.text(a, l.name, 57, Ui.TEXT), Ui.fill(190, 58, l.pinned ? 96 : 35, 80));
        if (l.pinned) c.addView(new IconView(a, Icons.PUSHPIN, Ui.ICON2).rot(45), Ui.at(630, 73, 50, 50));

        if (mode == 1) {
            c.addView(Ui.text(a, lead, 30, Ui.TEXT2), Ui.fill(35, 189, 35, 40));
            c.addView(new IconView(a, Icons.BOLT, Ui.TEXT2).cross(Ui.CARD), Ui.at(34, 237, 42, 42));
            TextView t = Ui.text(a, time, 40, Ui.ICON);
            t.setTypeface(Ui.semibold());
            c.addView(t, Ui.fill(87, 233, 35, 50));
        } else if (mode == 2) {
            c.addView(new IconView(a, Icons.BOLT, Ui.TEXT2), Ui.at(36, 188, 40, 40));
            c.addView(Ui.text(a, simple, 30, Ui.TEXT2), Ui.fill(79, 188, 35, 40));
        } else {
            TextView t = Ui.text(a, S.t("no_data"), 30, Ui.TEXT2);
            t.setGravity(Gravity.CENTER);
            c.addView(t, Ui.fill(0, 188, 0, 40));
        }

        c.setLayoutParams(Ui.lin(26, 0, 26, 28, Ui.MATCH, mode == 1 ? 316 : 262));
        return c;
    }

    private void menu(final Repo.Loc l) {
        String[] items = {S.t(l.pinned ? "unpin" : "pin"), S.t("delete")};
        Ui.pick(a, items, -1, new Ui.Pick() {
            public void pick(int i) {
                if (i == 0) a.repo.setPinned(l.id, !l.pinned);
                else a.repo.remove(l.id);
                Scheduler.schedule(a);
                refresh();
            }
        });
    }
}
