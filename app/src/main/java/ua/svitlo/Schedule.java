package ua.svitlo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/** Разобранный TSV-график: дни × 48 получасовых слотов (0 – нет данных, 1 – свет есть, 2 – нет). */
final class Schedule {
    static final TimeZone TZ = kyiv();

    private static TimeZone kyiv() {
        TimeZone t = TimeZone.getTimeZone("Europe/Kyiv");
        if (!"Europe/Kyiv".equals(t.getID())) {
            t = TimeZone.getTimeZone("Europe/Kiev");
            if (!"Europe/Kiev".equals(t.getID())) {
                // старый tzdata без Києва: UTC+2, летнее время по правилам ЕС
                t = new SimpleTimeZone(7200000, "Kyiv",
                        Calendar.MARCH, -1, Calendar.SUNDAY, 3600000, SimpleTimeZone.UTC_TIME,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 3600000, SimpleTimeZone.UTC_TIME, 3600000);
            }
        }
        return t;
    }

    static final class Change {
        final long time;
        final boolean toOn;
        Change(long time, boolean toOn) { this.time = time; this.toOn = toOn; }
    }

    static final class Interval {
        final int s, e;
        final boolean on;
        Interval(int s, int e, boolean on) { this.s = s; this.e = e; this.on = on; }
    }

    final String raw;
    final String[] dates;
    final byte[][] v;
    private final int[][] ymd;

    private Schedule(String raw, String[] dates, byte[][] v) {
        this.raw = raw; this.dates = dates; this.v = v;
        ymd = new int[dates.length][];
        for (int i = 0; i < dates.length; i++) {
            try {
                String d = dates[i];
                ymd[i] = new int[]{Integer.parseInt(d.substring(0, 4)),
                        Integer.parseInt(d.substring(5, 7)), Integer.parseInt(d.substring(8, 10))};
            } catch (Exception e) {
                ymd[i] = null;
            }
        }
    }

    static Schedule parse(String raw) {
        if (raw == null) return null;
        String[] lines = raw.split("\n", -1);
        if (lines.length < 2) return null;
        // trim() всей строки нельзя: хвостовая табуляция = пустая ячейка
        String[] h = lines[0].replace("\r", "").split("\t", -1);
        if (h.length < 2 || !"time".equals(h[0].trim())) return null;
        int n = h.length - 1;
        String[] dates = new String[n];
        for (int i = 0; i < n; i++) dates[i] = h[i + 1].trim();
        byte[][] v = new byte[n][48];
        for (int i = 1; i < lines.length; i++) {
            String ln = lines[i].replace("\r", "");
            if (ln.trim().length() == 0) continue;
            String[] p = ln.split("\t", -1);
            int slot = slotOf(p[0].trim());
            if (slot < 0) continue;
            for (int d = 0; d < n; d++) {
                String x = d + 1 < p.length ? p[d + 1].trim() : "";
                v[d][slot] = (byte) ("on".equals(x) ? 1 : "off".equals(x) ? 2 : 0);
            }
        }
        return new Schedule(raw, dates, v);
    }

    private static int slotOf(String t) {
        try {
            int c = t.indexOf(':');
            int hh = Integer.parseInt(t.substring(0, c)), mm = Integer.parseInt(t.substring(c + 1));
            if (hh < 0 || hh > 23 || mm < 0 || mm > 59) return -1;
            return hh * 2 + (mm >= 30 ? 1 : 0);
        } catch (Exception e) {
            return -1;
        }
    }

    // ---------- время ----------

    static Calendar cal(long t) {
        Calendar c = Calendar.getInstance(TZ);
        c.setTimeInMillis(t);
        return c;
    }

    static String dateOf(Calendar c) {
        int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH) + 1, d = c.get(Calendar.DAY_OF_MONTH);
        return y + "-" + (m < 10 ? "0" : "") + m + "-" + (d < 10 ? "0" : "") + d;
    }

    static String dateOfOffset(long now, int days) {
        Calendar c = cal(now);
        c.add(Calendar.DAY_OF_MONTH, days);
        return dateOf(c);
    }

    static String slotTime(int slot) {
        slot %= 48;
        int h = slot / 2, m = (slot % 2) * 30;
        return (h < 10 ? "0" : "") + h + ":" + (m == 0 ? "00" : "30");
    }

    long millis(int d, int slot) {
        Calendar c = Calendar.getInstance(TZ);
        c.clear();
        int[] y = ymd[d];
        if (y == null) return 0;
        c.set(y[0], y[1] - 1, y[2], slot / 2, (slot % 2) * 30, 0);
        return c.getTimeInMillis();
    }

    // ---------- запросы ----------

    int dateIndex(String ds) {
        for (int i = 0; i < dates.length; i++) if (dates[i].equals(ds)) return i;
        return -1;
    }

    boolean hasData(int d) {
        for (int i = 0; i < 48; i++) if (v[d][i] != 0) return true;
        return false;
    }

    int stateAt(long t) {
        Calendar c = cal(t);
        int d = dateIndex(dateOf(c));
        if (d < 0) return 0;
        return v[d][c.get(Calendar.HOUR_OF_DAY) * 2 + c.get(Calendar.MINUTE) / 30];
    }

    private boolean contiguous(int d) {
        if (d + 1 >= dates.length || ymd[d] == null || ymd[d + 1] == null) return false;
        long df = millis(d + 1, 0) - millis(d, 0);
        return df >= 23 * 3600000L && df <= 25 * 3600000L;
    }

    /** Ближайшая смена состояния (по сегодня+завтра), null если неизвестно. */
    Change nextChange(long t) {
        Calendar c = cal(t);
        int d = dateIndex(dateOf(c));
        if (d < 0) return null;
        int s = c.get(Calendar.HOUR_OF_DAY) * 2 + c.get(Calendar.MINUTE) / 30;
        byte cur = v[d][s];
        if (cur == 0) return null;
        s++;
        while (true) {
            if (s >= 48) {
                if (!contiguous(d)) return null;
                d++;
                s = 0;
            }
            byte x = v[d][s];
            if (x == 0) return null;
            if (x != cur) return new Change(millis(d, s), x == 1);
            s++;
        }
    }

    ArrayList<Interval> intervals(int d) {
        ArrayList<Interval> r = new ArrayList<Interval>();
        int i = 0;
        while (i < 48) {
            byte x = v[d][i];
            if (x == 0) { i++; continue; }
            int j = i + 1;
            while (j < 48 && v[d][j] == x) j++;
            r.add(new Interval(i, j, x == 1));
            i = j;
        }
        return r;
    }

    /** «02:00–05:30, 13:00–17:00» или "" если отключений нет. */
    String outageSummary(int d) {
        StringBuilder sb = new StringBuilder();
        ArrayList<Interval> l = intervals(d);
        for (int i = 0; i < l.size(); i++) {
            Interval iv = l.get(i);
            if (iv.on) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(slotTime(iv.s)).append('–').append(slotTime(iv.e));
        }
        return sb.toString();
    }
}
