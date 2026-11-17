package ua.svitlo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Рисует SVG-путь (viewBox 24) без векторных ресурсов, работает на любой версии Android. */
final class IconView extends View {
    private final Path path;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint cut, line;
    private float rot;

    IconView(Context c, String d, int color) {
        this(c, d, color, false);
    }

    IconView(Context c, String d, int color, boolean evenOdd) {
        super(c);
        path = SvgPath.parse(d);
        if (evenOdd) path.setFillType(Path.FillType.EVEN_ODD);
        fill.setColor(color);
    }

    IconView rot(float deg) { rot = deg; return this; }

    /** Перечёркивание: «вырез» цветом фона и тонкая линия цветом иконки (как в макете). */
    IconView cross(int cutColor) {
        cut = new Paint(Paint.ANTI_ALIAS_FLAG);
        cut.setStyle(Paint.Style.STROKE); cut.setStrokeCap(Paint.Cap.ROUND);
        cut.setStrokeWidth(3.6f); cut.setColor(cutColor);
        line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStyle(Paint.Style.STROKE); line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeWidth(1.6f); line.setColor(fill.getColor());
        return this;
    }

    @Override
    protected void onDraw(Canvas cv) {
        float s = Math.min(getWidth(), getHeight()) / 24f;
        cv.save();
        cv.scale(s, s);
        if (rot != 0) cv.rotate(rot, 12, 12);
        cv.drawPath(path, fill);
        cv.restore();
        if (cut != null) {
            cv.save();
            cv.scale(s, s);
            cv.drawLine(3, 3, 21, 21, cut);
            cv.drawLine(3, 3, 21, 21, line);
            cv.restore();
        }
    }
}
