package app.tgphotos.core.telegram;

/** Original JNI boundary for the official TDLib JSON C API. No native binary is packaged yet. */
public final class NativeTdJson {
    private NativeTdJson() {}
    public static void load() { System.loadLibrary("tgphotos_tdjson"); }
    public static native int createClient();
    public static native void send(int clientId, String request);
    /** One global receiver, as required by td_receive. Returned JSON is copied before the next receive. */
    public static synchronized native String receive(double timeoutSeconds);
    /** Serialize with receive while native returned JSON is copied; either call can invalidate its pointer. */
    public static synchronized native String execute(String request);
}
