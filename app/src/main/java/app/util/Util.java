package app.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class Util {

    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static Handler getHandler() {
        return handler;
    }

    @SafeVarargs
    public static <T> List<T> asList(T... elements) {
        List<T> result = new LinkedList<>();
        Collections.addAll(result, elements);
        return result;
    }

    public static String join(String[] list, String delimiter) {
        return join(Arrays.asList(list), delimiter);
    }

    public static String join(Collection<String> list, String delimiter) {
        StringBuilder result = new StringBuilder();
        int i = 0;

        for (String item : list) {
            result.append(item);

            if (++i < list.size()) {
                result.append(delimiter);
            }
        }

        return result.toString();
    }

    public static String join(@NonNull int[] list, String delimeter) {
        StringBuilder sb = new StringBuilder();

        for (int j = 0; j < list.length; j++) {
            if (j != 0) sb.append(delimeter);
            sb.append(list[j]);
        }

        return sb.toString();
    }

    public static String join(long[] list, String delimeter) {
        List<Long> boxed = new ArrayList<>(list.length);

        for (long l : list) {
            boxed.add(l);
        }

        return join(boxed, delimeter);
    }

    public static String join(List<Long> list, String delimeter) {
        StringBuilder sb = new StringBuilder();

        for (int j = 0; j < list.size(); j++) {
            if (j != 0) sb.append(delimeter);
            sb.append(list.get(j));
        }

        return sb.toString();
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static String getFirstNonEmpty(String... values) {
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    public static <E> List<List<E>> chunk(@NonNull List<E> list, int chunkSize) {
        List<List<E>> chunks = new ArrayList<>(list.size() / chunkSize);

        for (int i = 0; i < list.size(); i += chunkSize) {
            List<E> chunk = list.subList(i, Math.min(list.size(), i + chunkSize));
            chunks.add(chunk);
        }

        return chunks;
    }

    public static CharSequence getBoldedString(String value) {
        SpannableString spanned = new SpannableString(value);
        spanned.setSpan(new StyleSpan(Typeface.BOLD), 0, spanned.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spanned;
    }

    public static void wait(Object lock, long timeout) {
        try {
            lock.wait(timeout);
        } catch (InterruptedException ie) {
            throw new AssertionError(ie);
        }
    }

    public static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

    public static long getStreamLength(InputStream in) throws IOException {
        byte[] buffer = new byte[4096];
        int totalSize = 0;

        int read;

        while ((read = in.read(buffer)) != -1) {
            totalSize += read;
        }

        return totalSize;
    }

    public static void readFully(InputStream in, byte[] buffer) throws IOException {
        readFully(in, buffer, buffer.length);
    }

    public static void readFully(InputStream in, byte[] buffer, int len) throws IOException {
        int offset = 0;

        while (true) {
            int read = in.read(buffer, offset, len - offset);
            if (read == -1) {
                throw new EOFException("Stream ended early");
            }

            if (read + offset < len) {
                offset += read;
            } else {
                return;
            }
        }
    }

    public static byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;

        while ((read = in.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }

        in.close();

        return baos.toByteArray();
    }

    public static String readFullyAsString(InputStream in) throws IOException {
        return new String(readFully(in), StandardCharsets.UTF_8);
    }

    public static long copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        long total = 0;

        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            total += read;
        }

        in.close();
        out.close();

        return total;
    }

    public static <T> List<List<T>> partition(List<T> list, int partitionSize) {
        List<List<T>> results = new LinkedList<>();

        for (int index = 0; index < list.size(); index += partitionSize) {
            int subListSize = Math.min(partitionSize, list.size() - index);

            results.add(list.subList(index, index + subListSize));
        }

        return results;
    }

    public static List<String> split(String source, String delimiter) {
        List<String> results = new LinkedList<>();

        if (TextUtils.isEmpty(source)) {
            return results;
        }

        String[] elements = source.split(delimiter);
        Collections.addAll(results, elements);

        return results;
    }

    public static byte[][] split(byte[] input, int firstLength, int secondLength) {
        byte[][] parts = new byte[2][];

        parts[0] = new byte[firstLength];
        System.arraycopy(input, 0, parts[0], 0, firstLength);

        parts[1] = new byte[secondLength];
        System.arraycopy(input, firstLength, parts[1], 0, secondLength);

        return parts;
    }

    public static byte[] combine(byte[]... elements) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            for (byte[] element : elements) {
                baos.write(element);
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[] trim(byte[] input, int length) {
        byte[] result = new byte[length];
        System.arraycopy(input, 0, result, 0, result.length);

        return result;
    }

    public static int getManifestApkVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[] getSecretBytes(int size) {
        byte[] secret = new byte[size];
        getSecureRandom().nextBytes(secret);
        return secret;
    }

    public static SecureRandom getSecureRandom() {
        return new SecureRandom();
    }

    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static void postToMain(final @NonNull Runnable runnable) {
        getHandler().post(runnable);
    }

    public static void postDelayedToMain(final @NonNull Runnable runnable, long delayMillis) {
        getHandler().postDelayed(runnable, delayMillis);
    }

    public static void runOnMain(final @NonNull Runnable runnable) {
        if (isMainThread()) {
            runnable.run();
        } else {
            getHandler().post(runnable);
        }
    }

    public static void runOnMainDelayed(final @NonNull Runnable runnable, long delayMillis) {
        getHandler().postDelayed(runnable, delayMillis);
    }

    public static void cancelRunnableOnMain(@NonNull Runnable runnable) {
        getHandler().removeCallbacks(runnable);
    }

    public static void runOnMainSync(final @NonNull Runnable runnable) {
        if (isMainThread()) {
            runnable.run();
        } else {
            final CountDownLatch sync = new CountDownLatch(1);
            runOnMain(() -> {
                try {
                    runnable.run();
                } finally {
                    sync.countDown();
                }
            });
            try {
                sync.await();
            } catch (InterruptedException ie) {
                throw new AssertionError(ie);
            }
        }
    }

    public static void runOnThread(final @NonNull Runnable runnable) {
        new Thread(runnable).start();
    }

    public static <T> T getRandomElement(T[] elements) {
        return elements[new SecureRandom().nextInt(elements.length)];
    }

    public static boolean equals(@Nullable Object a, @Nullable Object b) {
        return Objects.equals(a, b);
    }

    public static int hashCode(@Nullable Object... objects) {
        return Arrays.hashCode(objects);
    }

    @Nullable
    public static Uri uri(@Nullable String uri) {
        if (uri == null) return null;
        else return Uri.parse(uri);
    }

    public static boolean isLowMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return activityManager.isLowRamDevice() || activityManager.getLargeMemoryClass() <= 64;
    }

    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    public static float clamp(float value, float min, float max) {
        return Math.min(Math.max(value, min), max);
    }

    public static int toIntExact(long value) {
        if ((int) value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) value;
    }

    public static boolean isStringEquals(String first, String second) {
        if (first == null) return second == null;
        return first.equals(second);
    }

    public static boolean isEquals(@Nullable Long first, long second) {
        return first != null && first == second;
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    public static <T> List<T> concatenatedList(List<T> first, List<T> second) {
        final List<T> concat = new ArrayList<>(first.size() + second.size());

        concat.addAll(first);
        concat.addAll(second);

        return concat;
    }

    public static <T> List<T> getAndClearList(ConcurrentLinkedQueue<T> queue) {
        List<T> list = new ArrayList<>();
        while (!queue.isEmpty()) {
            T item = queue.poll();
            if (item != null) {
                list.add(item);
            }
        }
        return list;
    }

}
