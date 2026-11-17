package ua.svitlo;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Insets;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;

/** Всё, что зависит от версии Android, собрано здесь; новые API изолированы во вложенных классах. */
final class Compat {
    private Compat() {}

    static int immutable() {
        return Build.VERSION.SDK_INT >= 23 ? 0x04000000 : 0; // FLAG_IMMUTABLE
    }

    // ---------- будильники ----------

    static boolean canExact(Context c) {
        return Build.VERSION.SDK_INT < 31 || A31.can(c);
    }

    static void setAlarm(Context c, long at, PendingIntent pi) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        int sdk = Build.VERSION.SDK_INT;
        try {
            if (sdk >= 31) A31.set(am, at, pi);
            else if (sdk >= 23) A23.set(am, at, pi);
            else if (sdk >= 19) A19.set(am, at, pi);
            else am.set(AlarmManager.RTC_WAKEUP, at, pi);
        } catch (RuntimeException e) { // нет права на точные будильники и т.п.: ставим обычный
            am.set(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    static class A19 {
        static void set(AlarmManager am, long at, PendingIntent pi) { am.setExact(AlarmManager.RTC_WAKEUP, at, pi); }
    }

    static class A23 {
        static void set(AlarmManager am, long at, PendingIntent pi) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    static class A31 {
        static boolean can(Context c) {
            return ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms();
        }

        static void set(AlarmManager am, long at, PendingIntent pi) {
            if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    // ---------- работа в фоне после будильника ----------

    static void runInBackground(BroadcastReceiver r, Context c) {
        if (Build.VERSION.SDK_INT >= 11) B11.run(r, c);
        else UpdateService.start(c); // IntentService: API 3+
    }

    static class B11 {
        static void run(BroadcastReceiver r, final Context c) {
            final BroadcastReceiver.PendingResult pr = r.goAsync();
            new Thread() {
                public void run() {
                    try { Updater.run(c); } finally { if (pr != null) pr.finish(); }
                }
            }.start();
        }
    }

    // ---------- уведомления ----------

    /** Вид уведомления: от него зависит звук. Нейтральный – смена графика и всё остальное. */
    static final int K_NEUTRAL = 0, K_ON = 1, K_OFF = 2;

    private static final String[] SND = {"snd_neutral", "snd_happy", "snd_sad"};
    private static final String[] CH = {"change_v2", "on_v2", "off_v2"};
    private static final String[] CH_NAME = {"ch_changes", "ch_on", "ch_off"};

    /** Звуки лежат в res/raw. Ссылка по имени, а не по числовому id: канал помнит её и после обновлений. */
    static Uri sound(Context c, int kind) {
        return Uri.parse("android.resource://" + c.getPackageName() + "/raw/" + SND[kind]);
    }

    static void post(Context c, int id, int kind, String title, String text) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | immutable());
        int sdk = Build.VERSION.SDK_INT;
        try {
            Notification n;
            if (sdk >= 26) n = N26.build(c, nm, kind, title, text, pi);
            else if (sdk >= 16) n = N16.build(c, kind, title, text, pi);
            else if (sdk >= 11) n = N11.build(c, kind, title, text, pi);
            else n = legacy(c, kind, title, text, pi);
            nm.notify(id, n);
        } catch (Throwable t) {
            // деградация: без своего звука (системный по умолчанию), но уведомление не теряем
            if (sdk < 26) {
                try { nm.notify(id, legacy(c, -1, title, text, pi)); } catch (Throwable ignore) { /* что поделать */ }
            }
        }
    }

    /** Свой звук задаётся отдельно, поэтому из «по умолчанию» оставляем только вибрацию и индикатор. */
    private static final int DEF = Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE;

    static class N11 {
        static Notification build(Context c, int kind, String title, String text, PendingIntent pi) {
            return new Notification.Builder(c).setSmallIcon(R.drawable.ic_stat).setContentTitle(title)
                    .setContentText(text).setContentIntent(pi).setAutoCancel(true)
                    .setSound(sound(c, kind)).setDefaults(DEF).getNotification();
        }
    }

    static class N16 {
        static Notification build(Context c, int kind, String title, String text, PendingIntent pi) {
            return new Notification.Builder(c).setSmallIcon(R.drawable.ic_stat).setContentTitle(title)
                    .setContentText(text).setContentIntent(pi).setAutoCancel(true)
                    .setSound(sound(c, kind)).setDefaults(DEF).setPriority(Notification.PRIORITY_HIGH)
                    .setStyle(new Notification.BigTextStyle().bigText(text)).build();
        }
    }

    static class N26 {
        static Notification build(Context c, NotificationManager nm, int kind, String title, String text, PendingIntent pi) {
            // Звук канала после создания не меняется, поэтому каналы новые (_v2), а прежние «alerts»/«changes» удаляем.
            nm.deleteNotificationChannel("alerts");
            nm.deleteNotificationChannel("changes");
            String id = CH[kind];
            NotificationChannel ch = new NotificationChannel(id, S.t(CH_NAME[kind]),
                    kind == K_NEUTRAL ? NotificationManager.IMPORTANCE_DEFAULT : NotificationManager.IMPORTANCE_HIGH);
            ch.setSound(sound(c, kind), new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            nm.createNotificationChannel(ch);
            return new Notification.Builder(c, id).setSmallIcon(R.drawable.ic_stat).setContentTitle(title)
                    .setContentText(text).setContentIntent(pi).setAutoCancel(true)
                    .setStyle(new Notification.BigTextStyle().bigText(text)).build();
        }
    }

    /** Android 2.x: kind < 0 – обычный системный звук. */
    @SuppressWarnings("deprecation")
    private static Notification legacy(Context c, int kind, String title, String text, PendingIntent pi) {
        Notification n = new Notification(R.drawable.ic_stat, text, System.currentTimeMillis());
        n.flags |= Notification.FLAG_AUTO_CANCEL;
        if (kind >= 0) {
            n.sound = sound(c, kind);
            n.defaults = DEF;
        } else {
            n.defaults = Notification.DEFAULT_ALL;
        }
        try { // setLatestEventInfo удалён из SDK, на старых системах вызываем рефлексией
            Notification.class.getMethod("setLatestEventInfo", Context.class, CharSequence.class,
                    CharSequence.class, PendingIntent.class).invoke(n, c, title, text, pi);
        } catch (Exception e) {
            // без текста, но уведомление всё равно покажется
        }
        return n;
    }

    // ---------- системные экраны ----------

    static void openNotifSettings(Activity a) {
        String pkg = a.getPackageName();
        Intent i;
        if (Build.VERSION.SDK_INT >= 26) {
            i = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
            i.putExtra("android.provider.extra.APP_PACKAGE", pkg);
        } else {
            i = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.parse("package:" + pkg));
        }
        try { a.startActivity(i); } catch (Exception e) { /* нет такого экрана */ }
    }

    // ---------- оформление ----------

    /** Android 15+ рисует edge-to-edge принудительно: отступаем под системные панели и клавиатуру. */
    static void edgeToEdge(View root) {
        if (Build.VERSION.SDK_INT >= 35) V35.apply(root);
    }

    static class V35 {
        static void apply(View root) {
            root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                public WindowInsets onApplyWindowInsets(View v, WindowInsets in) {
                    Insets i = in.getInsets(WindowInsets.Type.systemBars()
                            | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                    v.setPadding(i.left, i.top, i.right, i.bottom);
                    return WindowInsets.CONSUMED;
                }
            });
        }
    }

    static void cursor(EditText e, int color) {
        if (Build.VERSION.SDK_INT >= 29) V29.cursor(e, color);
    }

    static class V29 {
        static void cursor(EditText e, int color) {
            GradientDrawable g = new GradientDrawable();
            g.setColor(color);
            g.setSize(Ui.px(4), Ui.px(56));
            e.setTextCursorDrawable(g);
        }
    }
}
