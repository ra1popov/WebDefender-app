
/*
 * different native types and functions
 *
 * last modified: 2015.04.30
 */

package app.common;

import java.nio.ByteBuffer;

public class LibTypes {

    /* --- int set functions --- */

    // create fast int hash_set, ~0.75 memory overhead, values 0 and -1 can't be stored :(
    public static ByteBuffer intSetCreate() {
        return tf01();
    }

    public static void intSetDelete(ByteBuffer set) {
        tf02(set);
    }

    public static long intSetSize(ByteBuffer set) {
        return tf03(set);
    }

    public static boolean intSetContains(ByteBuffer set, int value) {
        return tf04(set, value);
    }

    // return true if the set was modified by operation
    public static boolean intSetAdd(ByteBuffer set, int value) {
        return tf05(set, value);
    }
    //public static native boolean	  intSetRemove	 (ByteBuffer set, int value);

    /* --------- real --------- */

    private static native ByteBuffer tf01();

    private static native void tf02(ByteBuffer set);

    private static native long tf03(ByteBuffer set);

    private static native boolean tf04(ByteBuffer set, int value);

    private static native boolean tf05(ByteBuffer set, int value);
}
