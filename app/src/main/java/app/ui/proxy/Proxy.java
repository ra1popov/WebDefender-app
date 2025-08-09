package app.ui.proxy;

import androidx.annotation.Keep;

import java.io.Serializable;
import java.util.Objects;

@Keep
public class Proxy implements Serializable {

    public String country;
    public String title;
    public int icon;
    public boolean selected;

    public Proxy(String country, String title, int icon, boolean selected) {
        this.country = country;
        this.title = title;
        this.icon = icon;
        this.selected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Proxy proxy = (Proxy) o;
        return Objects.equals(this.country, proxy.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(country);
    }

}
