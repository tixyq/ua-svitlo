package ua.svitlo;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

import java.util.Calendar;

final class LocationScreen extends Screen {
    private final long id;
    private int sel = -1; // выбранный день недели (0 = Пн); -1 – сегодня
    private LinearLayout box;
    private ScrollView scroll;
    private boolean scrolled;

    LocationScreen(MainActivity a, long id) { super(a); this.id = id; }

    @Override
    View build() {
        Repo.Loc l = a.repo.byId(id);
        FrameLayout f = new FrameLayout(a);
        f.setBackgroundColor(Ui.BG);

        ScrollView sv = scroll = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 0, 0, Ui.px(25));
        sv.addView(col, new ViewGroup.LayoutParams(-1, -2));
        f.addView(sv, Ui.fill(0, 132, 0, Ui.MATCH));

        // верхний блок: плитка, название, область, чип группы (y = 132…404 макета)
        FrameLayout top = new FrameLayout(a);
        FrameLayout tile = new FrameLayout(a);
        Ui.bg(tile, Ui.shape(Ui.ACCENT, 26));
        top.addView(tile, Ui.at(36, 0, 119, 119));
        top.addView(new IconView(a, Icons.PIN, Ui.ON_ACCENT, true), Ui.at(63, 27, 64, 64));
        TextView name = Ui.text(a, l == null ? "" : l.name, 57, Ui.NAME);
        name.setTypeface(Ui.medium());
        top.addView(name, Ui.fill(195, 12, 26, 80));
        top.addView(Ui.text(a, l == null ? "" : Region.byCode(l.region).full(), 30, Ui.REGION), Ui.fill(195, 100, 26, 40));
        TextView chip = Ui.text(a, S.t("group") + " " + (l == null ? "" : l.label()), 30, Ui.TEXT2);
        chip.setGravity(Gravity.CENTER);
        chip.setMinWidth(Ui.px(191));
        chip.setPadding(Ui.px(28), 0, Ui.px(28), 0);
        Ui.bg(chip, Ui.shape(Ui.CHIP, 36));
        FrameLayout.LayoutParams cp = Ui.at(195, 154, Ui.WRAP, 72);
        top.addView(chip, cp);
        col.addView(top, new LinearLayout.LayoutParams(-1, Ui.px(272)));

        // карточка графика
        box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        Ui.bg(box, Ui.shape(Ui.CARD, 40));
        col.addView(box, Ui.lin(36, 0, 35, 0, Ui.MATCH, Ui.WRAP));

        // удалить
        FrameLayout del = new FrameLayout(a);
        Ui.bg(del, Ui.press(Ui.CARD, 40, 40, 40, 40));
        del.addView(new IconView(a, Icons.TRASH, Ui.ICON2), Ui.at(51, 39, 52, 52));
        del.addView(Ui.text(a, S.t("delete"), 38, Ui.TEXT), Ui.fill(130, 38, 20, 50));
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                a.repo.remove(id);
                Scheduler.schedule(a);
                a.pop();
            }
        });
        col.addView(del, Ui.lin(36, 41, 35, 0, Ui.MATCH, 126));

        f.addView(Ui.header(a, "", 0, Ui.circle(a, Icons.BACK, new View.OnClickListener() {
            public void onClick(View v) { a.pop(); }
        })), Ui.fill(0, 0, 0, 132));
        return f;
    }

    @Override
    void refresh() {
        if (a.repo.byId(id) == null) { a.pop(); return; }
        render();
    }

    private TextView msg(String s, float h) {
        TextView t = Ui.text(a, s, 30, Ui.TEXT2);
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams(-1, Ui.px(h)));
        return t;
    }

    private void render() {
        box.removeAllViews();
        long now = System.currentTimeMillis();
        Schedule s = a.repo.schedule(id);
        if (s == null) { box.addView(msg(S.t("no_data"), 300)); return; }

        int todayIdx = (Schedule.cal(now).get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int cur = sel < 0 ? todayIdx : sel;

        // кнопки дней недели
        LinearLayout pills = new LinearLayout(a);
        pills.setPadding(Ui.px(38), 0, Ui.px(38), 0);
        String[] dn = S.days();
        for (int i = 0; i < 7; i++) {
            final int idx = i;
            boolean on = i == cur;
            TextView t = Ui.text(a, dn[i], 34, on ? Ui.ON_ACCENT : Ui.PILL_TXT);
            t.setGravity(Gravity.CENTER);
            if (on) t.setTypeface(Ui.medium());
            Ui.bg(t, Ui.shape(on ? Ui.ACCENT : Ui.PILL, 39));
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { sel = idx; render(); }
            });
            pills.addView(t, new LinearLayout.LayoutParams(Ui.px(78), Ui.px(78)));
            if (i < 6) pills.addView(new View(a), new LinearLayout.LayoutParams(0, 1, 1f));
        }
        box.addView(pills, Ui.lin(0, 41, 0, 0, Ui.MATCH, 78));

        int di = s.dateIndex(Schedule.dateOfOffset(now, cur - todayIdx));
        if (di < 0 || !s.hasData(di)) {
            box.addView(msg(S.t("no_data"), 250));
            return;
        }

        final LinearLayout rows = new LinearLayout(a);
        rows.setOrientation(LinearLayout.VERTICAL);
        View curRow = null;
        ArrayList<Schedule.Interval> ivs = s.intervals(di);
        for (int i = 0; i < ivs.size(); i++) {
            Schedule.Interval iv = ivs.get(i);
            View r = row(s, di, iv, now);
            rows.addView(r);
            long st = s.millis(di, iv.s);
            if (now >= st && now < st + (iv.e - iv.s) * 1800000L) curRow = r;
        }
        box.addView(rows, Ui.lin(0, 55, 0, 36, Ui.MATCH, Ui.WRAP));

        // сегодня: при первом показе прокручиваем к текущему интервалу
        if (curRow != null && !scrolled) {
            scrolled = true;
            final View cr = curRow;
            scroll.post(new Runnable() {
                public void run() {
                    scroll.scrollTo(0, Math.max(0, box.getTop() + rows.getTop() + cr.getTop() - Ui.px(420)));
                }
            });
        }
    }

    /** Интервал: значок (если света нет), время и длительность; колонки одинаковы во всех строках. */
    private View row(Schedule s, int di, Schedule.Interval iv, long now) {
        long st = s.millis(di, iv.s), en = st + (iv.e - iv.s) * 1800000L;
        boolean current = now >= st && now < en, past = now >= en;

        FrameLayout r = new FrameLayout(a);
        r.setLayoutParams(new LinearLayout.LayoutParams(-1, Ui.px(93)));
        if (current) {
            View band = new View(a);
            band.setBackgroundColor(Ui.BAND);
            r.addView(band, Ui.fill(0, 0, 0, 92));
        }
        if (!iv.on) {
            r.addView(new IconView(a, Icons.BOLT, past ? Ui.DIM : Ui.TEXT2).cross(current ? Ui.BAND : Ui.CARD),
                    Ui.at(42, 29, 34, 34));
        }
        int tc = past ? Ui.DIM : iv.on ? Ui.ORANGE : Ui.ROW;
        TextView time = Ui.text(a, Schedule.slotTime(iv.s) + " - " + Schedule.slotTime(iv.e), 38, tc);
        time.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        r.addView(time, Ui.fill(232, 24, 190, 44));
        TextView dur = Ui.text(a, S.durShort((iv.e - iv.s) * 30), 38, past ? Ui.DIM : Ui.ROW);
        dur.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        r.addView(dur, Ui.atR(40, 24, Ui.WRAP, 44));
        return r;
    }
}
