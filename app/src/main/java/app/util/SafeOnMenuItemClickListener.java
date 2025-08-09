package app.util;

import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;

public class SafeOnMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

    private static final long DELAY = 1000;

    private boolean isMenuClickAllowed = true;

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            isMenuClickAllowed = true;
        }
    };

    @Override
    public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
        if (!isMenuClickAllowed) {
            return false;
        }
        isMenuClickAllowed = false;
        Util.runOnMainDelayed(runnable, DELAY);
        return true;
    }

}