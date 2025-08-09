package app.util;

import android.view.View;

import androidx.annotation.CallSuper;

public class SafeOnClickListener implements View.OnClickListener {

    private static final long DELAY = 1000;

    private View button;

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (button != null) {
                button.setClickable(true);
            }
        }
    };

    @CallSuper
    @Override
    public void onClick(View v) {
        button = v;
        if (button != null) {
            button.setClickable(false);
            Util.runOnMainDelayed(runnable, DELAY);
        }
    }

}