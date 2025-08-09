package app.ui.statlog;

import androidx.annotation.Keep;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.util.Objects;

@Keep
@DatabaseTable(tableName = "statlog")
public class Statlog implements Serializable {

    public static final String FIELD_ID = "id";

    public static final int TYPE_AD = 1;
    public static final int TYPE_APK = 2;
    public static final int TYPE_MALWARE = 3;
    public static final int TYPE_PAID = 4;

    @DatabaseField(columnName = FIELD_ID, generatedId = true, index = true)
    public long id;

    @DatabaseField
    public int type;

    @DatabaseField
    public String pkgName;

    @DatabaseField
    public String domain;

    @DatabaseField
    public long time;

    public Statlog() {

    }

    public Statlog(int type, String pkgName, String domain, long time) {
        this.type = type;
        this.pkgName = pkgName;
        this.domain = domain;
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Statlog statlog = (Statlog) o;
        return Objects.equals(this.id, statlog.id)
                && Objects.equals(this.type, statlog.type)
                && Objects.equals(this.pkgName, statlog.pkgName)
                && Objects.equals(this.domain, statlog.domain)
                && Objects.equals(this.time, statlog.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, pkgName, domain, time);
    }

}
