package ua.svitlo;

import android.content.Context;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

/** «Група n.n»: название локации и кнопка «Зберегти». */
final class NameScreen extends Screen {
    private final Region region;
    private final String group, sub;
    private EditText et;

    NameScreen(MainActivity a, Region region, String group, String sub) {
        super(a);
        this.region = region; this.group = group; this.sub = sub;
    }

    @Override
    View build() {
        FrameLayout f = new FrameLayout(a);
        f.setBackgroundColor(Ui.BG);

        f.addView(Ui.text(a, S.t("name_label"), 30, Ui.TEXT2), Ui.fill(61, 132, 20, 40));

        FrameLayout field = new FrameLayout(a);
        Ui.bg(field, Ui.shape(Ui.CARD, 40));
        et = new EditText(a);
        Ui.bg(et, null);
        et.setSingleLine(true);
        et.setHint(S.t("name_hint"));
        et.setHintTextColor(Ui.DIM);
        et.setTextColor(Ui.TEXT);
        et.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 44 * Ui.unit);
        et.setGravity(Gravity.CENTER_VERTICAL);
        et.setPadding(Ui.px(35), 0, Ui.px(35), 0);
        et.setImeOptions(EditorInfo.IME_ACTION_DONE);
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        Compat.cursor(et, Ui.ACCENT);
        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int action, KeyEvent e) {
                save();
                return true;
            }
        });
        field.addView(et, new FrameLayout.LayoutParams(-1, -1));
        f.addView(field, Ui.fill(26, 184, 26, 140));

        TextView save = Ui.text(a, S.t("save"), 38, Ui.ON_ACCENT);
        save.setTypeface(Ui.medium());
        save.setGravity(Gravity.CENTER);
        Ui.bg(save, Ui.press(Ui.ACCENT, 40, 40, 40, 40));
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { save(); }
        });
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(Ui.px(220), Ui.px(126), Gravity.RIGHT | Gravity.BOTTOM);
        sp.rightMargin = Ui.px(26);
        sp.bottomMargin = Ui.px(41);
        f.addView(save, sp);

        View back = Ui.circle(a, Icons.BACK, new View.OnClickListener() {
            public void onClick(View v) { a.pop(); }
        });
        View close = Ui.circle(a, Icons.CLOSE, new View.OnClickListener() {
            public void onClick(View v) { a.home(); }
        });
        f.addView(Ui.header(a, S.t("group") + " " + group + "." + sub, 146, back, close), Ui.fill(0, 0, 0, 132));
        return f;
    }

    @Override
    void onShow() {
        et.requestFocus();
        Ui.H.postDelayed(new Runnable() {
            public void run() {
                InputMethodManager imm = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && et.isShown()) imm.showSoftInput(et, 0);
            }
        }, 200);
    }

    private void save() {
        String name = et.getText().toString().trim();
        if (name.length() == 0) name = region.name();
        a.repo.add(name, region.code, group, sub);
        a.repo.refreshAsync();
        Scheduler.schedule(a);
        a.home();
    }
}
