package app.common;

public class UtilsHttpResult {

    private final int resultCode;
    private final byte[] data;

    // use this constructor if http request failed (no network, timeout, etc.)
    UtilsHttpResult() {
        resultCode = -1;
        data = null;
    }

    UtilsHttpResult(int resultCode, byte[] data) {
        this.resultCode = resultCode;
        this.data = data;
    }

    public boolean isFailed() {
        return (resultCode < 0);
    }

    // return value < 0 on failed request
    public int getResultCode() {
        return resultCode;
    }

    public byte[] getData() {
        return data;
    }
}
