package app.util;

import java.util.concurrent.Callable;


public abstract class Callback<T> implements Callable<Void> {
    public T result;

    public void setResult(T result) {
        this.result = result;
    }

    public abstract Void call();
}