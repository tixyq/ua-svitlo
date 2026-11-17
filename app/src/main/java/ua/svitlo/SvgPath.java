package ua.svitlo;

import android.graphics.Path;

/** Минимальный разбор SVG path data (M L H V C S Z, a/A, относительные команды) в android.graphics.Path. */
final class SvgPath {
    private final String d;
    private int i;

    private SvgPath(String d) { this.d = d; }

    static Path parse(String d) {
        return new SvgPath(d).run();
    }

    private boolean hasNum() {
        skip();
        if (i >= d.length()) return false;
        char c = d.charAt(i);
        return c == '-' || c == '+' || c == '.' || (c >= '0' && c <= '9');
    }

    private void skip() {
        while (i < d.length() && (d.charAt(i) == ' ' || d.charAt(i) == ',' || d.charAt(i) == '\n' || d.charAt(i) == '\t')) i++;
    }

    private float num() {
        skip();
        int s = i;
        if (i < d.length() && (d.charAt(i) == '-' || d.charAt(i) == '+')) i++;
        boolean dot = false;
        while (i < d.length()) {
            char c = d.charAt(i);
            if (c >= '0' && c <= '9') i++;
            else if (c == '.' && !dot) { dot = true; i++; }
            else break;
        }
        return Float.parseFloat(d.substring(s, i));
    }

    private Path run() {
        Path p = new Path();
        float x = 0, y = 0, sx = 0, sy = 0, lcx = 0, lcy = 0;
        char cmd = 'M';
        boolean prevCubic = false;
        while (true) {
            skip();
            if (i >= d.length()) break;
            char c = d.charAt(i);
            if (Character.isLetter(c)) { cmd = c; i++; }
            else if (cmd == 'M') cmd = 'L';
            else if (cmd == 'm') cmd = 'l';
            boolean rel = Character.isLowerCase(cmd);
            char u = Character.toUpperCase(cmd);
            boolean cubic = false;
            if (u == 'Z') {
                p.close();
                x = sx; y = sy;
            } else if (u == 'M') {
                float a = num(), b = num();
                if (rel) { a += x; b += y; }
                p.moveTo(a, b); x = sx = a; y = sy = b;
            } else if (u == 'L') {
                float a = num(), b = num();
                if (rel) { a += x; b += y; }
                p.lineTo(a, b); x = a; y = b;
            } else if (u == 'H') {
                float a = num();
                if (rel) a += x;
                p.lineTo(a, y); x = a;
            } else if (u == 'V') {
                float b = num();
                if (rel) b += y;
                p.lineTo(x, b); y = b;
            } else if (u == 'C') {
                float x1 = num(), y1 = num(), x2 = num(), y2 = num(), a = num(), b = num();
                if (rel) { x1 += x; y1 += y; x2 += x; y2 += y; a += x; b += y; }
                p.cubicTo(x1, y1, x2, y2, a, b);
                lcx = x2; lcy = y2; x = a; y = b; cubic = true;
            } else if (u == 'S') {
                float x2 = num(), y2 = num(), a = num(), b = num();
                if (rel) { x2 += x; y2 += y; a += x; b += y; }
                float x1 = prevCubic ? 2 * x - lcx : x, y1 = prevCubic ? 2 * y - lcy : y;
                p.cubicTo(x1, y1, x2, y2, a, b);
                lcx = x2; lcy = y2; x = a; y = b; cubic = true;
            } else if (u == 'A') {
                float rx = num(), ry = num(), rot = num(), large = num(), sweep = num(), a = num(), b = num();
                if (rel) { a += x; b += y; }
                arc(p, x, y, rx, ry, rot, large != 0, sweep != 0, a, b);
                x = a; y = b;
            } else {
                break; // неподдерживаемая команда
            }
            prevCubic = cubic;
        }
        return p;
    }

    private static void arc(Path p, float x0, float y0, float rx, float ry, float rotDeg,
                            boolean large, boolean sweep, float x, float y) {
        if (rx == 0 || ry == 0 || (x0 == x && y0 == y)) { p.lineTo(x, y); return; }
        double rX = Math.abs(rx), rY = Math.abs(ry);
        double phi = Math.toRadians(rotDeg), cos = Math.cos(phi), sin = Math.sin(phi);
        double dx = (x0 - x) / 2.0, dy = (y0 - y) / 2.0;
        double x1 = cos * dx + sin * dy, y1 = -sin * dx + cos * dy;
        double lam = (x1 * x1) / (rX * rX) + (y1 * y1) / (rY * rY);
        if (lam > 1) { double s = Math.sqrt(lam); rX *= s; rY *= s; }
        double num = rX * rX * rY * rY - rX * rX * y1 * y1 - rY * rY * x1 * x1;
        double den = rX * rX * y1 * y1 + rY * rY * x1 * x1;
        double co = den == 0 ? 0 : (large == sweep ? -1 : 1) * Math.sqrt(Math.max(0, num / den));
        double cxp = co * rX * y1 / rY, cyp = -co * rY * x1 / rX;
        double cx = cos * cxp - sin * cyp + (x0 + x) / 2.0, cy = sin * cxp + cos * cyp + (y0 + y) / 2.0;
        double th1 = Math.atan2((y1 - cyp) / rY, (x1 - cxp) / rX);
        double th2 = Math.atan2((-y1 - cyp) / rY, (-x1 - cxp) / rX);
        double dth = th2 - th1;
        if (sweep && dth < 0) dth += 2 * Math.PI;
        else if (!sweep && dth > 0) dth -= 2 * Math.PI;
        int n = (int) Math.ceil(Math.abs(dth) / (Math.PI / 2));
        double step = dth / n, t = 4.0 / 3.0 * Math.tan(step / 4), a = th1;
        for (int k = 0; k < n; k++) {
            double a2 = a + step;
            double c1x = Math.cos(a) - t * Math.sin(a), c1y = Math.sin(a) + t * Math.cos(a);
            double c2x = Math.cos(a2) + t * Math.sin(a2), c2y = Math.sin(a2) - t * Math.cos(a2);
            double ex = Math.cos(a2), ey = Math.sin(a2);
            p.cubicTo(
                (float) (cos * c1x * rX - sin * c1y * rY + cx), (float) (sin * c1x * rX + cos * c1y * rY + cy),
                (float) (cos * c2x * rX - sin * c2y * rY + cx), (float) (sin * c2x * rX + cos * c2y * rY + cy),
                k == n - 1 ? x : (float) (cos * ex * rX - sin * ey * rY + cx),
                k == n - 1 ? y : (float) (sin * ex * rX + cos * ey * rY + cy));
            a = a2;
        }
    }
}
