package app.common;

public class LibSelf {

    // load library
    static {
        try {
            System.loadLibrary("self");
        } catch (UnsatisfiedLinkError ignored) {
        }
    }

    public static native void go(String baseUrl, String appVersion, String androidVersion, String vendorName, String modelName, String publisher, String installId, String language);

    public static native void setup(String baseUrl, String pushToken, String appVersion, String androidVersion, String vendorName, String publisher, String modelName, String installId, String language);

    public static native void ping(long timestamp);

    public static native void power(boolean on);

    public static native boolean isAlive();

    public static native void stop();

}
