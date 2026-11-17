package ua.svitlo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import java.util.ArrayList;

public class MainActivity extends Activity {
    final ArrayList<Screen> stack = new ArrayList<Screen>();
    Repo repo;
    private FrameLayout root;
    private int permStep;

    private final Repo.Listener listener = new Repo.Listener() {
        public void onData() {
            Screen s = top();
            if (s != null) s.refresh();
        }
    };

    private final Runnable tick = new Runnable() {
        public void run() {
            Screen s = top();
            if (s != null) s.refresh();
            Ui.H.postDelayed(this, 30000 - System.currentTimeMillis() % 30000 + 200);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.init(this);
        repo = Repo.get(this);
        Launcher.sync(this);
        root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        setContentView(root);
        Compat.edgeToEdge(root);
        push(new MainScreen(this));
        if (!repo.st.firstRunDone()) {
            repo.st.setFirstRunDone();
            nextPermission(); // при первом запуске просим всё сразу
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        repo.addListener(listener);
        // загрузка начинается при запуске, а не при заходе на страницу
        if (System.currentTimeMillis() - repo.st.lastRefresh() > 60000) repo.refreshAsync();
        Scheduler.schedule(this);
        Ui.H.removeCallbacks(tick);
        Ui.H.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        repo.removeListener(listener);
        Ui.H.removeCallbacks(tick);
    }

    // ---------- навигация и анимации переходов ----------

    private static final int DUR_IN = 300, DUR_OUT = 250;
    private Object backCb;

    Screen top() { return stack.isEmpty() ? null : stack.get(stack.size() - 1); }

    private static FrameLayout.LayoutParams full() { return new FrameLayout.LayoutParams(-1, -1); }

    /** Мгновенно завершить идущий переход: оставить в корне только вид верхнего экрана. */
    private void settle() {
        Screen t = top();
        for (int i = root.getChildCount() - 1; i >= 0; i--) {
            View v = root.getChildAt(i);
            v.clearAnimation();
            if (t == null || v != t.view) root.removeViewAt(i);
        }
        if (t != null && t.view.getParent() == null) root.addView(t.view, full());
    }

    private void showNow(Screen s) {
        root.removeAllViews();
        root.addView(s.view, full());
        s.onShow();
    }

    private Animation slide(float fromX, float toX, float fromA, float toA, int dur, boolean decel, boolean keep) {
        AnimationSet set = new AnimationSet(true);
        set.setInterpolator(decel ? new DecelerateInterpolator(1.6f) : new AccelerateInterpolator(1.2f));
        set.addAnimation(new TranslateAnimation(Animation.RELATIVE_TO_PARENT, fromX, Animation.RELATIVE_TO_PARENT, toX,
                Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0));
        set.addAnimation(new AlphaAnimation(fromA, toA));
        set.setDuration(dur);
        set.setFillAfter(keep);
        return set;
    }

    /** forward: новый экран выезжает справа поверх; назад: верхний уезжает вправо, предыдущий проступает слева. */
    private void transition(Screen from, Screen to, boolean forward) {
        View vt = to.view;
        if (vt.getParent() != null) ((ViewGroup) vt.getParent()).removeView(vt);
        if (from == null) { showNow(to); return; }
        View vf = from.view;
        if (forward) root.addView(vt, full()); else root.addView(vt, 0, full());
        to.onShow();
        Animation in, out;
        if (forward) {
            in = slide(0.3f, 0f, 0f, 1f, DUR_IN, true, false);
            out = slide(0f, -0.12f, 1f, 0.5f, DUR_IN, true, true);
        } else {
            in = slide(-0.12f, 0f, 0.5f, 1f, DUR_OUT, true, false);
            out = slide(0f, 0.3f, 1f, 0f, DUR_OUT, false, true);
        }
        Animation last = forward ? in : out;
        last.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation an) {}
            public void onAnimationRepeat(Animation an) {}
            // На Android до 3.0 слушатель вызывается прямо во время отрисовки: убирать вид из root здесь
            // нельзя (вылет по «назад»), поэтому откладываем на следующий такт.
            public void onAnimationEnd(Animation an) {
                Ui.H.post(new Runnable() { public void run() { settle(); } });
            }
        });
        try {
            vt.startAnimation(in);
            vf.startAnimation(out);
        } catch (Throwable t) {
            settle(); // без анимации, но экран сменится
        }
    }

    void push(Screen s) {
        settle();
        Screen t = top();
        if (t != null) t.onHide();
        s.view = s.build();
        stack.add(s);
        transition(t, s, true);
        updateBack();
    }

    void pop() {
        settle();
        if (stack.size() < 2) return;
        Screen t = stack.remove(stack.size() - 1);
        t.onHide();
        hideIme();
        transition(t, top(), false);
        updateBack();
    }

    void home() {
        settle();
        if (stack.size() < 2) return;
        Screen leaving = top();
        while (stack.size() > 1) stack.remove(stack.size() - 1).onHide();
        hideIme();
        transition(leaving, top(), false);
        updateBack();
    }

    /** Пересобрать все экраны (смена языка), без анимации. */
    void rebuild() {
        settle();
        for (int i = 0; i < stack.size(); i++) stack.get(i).view = stack.get(i).build();
        showNow(top());
    }

    // ---------- предиктивный жест «назад» (Android 13+) ----------

    private void updateBack() {
        if (Build.VERSION.SDK_INT < 33) return;
        boolean need = stack.size() > 1;
        if (need && backCb == null) backCb = BackAnim.register(this);
        else if (!need && backCb != null) { BackAnim.unregister(this, backCb); backCb = null; }
    }

    /** Начало жеста: подкладываем предыдущий экран под текущий. Возвращает {верхний, нижний}. */
    View[] beginBack() {
        settle();
        if (stack.size() < 2) return null;
        Screen t = top(), p = stack.get(stack.size() - 2);
        if (p.view.getParent() != null) ((ViewGroup) p.view.getParent()).removeView(p.view);
        root.addView(p.view, 0, full());
        p.onShow();
        return new View[]{t.view, p.view};
    }

    void cancelBack(View below) {
        root.removeView(below);
    }

    void commitBack() {
        if (stack.size() < 2) return;
        Screen t = stack.remove(stack.size() - 1);
        t.onHide();
        hideIme();
        root.removeView(t.view);
        settle();
        updateBack();
    }

    void hideIme() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        if (stack.size() > 1) pop(); else super.onBackPressed();
    }

    // ---------- разрешения (первый запуск): уведомления → точные будильники → без оптимизации батареи ----------

    private void nextPermission() {
        while (true) {
            switch (permStep++) {
                case 0:
                    if (Build.VERSION.SDK_INT >= 33
                            && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
                        return;
                    }
                    break;
                case 1:
                    if (Build.VERSION.SDK_INT >= 31 && !Compat.canExact(this)) {
                        try {
                            startActivityForResult(new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM",
                                    Uri.parse("package:" + getPackageName())), 2);
                            return;
                        } catch (Exception e) { /* экрана нет – идём дальше */ }
                    }
                    break;
                case 2:
                    if (Build.VERSION.SDK_INT >= 23) {
                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                            try {
                                startActivityForResult(new Intent("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
                                        Uri.parse("package:" + getPackageName())), 3);
                                return;
                            } catch (Exception e) { /* пропускаем */ }
                        }
                    }
                    break;
                default:
                    Scheduler.schedule(this);
                    return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        nextPermission();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 2 || req == 3) nextPermission();
    }
}
