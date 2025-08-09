package app.common;

import android.os.Build;
import android.os.Process;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class Rnd {

    /*
     * IMPLEMENTATION NOTE: Requests to generate bytes and to mix in a seed
     * are passed through to the Linux PRNG (/dev/urandom). Instances of
     * this class seed themselves by mixing in the current time, PID, UID,
     * build fingerprint, and hardware serial number (where available) into
     * Linux PRNG.
     *
     * Concurrency: Read requests to the underlying Linux PRNG are
     * serialized (on sLock) to ensure that multiple threads do not get
     * duplicated PRNG output.
     */

    private static final File URANDOM_FILE = new File("/dev/urandom");

    private static final Object sLock = new Object();

    /**
     * Input stream for reading from Linux PRNG or {@code null} if not yet
     * opened.
     *
     * @GuardedBy("sLock")
     */
    private static DataInputStream sUrandomIn;

    /**
     * Output stream for writing to Linux PRNG or {@code null} if not yet
     * opened.
     *
     * @GuardedBy("sLock")
     */
    private static OutputStream sUrandomOut;

    /**
     * Whether this engine instance has been seeded. This is needed because
     * each instance needs to seed itself if the client does not explicitly
     * seed it.
     */
    private boolean mSeeded;

    private static final byte[] BUILD_FINGERPRINT_AND_DEVICE_SERIAL =
            getBuildFingerprintAndDeviceSerial();

    private byte[] B = new byte[1];
    private byte[] I = new byte[4];

    public byte[] generateBytes(int size) throws SecurityException {
        byte[] seed = new byte[size];
        engineNextBytes(seed);

        return seed;
    }

    public void generateBytesFill(byte[] bytes) throws SecurityException {
        engineNextBytes(bytes);
    }

    public byte generateByte() throws SecurityException {
        synchronized (sLock) {
            engineNextBytes(B);
            return B[0];
        }
    }

    public int generateInt() throws SecurityException {
        synchronized (sLock) {
            engineNextBytes(I);
            return ((I[0] & 0xff) | ((I[1] & 0xff) << 8) | ((I[2] & 0xff) << 16) | (I[3] << 24));
        }
    }

    /*
     * returns a pseudo-random uniformly distributed int in the half-open range [0, n)
     * see Random.nextInt(int n)
     *
     * from + rnd.nextInt(to - from + 1)
     */
    public int generateInt(int n) throws SecurityException, IllegalArgumentException {
        if (n <= 0)
            throw new IllegalArgumentException();

        if ((n & -n) == n)
            return (int) ((n * (long) (generateInt() & 0x7fffffff)) >> 31);

        int bits, val;
        do {
            bits = (generateInt() & 0x7fffffff);
            val = bits % n;
        }
        while (bits - val + (n - 1) < 0);

        return val;
    }

    protected void engineNextBytes(byte[] bytes) {
        if (!mSeeded) {
            // Mix in the device- and invocation-specific seed.
            engineSetSeed(generateSeed());
        }

        try {
            DataInputStream in;
            synchronized (sLock) {
                in = getUrandomInputStream();
            }
            synchronized (in) {
                in.readFully(bytes);
            }
        } catch (IOException e) {
            throw new SecurityException("Failed to read from " + URANDOM_FILE, e);
        }
    }

    protected void engineSetSeed(byte[] bytes) {
        try {
            OutputStream out;
            synchronized (sLock) {
                out = getUrandomOutputStream();
            }
            out.write(bytes);
            out.flush();
        } catch (IOException e) {
            // On a small fraction of devices /dev/urandom is not writable.
            // Log and ignore.
            //Log.w(PRNGFixes.class.getSimpleName(),
            //		  "Failed to mix seed into " + URANDOM_FILE);
        } finally {
            mSeeded = true;
        }
    }

    /**
     * Generates a device- and invocation-specific seed to be mixed into the
     * Linux PRNG.
     */
    public static byte[] generateSeed() throws SecurityException {
        Exception e;

        try {
            ByteArrayOutputStream seedBuffer = new ByteArrayOutputStream();
            DataOutputStream seedBufferOut = new DataOutputStream(seedBuffer);

            seedBufferOut.writeLong(System.currentTimeMillis());
            seedBufferOut.writeLong(System.nanoTime());
            seedBufferOut.writeInt(Process.myPid());
            seedBufferOut.writeInt(Process.myUid());
            seedBufferOut.write(BUILD_FINGERPRINT_AND_DEVICE_SERIAL);
            seedBufferOut.close();

            return seedBuffer.toByteArray();
        } catch (IOException | RuntimeException ex) {
            e = ex;
        }

        throw new SecurityException("Failed to generate seed", e);
    }

    public DataInputStream getUrandomInputStream() throws SecurityException {
        synchronized (sLock) {
            if (sUrandomIn == null) {
                // NOTE: Consider inserting a BufferedInputStream between
                // DataInputStream and FileInputStream if you need higher
                // PRNG output performance and can live with future PRNG
                // output being pulled into this process prematurely.
                try {
                    sUrandomIn = new DataInputStream(new FileInputStream(URANDOM_FILE));
                } catch (IOException e) {
                    throw new SecurityException("Failed to open " + URANDOM_FILE + " for reading", e);
                }
            }

            return sUrandomIn;
        }
    }

    public OutputStream getUrandomOutputStream() throws IOException {
        synchronized (sLock) {
            if (sUrandomOut == null)
                sUrandomOut = new FileOutputStream(URANDOM_FILE);

            return sUrandomOut;
        }
    }

    /**
     * Gets the hardware serial number of this device.
     *
     * @return serial number or {@code null} if not available.
     */
    public static String getDeviceSerialNumber() {
        // We're using the Reflection API because Build.SERIAL is only available
        // since API Level 9 (Gingerbread, Android 2.3).
        try {
            return (String) Build.class.getField("SERIAL").get(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static byte[] getBuildFingerprintAndDeviceSerial() throws RuntimeException {
        StringBuilder result = new StringBuilder();

        String fingerprint = Build.FINGERPRINT;
        if (fingerprint != null)
            result.append(fingerprint);

        String serial = getDeviceSerialNumber();
        if (serial != null)
            result.append(serial);

        try {
            return result.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not supported");
        }
    }
}
