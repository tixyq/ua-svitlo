package ua.svitlo;

import android.view.View;
import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import android.window.OnBackInvokedCallback;

/** Android 14+: экран уменьшается и смещается вслед за пальцем, под ним проступает предыдущий. */
final class BackAnim34 {
    private BackAnim34() {}

    static OnBackInvokedCallback create(MainActivity a) { return new Cb(a); }

    private static final class Cb implements OnBackAnimationCallback {
        private final MainActivity a;
        private View top, below;
        private boolean active;
        private float dir = 1f;

        Cb(MainActivity a) { this.a = a; }

        public void onBackStarted(BackEvent e) {
            View[] v = a.beginBack();
            if (v == null) return;
            top = v[0];
            below = v[1];
            dir = e.getSwipeEdge() == BackEvent.EDGE_LEFT ? 1f : -1f;
            active = true;
        }

        public void onBackProgressed(BackEvent e) {
            if (!active) return;
            float p = e.getProgress();
            top.setScaleX(1f - 0.1f * p);
            top.setScaleY(1f - 0.1f * p);
            top.setTranslationX(dir * p * top.getWidth() * 0.06f);
            below.setScaleX(0.94f + 0.06f * p);
            below.setScaleY(0.94f + 0.06f * p);
            below.setAlpha(0.4f + 0.6f * p);
        }

        public void onBackCancelled() {
            if (!active) return;
            active = false;
            final View t = top, b = below;
            t.animate().scaleX(1f).scaleY(1f).translationX(0f).setDuration(180).withEndAction(new Runnable() {
                public void run() {
                    BackAnim.reset(t);
                    BackAnim.reset(b);
                    a.cancelBack(b);
                }
            });
        }

        public void onBackInvoked() {
            if (!active) { a.pop(); return; } // кнопка «назад» без жеста
            active = false;
            final View t = top, b = below;
            b.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(200);
            t.animate().translationX(dir * t.getWidth() * 0.5f).alpha(0f).scaleX(0.85f).scaleY(0.85f)
                    .setDuration(200).withEndAction(new Runnable() {
                public void run() {
                    BackAnim.reset(t);
                    BackAnim.reset(b);
                    a.commitBack();
                }
            });
        }
    }
}
