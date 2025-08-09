
/*
 * files patch functions
 *
 * last modified: 2015.04.30
 */

package app.common;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Formatter;

import app.common.debug.L;
import app.internal.Settings;

public class LibPatch {

    // return true on success
    public static boolean bspatchFile(String oldfile, String newfile, String patchfile) {
        return pf01(oldfile, newfile, patchfile);
    }

    // patch flags
    public static final int PATCH_DEFAULT = 0; // default flags
    public static final int DELETE_PATCH = 1; // delete patch file on success
    public static final int DELETE_SOURCE = 2; // delete source file on success
    public static final int DELETE_BADNEW = 4; // delete new file on error

    /*
     * patch source file using patch util and save result to new file
     * return true on success (additional flags operations are not checking for errors)
     *
     * also additional flags operations are performed even on error
     */
    public static boolean patchFile(Context context, String oldFile, String patchFile, String newFile,
                                    int patchFlags) {
        boolean retval = false;

        retval = bspatchFile(oldFile, newFile, patchFile);
        if (!retval) {
            if (Settings.DEBUG)
                L.e(Settings.TAG_LIBPATCH, "bspatchFile '" + oldFile + "' + '" + patchFile + "' -> '" + newFile + "' failed");
        }

        // delete bad new file ?
        if (!retval && (patchFlags & DELETE_BADNEW) == DELETE_BADNEW) {
            File file = new File(newFile);
            if (file.canWrite())
                file.delete();
        }

        // delete patch file ?
        if ((patchFlags & DELETE_PATCH) == DELETE_PATCH) {
            File file = new File(patchFile);
            if (file.canWrite())
                file.delete();
        }

        // delete source file ?
        if ((patchFlags & DELETE_SOURCE) == DELETE_SOURCE) {
            File file = new File(oldFile);
            if (file.canWrite())
                file.delete();
        }

        return retval;
    }

    //
    protected static byte[] getFileSha1(String filepath) {
        byte[] digest = new byte[0];

        File file = new File(filepath);
        if (!file.canRead()) {
            if (Settings.DEBUG)
                L.e(Settings.TAG_LIBPATCH, "invalid file for digest '" + filepath + "'");
            return digest;
        }

        InputStream is = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            is = new BufferedInputStream(new FileInputStream(file));

            final byte[] buffer = new byte[1024];
            for (int read = 0; (read = is.read(buffer)) != -1; )
                md.update(buffer, 0, read);
            digest = md.digest();
        } catch (IOException e) {
        } catch (NoSuchAlgorithmException e) {
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        // err check
        if (digest.length == 0)
            L.e(Settings.TAG_LIBPATCH, "digest calc error");

        return digest;
    }

    // see patchFile + return false on digest mismatch
    public static boolean patchFileAndVerify(Context context, String oldFile, String patchFile, String newFile,
                                             int patchFlags, byte[] newDigest) {
        boolean retval = patchFile(context, oldFile, patchFile, newFile, patchFlags);
        if (retval) {
            byte[] digest = getFileSha1(newFile);
            if (digest.length == 0 || !Arrays.equals(digest, newDigest)) {
                L.e(Settings.TAG_LIBPATCH, "digest mismatch");
                retval = false;

                // delete bad new file ?
                if ((patchFlags & DELETE_BADNEW) == DELETE_BADNEW) {
                    File file = new File(newFile);
                    if (file.canWrite())
                        file.delete();
                }
            }
        }

        return retval;
    }

    public static boolean patchFileAndVerify(Context context, String oldFile, String patchFile, String newFile,
                                             int patchFlags, String newDigest) {
        boolean retval = patchFile(context, oldFile, patchFile, newFile, patchFlags);
        if (retval && newDigest != null) {
            byte[] digest = getFileSha1(newFile);

            Formatter formatter = new Formatter();
            for (final byte b : digest)
                formatter.format("%02x", b);
            String digest_str = formatter.toString();

            if (digest.length == 0 || !newDigest.toLowerCase().equals(digest_str)) {
                L.e(Settings.TAG_LIBPATCH, "digest mismatch");
                retval = false;

                // delete bad new file ?
                if ((patchFlags & DELETE_BADNEW) == DELETE_BADNEW) {
                    File file = new File(newFile);
                    if (file.canWrite())
                        file.delete();
                }
            }
        }

        return retval;
    }

    /* --------- real --------- */

    private static native boolean pf01(String oldfile, String newfile, String patchfile);
}
