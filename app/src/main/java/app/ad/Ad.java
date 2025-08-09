package app.ad;

import androidx.annotation.Keep;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Keep
public class Ad implements Serializable {

    public String id;
    public String url;
    public String image;
    public Map<String, String> images = new LinkedHashMap<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Ad ad = (Ad) o;
        return Objects.equals(this.id, ad.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
