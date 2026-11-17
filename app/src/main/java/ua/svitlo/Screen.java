package ua.svitlo;

import android.view.View;

/** Экран внутри единственной Activity (фрагментов на Android 2.x нет). */
abstract class Screen {
    final MainActivity a;
    View view;

    Screen(MainActivity a) { this.a = a; }

    abstract View build();
    void onShow() { refresh(); }
    void onHide() {}
    void refresh() {}
}
