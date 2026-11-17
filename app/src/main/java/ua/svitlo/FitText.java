package ua.svitlo;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.TextView;

/** Однострочный текст, который сам уменьшает шрифт, чтобы поместиться целиком (многоточие только в крайнем случае). */
final class FitText extends TextView {
    private float base;

    FitText(Context c) { super(c); }

    @Override
    public void setTextSize(int unit, float size) {
        super.setTextSize(unit, size);
        base = getTextSize();
    }

    @Override
    public void setText(CharSequence t, BufferType type) {
        super.setText(t, type);
        if (getWidth() > 0) fit(getWidth());
    }

    @Override
    public void setTypeface(Typeface tf) {
        super.setTypeface(tf);
        if (getWidth() > 0) fit(getWidth());
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        fit(w);
    }

    private void fit(int w) {
        int avail = w - getPaddingLeft() - getPaddingRight();
        CharSequence t = getText();
        if (avail <= 0 || base <= 0 || t == null || t.length() == 0) return;
        Paint p = new Paint(getPaint());
        float s = base;
        p.setTextSize(s);
        while (s > base * 0.5f && p.measureText(t, 0, t.length()) > avail) {
            s -= base * 0.03f;
            p.setTextSize(s);
        }
        if (Math.abs(s - getTextSize()) > 0.5f) super.setTextSize(TypedValue.COMPLEX_UNIT_PX, s);
    }
}
