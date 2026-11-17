package ua.svitlo;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** «Виберіть регіон»: список областей, у которых есть данные. */
final class RegionScreen extends Screen {
    RegionScreen(MainActivity a) { super(a); }

    @Override
    View build() {
        FrameLayout f = new FrameLayout(a);
        f.setBackgroundColor(Ui.BG);

        ScrollView sv = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 0, 0, Ui.px(40));
        sv.addView(col, new ViewGroup.LayoutParams(-1, -2));
        f.addView(sv, Ui.fill(0, 132, 0, Ui.MATCH));

        int n = Region.ALL.length;
        for (int i = 0; i < n; i++) {
            final Region r = Region.ALL[i];
            TextView t = Ui.text(a, r.name(), 38, Ui.TEXT);
            t.setPadding(Ui.px(40), 0, Ui.px(40), 0);
            float top = i == 0 ? 40 : 12, bottom = i == n - 1 ? 40 : 12;
            Ui.bg(t, Ui.press(Ui.CARD, top, top, bottom, bottom));
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { a.push(new GroupScreen(a, r)); }
            });
            col.addView(t, Ui.lin(26, i == 0 ? 0 : 4, 26, 0, Ui.MATCH, 112));
        }

        View close = Ui.circle(a, Icons.CLOSE, new View.OnClickListener() {
            public void onClick(View v) { a.home(); }
        });
        f.addView(Ui.header(a, S.t("choose_region"), 27, null, close), Ui.fill(0, 0, 0, 132));
        return f;
    }
}
