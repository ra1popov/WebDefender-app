package app.common.debug;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

import app.App;
import app.common.Utils;
import app.internal.Settings;
import app.netfilter.proxy.Packet;

public class L {
    private static PrintWriter log = null;
    private static PCapFileWriter pktDumper = null;
    private static final Object lock = new Object();

    static {
        if (Settings.DEBUG_WRITE) {
            String filePath = Utils.getWritablePath(Settings.APP_FILES_PREFIX + ".log");
            String timeStamp = new SimpleDateFormat().format(new Date());
            try {
                log = new PrintWriter(new FileWriter(filePath, true));
                Log.d(Settings.TAG_LOG, filePath);

                Runtime.getRuntime().exec("chmod 0666 " + filePath);
                log.println(" --- start time " + timeStamp + " ---");
                log.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (Settings.DEBUG_PKT_DUMP) {
            String filePath = Utils.getWritablePath(Settings.APP_FILES_PREFIX + ".pcap");
            String timeStamp = new SimpleDateFormat().format(new Date());
            try {
                File file = new File(filePath);
                pktDumper = new PCapFileWriter(file, false);
                Log.d(Settings.TAG_PKTDUMP, filePath);

                Runtime.getRuntime().exec("chmod 0666 " + filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void printBacktrace(final String tag, char type) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement ste : stack) {
            String s = "	" + ste.toString();
            switch (type) {
                case 'i':
                    Log.i(tag, s);
                    break;
                case 'w':
                    Log.w(tag, s);
                    break;
                case 'e':
                    Log.e(tag, s);
                    break;
                case 'a':
                    Log.i(tag, s);
                    break;

                case 'd':
                default:
                    Log.d(tag, s);
            }

            if (Settings.DEBUG_WRITE)
                if (log != null) log.println("[" + tag + "]" + type + " " + s);
        }

        if (Settings.DEBUG_WRITE)
            if (log != null) log.flush();
    }

    public static void d(final String tag, String s1) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.d(tag, s1);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]d " + s1);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'd');
            }
        }
    }

    public static void d(final String tag, String s1, String s2) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.d(tag, s1 + s2);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]d " + s1 + s2);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'd');
            }
        }
    }

    public static void d(final String tag, String s1, String s2, String s3) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.d(tag, s1 + s2 + s3);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]d " + s1 + s2 + s3);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'd');
            }
        }
    }

    public static void d(final String tag, String s1, String s2, String s3, String s4) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.d(tag, s1 + s2 + s3 + s4);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]d " + s1 + s2 + s3 + s4);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'd');
            }
        }
    }

    public static void i(final String tag, String s1) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.i(tag, s1);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]i " + s1);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'i');
            }
        }
    }

    public static void i(final String tag, String s1, String s2) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.i(tag, s1 + s2);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]i " + s1 + s2);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'i');
            }
        }
    }

    public static void i(final String tag, String s1, String s2, String s3) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.i(tag, s1 + s2 + s3);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]i " + s1 + s2 + s3);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'i');
            }
        }
    }

    public static void i(final String tag, String s1, String s2, String s3, String s4) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.i(tag, s1 + s2 + s3 + s4);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]i " + s1 + s2 + s3 + s4);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'i');
            }
        }
    }

    public static void w(final String tag, String s1) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.w(tag, s1);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]w " + s1);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'w');
            }
        }
    }

    public static void w(final String tag, String s1, String s2) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.w(tag, s1 + s2);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]w " + s1 + s2);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'w');
            }
        }
    }

    public static void w(final String tag, String s1, String s2, String s3) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.w(tag, s1 + s2 + s3);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]w " + s1 + s2 + s3);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'w');
            }
        }
    }

    public static void w(final String tag, String s1, String s2, String s3, String s4) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.w(tag, s1 + s2 + s3 + s4);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]w " + s1 + s2 + s3 + s4);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'w');
            }
        }
    }

    public static void w(final String tag, String s1, String s2, String s3, String s4, String s5) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.w(tag, s1 + s2 + s3 + s4 + s5);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]w " + s1 + s2 + s3 + s4 + s5);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'w');
            }
        }
    }

    public static void w(final String tag, String s1, String s2, String s3, String s4, String s5, String s6) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.w(tag, s1 + s2 + s3 + s4 + s5 + s6);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]w " + s1 + s2 + s3 + s4 + s5 + s6);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'w');
            }
        }
    }

    public static void w(final String tag, String s1, String s2, String s3, String s4, String s5, String s6, String s7) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.w(tag, s1 + s2 + s3 + s4 + s5 + s6 + s7);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]w " + s1 + s2 + s3 + s4 + s5 + s6 + s7);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'w');
            }
        }
    }

    public static void w(final String tag, String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.w(tag, s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]w " + s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'w');
            }
        }
    }

    public static void w(final String tag, String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8, String s9) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.w(tag, s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8 + s9);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]w " + s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8 + s9);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'w');
            }
        }
    }

    public static void e(final String tag, String s1) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.e(tag, s1);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]e " + s1);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'e');
            }
        }
    }

    public static void e(final String tag, String s1, String s2) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.e(tag, s1 + s2);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]e " + s1 + s2);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'e');
            }
        }
    }

    public static void e(final String tag, String s1, String s2, String s3) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.e(tag, s1 + s2 + s3);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]e " + s1 + s2 + s3);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'e');
            }
        }
    }

    public static void e(final String tag, String s1, String s2, String s3, String s4) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.e(tag, s1 + s2 + s3 + s4);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]e " + s1 + s2 + s3 + s4);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'e');
            }
        }
    }

    public static void e(final String tag, String s1, String s2, String s3, String s4, String s5) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.e(tag, s1 + s2 + s3 + s4 + s5);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]e " + s1 + s2 + s3 + s4 + s5);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'e');
            }
        }
    }

    public static void e(final String tag, String s1, String s2, String s3, String s4, String s5, String s6) {
        if (Settings.DEBUG) {
            synchronized (lock) {
                Log.e(tag, s1 + s2 + s3 + s4 + s5 + s6);

                if (Settings.DEBUG_WRITE) {
                    if (log == null) return;
                    log.println("[" + tag + "]e " + s1 + s2 + s3 + s4 + s5 + s6);
                    log.flush();
                }

                if (Settings.DEBUG_BT)
                    printBacktrace(tag, 'e');
            }
        }
    }

    public static void a(final String tag, String s1) {
        synchronized (lock) {
            //Log.d(tag, s1);
            Log.i(tag, s1);

            if (Settings.DEBUG_WRITE) {
                if (log == null) return;
                log.println("[" + tag + "]a " + s1);
                log.flush();
            }

            if (Settings.DEBUG_BT)
                printBacktrace(tag, 'a');
        }
    }

    public static void a(final String tag, String s1, String s2) {
        synchronized (lock) {
            //Log.d(tag, s1 + s2);
            Log.i(tag, s1 + s2);

            if (Settings.DEBUG_WRITE) {
                if (log == null) return;
                log.println("[" + tag + "]a " + s1 + s2);
                log.flush();
            }

            if (Settings.DEBUG_BT)
                printBacktrace(tag, 'a');
        }
    }

    public static void a(final String tag, String s1, String s2, String s3) {
        synchronized (lock) {
            //Log.d(tag, s1 + s2 + s3);
            Log.i(tag, s1 + s2 + s3);

            if (Settings.DEBUG_WRITE) {
                if (log == null) return;
                log.println("[" + tag + "]a " + s1 + s2 + s3);
                log.flush();
            }

            if (Settings.DEBUG_BT)
                printBacktrace(tag, 'a');
        }
    }

    public static void ar(final String tag, byte[] bytes, int pos, int sz) {
        synchronized (lock) {
            char[] hexChars = new char[sz * 2];
            final char[] hexArray = "0123456789ABCDEF".toCharArray();

            for (int i = 0; i < sz; i++, pos++) {
                int v = bytes[pos] & 0xFF;
                hexChars[i * 2] = hexArray[v >>> 4];
                hexChars[i * 2 + 1] = hexArray[v & 0x0F];
            }

            Log.i(tag, (new String(hexChars)));
        }
    }

    public static void f(String fileName, byte[] data) {
        try {
            FileOutputStream fos = new FileOutputStream(App.getContext().getFilesDir().getAbsolutePath() + "/" + fileName);
            fos.write(data);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void pkt(Packet packet, boolean input) {
        if (Settings.DEBUG_PKT_DUMP) {
            // TODO XXX very slow, allocations in getIpFrame, createIPPacketBytes, getSrcMacByteArray

            EthernetFrame full = new EthernetFrame();
            if (!input) {
                byte[] src = full.getSrcMacByteArray();
                full.setSrcMacAddress(full.getDstMacByteArray());
                full.setDstMacAddress(src);
            }

            byte[] ipdata = packet.getIpFrame();
            full.createIPPacketBytes(ipdata);
            try {
                pkt(full.getRawBytes());
            } catch (NetUtilsException e) {
                e.printStackTrace();
            }
        }
    }

    public static void pkt(byte[] packet) {
        if (Settings.DEBUG_PKT_DUMP) {
            if (pktDumper == null) return;
            try {
                synchronized (pktDumper) {
                    pktDumper.addPacket(packet);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
