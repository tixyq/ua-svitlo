package ua.svitlo;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** «Виберіть групу»: список групп приходит с сервера одним запросом и кэшируется. */
final class GroupScreen extends Screen {
    private final Region region;
    private LinearLayout col;
    private TextView msg;
    private Groups.CB cb;
    private List<String> items = new ArrayList<String>();
    private boolean done, failed;

    GroupScreen(MainActivity a, Region region) { super(a); this.region = region; }

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

        msg = Ui.text(a, "", 34, Ui.TEXT2);
        msg.setGravity(Gravity.CENTER);
        f.addView(msg, new FrameLayout.LayoutParams(-1, -1));

        View back = Ui.circle(a, Icons.BACK, new View.OnClickListener() {
            public void onClick(View v) { a.pop(); }
        });
        View close = Ui.circle(a, Icons.CLOSE, new View.OnClickListener() {
            public void onClick(View v) { a.home(); }
        });
        f.addView(Ui.header(a, S.t("choose_group"), 146, back, close), Ui.fill(0, 0, 0, 132));
        return f;
    }

    @Override
    void onShow() {
        fill();
        cb = new Groups.CB() {
            public void on(List<String> list, boolean d, boolean err) {
                items = list; done = d; failed = err;
                fill();
            }
        };
        Groups.get(a, region.code, cb);
    }

    @Override
    void onHide() {
        Groups.cancel(cb);
        cb = null;
    }

    private void fill() {
        col.removeAllViews();
        if (items.isEmpty()) {
            msg.setText(done ? S.t(failed ? "net_error" : "no_data") : S.t("searching"));
            msg.setVisibility(View.VISIBLE);
            return;
        }
        msg.setVisibility(View.GONE);
        int n = items.size();
        for (int i = 0; i < n; i++) {
            final String it = items.get(i);
            TextView t = Ui.text(a, it, 38, Ui.TEXT);
            t.setPadding(Ui.px(40), 0, Ui.px(40), 0);
            float top = i == 0 ? 40 : 12, bottom = i == n - 1 ? 40 : 12;
            Ui.bg(t, Ui.press(Ui.CARD, top, top, bottom, bottom));
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    int dot = it.indexOf('.');
                    a.push(new NameScreen(a, region, it.substring(0, dot), it.substring(dot + 1)));
                }
            });
            col.addView(t, Ui.lin(26, i == 0 ? 0 : 4, 26, 0, Ui.MATCH, 112));
        }
    }
}
