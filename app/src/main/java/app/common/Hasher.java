package app.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import app.common.debug.L;
import app.internal.Settings;
import app.netfilter.IHasherListener;

public class Hasher {

    MessageDigest sha1 = null;
    MessageDigest sha256 = null;
    MessageDigest md5 = null;
    private long size = 0;
    private String url = null;
    private IHasherListener listener = null;

    public Hasher(String url, IHasherListener listener) {
        this.url = url;
        this.listener = listener;

        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }

    public long getSize() {
        return size;
    }

    public void update(byte[] buf, int offset, int length) {
        // TODO Implement proper reading of chunked.

        if (sha1 != null)
            sha1.update(buf, offset, length);
        if (sha256 != null)
            sha256.update(buf, offset, length);
        if (md5 != null)
            md5.update(buf, offset, length);

        size += length;
    }

    public void update(byte[] buf) {
        update(buf, 0, buf.length);
    }

    public void finish() {
        if (listener != null)
            listener.onFinish(url, getSha1(), getSha256(), getMd5());
    }

    public byte[] getSha1() {
        return sha1.digest();
    }

    public byte[] getSha256() {
        return sha256.digest();
    }

    public byte[] getMd5() {
        return md5.digest();
    }

    public void hashFile(String filePath) {
        int size;
        byte[] buf = new byte[8192];
        InputStream is = null;

        try {
            is = new FileInputStream(filePath);

            while ((size = is.read(buf)) > 0) {
                update(buf, 0, size);
            }
        } catch (IOException e) {
            L.a(Settings.TAG_HASHER, "Error openning file: " + filePath);
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    //

    public static byte[] md5(String password) {
        return md5(password.getBytes());
    }

    public static byte[] md5(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(data);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] sha1(String password) {
        return sha1(password.getBytes());
    }

    public static byte[] sha1(byte[] data) {
        try {
            MessageDigest mdSha1 = MessageDigest.getInstance("SHA-1");
            mdSha1.update(data);
            return mdSha1.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] sha1File(String filePath) {
        int size;
        byte[] buf = new byte[8192];
        InputStream is = null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            is = new FileInputStream(filePath);

            while ((size = is.read(buf)) > 0) {
                digest.update(buf, 0, size);
                //Thread.yield();
            }

            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
            L.a(Settings.TAG_HASHER, "Error openning file: " + filePath);
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return null;
    }

    public static String sha1DexFromApkAsBase64(String path) {
        String res = null;

        final ByteBuffer entry = LibNative.zipReadFile(path, "META-INF/MANIFEST.MF");
        if (entry == null)
            return "No manifest in file!";

        final byte[] tmp = new byte[entry.capacity()];
        entry.get(tmp);
        LibNative.zipFreeData(entry);

        final FastLineReader reader = new FastLineReader(tmp, 0);
        while (true) {
            String line = reader.readLine();
            if (line != null && line.equalsIgnoreCase("Name: classes.dex")) {
                line = reader.readLine();
                if (line != null && LibNative.asciiStartsWith("SHA1-Digest:", line) && line.length() > 12) {
                    line = line.substring(12).trim();
                    reader.close();
                    res = line;
                    break;
                }
            }
            if (line == null)
                break;
        }
        reader.close();

        return res;
    }
}
