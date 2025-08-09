package app.adapter;

import java.io.Serializable;

public abstract class DataFilterable implements Serializable {

    public String title;

    protected DataFilterable(String title) {
        this.title = title;
    }

}
