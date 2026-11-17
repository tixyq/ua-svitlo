package ua.svitlo;

import android.os.Build;
import android.view.View;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/**
 * Жест «назад» Android 13+ (на 14+ с предиктивной анимацией по ходу пальца).
 * Класс загружается только на API 33+. Колбэк регистрируется, пока есть куда вернуться;
 * на корневом экране его нет, и система сама проигрывает анимацию «в домой».
 */
final class BackAnim {
    private BackAnim() {}

    static Object register(MainActivity a) {
        OnBackInvokedCallback cb = Build.VERSION.SDK_INT >= 34 ? BackAnim34.create(a) : new Cb33(a);
        a.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, cb);
        return cb;
    }

    static void unregister(MainActivity a, Object cb) {
        a.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((OnBackInvokedCallback) cb);
    }

    static void reset(View v) {
        v.animate().cancel();
        v.setScaleX(1f);
        v.setScaleY(1f);
        v.setTranslationX(0f);
        v.setAlpha(1f);
    }

    /** Android 13: без прогресса жеста, обычная анимация закрытия. */
    static final class Cb33 implements OnBackInvokedCallback {
        private final MainActivity a;
        Cb33(MainActivity a) { this.a = a; }
        public void onBackInvoked() { a.pop(); }
    }
}
