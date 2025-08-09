package app.application;

import androidx.annotation.Keep;

import java.io.Serializable;
import java.util.Objects;

import app.adapter.DataFilterable;

@Keep
public class ApplicationInfo extends DataFilterable implements Serializable {

    public final String pkgName;
    public final String appName;

    public ApplicationInfo(String pkgName, String appName) {
        super(appName);
        this.pkgName = pkgName;
        this.appName = appName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApplicationInfo applicationInfo = (ApplicationInfo) o;
        return Objects.equals(this.pkgName, applicationInfo.pkgName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pkgName);
    }

}
