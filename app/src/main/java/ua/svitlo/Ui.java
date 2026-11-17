package ua.svitlo;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Помощники для вёрстки «1:1 с макетом». Макет нарисован на холсте шириной 768:
 * 1 единица макета = ширина_экрана/768 пикселей, и для размеров, и для шрифтов.
 * Так интерфейс выглядит как макет на любом телефоне, независимо от плотности, масштаба экрана и шрифта.
 */
final class Ui {
    private Ui() {}

    static final Handler H = new Handler(Looper.getMainLooper());
    static float unit = 1.4f;

    static final int BG = 0xFF000000, CARD = 0xFF1C1B1F, BTN = 0xFF1F1F23, ACCENT = 0xFFF27F0C,
            ON_ACCENT = 0xFFFDEBD9, TEXT = 0xFFE6E1E9, TEXT2 = 0xFFB9B5C9, ICON = 0xFFF2EFF7,
            ICON2 = 0xFFA6A3AD, PILL = 0xFF2B2B30, PILL_TXT = 0xFFA9A6B4, ORANGE = 0xFFE8772A,
            ROW = 0xFFC9C5D8, DIM = 0xFF6E6C76, BAND = 0xFF2F2F32, CHIP = 0xFF232327,
            REGION = 0xFF8F8C9B, NAME = 0xFFDCD8E2;

    static final int MATCH = -1, WRAP = -2;

    static void init(Context c) { unit = c.getResources().getDisplayMetrics().widthPixels / 768f; }

    static int px(float d) { return Math.round(d * unit); }

    private static int dim(float v) { return v == MATCH ? MATCH : v == WRAP ? WRAP : px(v); }

    // ---------- фоны ----------

    static GradientDrawable shape(int color, float tl, float tr, float br, float bl) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        float a = px(tl), b = px(tr), c = px(br), d = px(bl);
        g.setCornerRadii(new float[]{a, a, b, b, c, c, d, d});
        return g;
    }

    static GradientDrawable shape(int color, float r) { return shape(color, r, r, r, r); }

    private static int lighten(int c) {
        int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
        r += (255 - r) * 12 / 100; g += (255 - g) * 12 / 100; b += (255 - b) * 12 / 100;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    static Drawable press(int color, float tl, float tr, float br, float bl) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, shape(lighten(color), tl, tr, br, bl));
        s.addState(new int[]{}, shape(color, tl, tr, br, bl));
        return s;
    }

    @SuppressWarnings("deprecation")
    static void bg(View v, Drawable d) { v.setBackgroundDrawable(d); }

    // ---------- текст ----------

    static Typeface medium() {
        return Build.VERSION.SDK_INT >= 16 ? Typeface.create("sans-serif-medium", Typeface.NORMAL) : Typeface.DEFAULT;
    }

    static Typeface semibold() { return Typeface.DEFAULT_BOLD; }

    static TextView text(Context c, String s, float size, int color) {
        TextView t = new FitText(c);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * unit);
        t.setText(s);
        t.setTextColor(color);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setIncludeFontPadding(false);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    // ---------- позиционирование в FrameLayout (координаты макета) ----------

    static FrameLayout.LayoutParams at(float l, float t, float w, float h) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dim(w), dim(h));
        p.leftMargin = px(l); p.topMargin = px(t); p.gravity = Gravity.LEFT | Gravity.TOP;
        return p;
    }

    static FrameLayout.LayoutParams atR(float r, float t, float w, float h) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dim(w), dim(h));
        p.rightMargin = px(r); p.topMargin = px(t); p.gravity = Gravity.RIGHT | Gravity.TOP;
        return p;
    }

    /** Во всю ширину с отступами слева/справа. */
    static FrameLayout.LayoutParams fill(float l, float t, float r, float h) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(MATCH, dim(h));
        p.leftMargin = px(l); p.rightMargin = px(r); p.topMargin = px(t); p.gravity = Gravity.LEFT | Gravity.TOP;
        return p;
    }

    static LinearLayout.LayoutParams lin(float l, float t, float r, float b, float w, float h) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dim(w), dim(h));
        p.leftMargin = px(l); p.topMargin = px(t); p.rightMargin = px(r); p.bottomMargin = px(b);
        return p;
    }

    // ---------- шапка ----------

    static View circle(Context c, String icon, View.OnClickListener l) {
        FrameLayout f = new FrameLayout(c);
        bg(f, press(BTN, 48, 48, 48, 48));
        f.setOnClickListener(l);
        f.addView(new IconView(c, icon, ICON), new FrameLayout.LayoutParams(px(56), px(56), Gravity.CENTER));
        return f;
    }

    /** Высота шапки в макете: 132. Кнопки справа перечисляются от крайней правой. */
    static FrameLayout header(Context c, String title, float titleLeft, View left, View... rights) {
        FrameLayout h = new FrameLayout(c);
        if (left != null) h.addView(left, at(26, 15, 96, 96));
        float rr = 26;
        for (int i = 0; i < rights.length; i++) {
            h.addView(rights[i], atR(rr, 15, 96, 96));
            rr += 112;
        }
        if (title.length() > 0) {
            h.addView(text(c, title, 52, TEXT), fill(titleLeft, 22, rights.length > 0 ? rr - 8 : 26, 76));
        }
        return h;
    }

    // ---------- диалог выбора ----------

    interface Pick { void pick(int index); }

    static void pick(Context c, String[] items, int sel, final Pick cb) {
        final Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        bg(box, shape(CARD, 40));
        int p = px(16);
        box.setPadding(p, p, p, p);
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            TextView t = text(c, items[i], 38, i == sel ? ACCENT : TEXT);
            t.setPadding(px(36), 0, px(36), 0);
            bg(t, press(CARD, 28, 28, 28, 28));
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { d.dismiss(); cb.pick(idx); }
            });
            box.addView(t, new LinearLayout.LayoutParams(MATCH, px(112)));
        }
        d.setContentView(box, new ViewGroup.LayoutParams(px(560), WRAP));
        d.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        d.getWindow().setLayout(px(560), WRAP);
        d.show();
    }
}
