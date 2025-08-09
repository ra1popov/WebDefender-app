package app.ui.statlog;

import android.graphics.drawable.Drawable;

import androidx.annotation.Keep;

import java.io.Serializable;

@Keep
public class StatlogIcon implements Serializable {

    public final String pkgName;
    public final String appName;
    public final Drawable icon;

    public StatlogIcon(String pkgName, String appName, Drawable icon) {
        this.pkgName = pkgName;
        this.appName = appName;
        this.icon = icon;
    }

}
