package app.netfilter.proxy;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;

import app.common.Utils;
import app.common.debug.L;
import app.internal.Settings;

public class TunWriteThread extends Thread {
    public static final int WRITE_RETRY_MAX = 1;

    private final IThreadEventListener listener;
    private final FileOutputStream out;
    private ProxyWorker worker;
    private volatile boolean interrupted = false;

    private ArrayDeque<Packet> packetsCache = null;
    private final ArrayDeque<ArrayDeque<Packet>> writeQueue = new ArrayDeque();

    public TunWriteThread(FileDescriptor fd, ProxyWorker worker, IThreadEventListener listener) {
        out = new FileOutputStream(fd);
        this.worker = worker;
        this.listener = listener;
    }

    public static TunWriteThread create(FileDescriptor fd, ProxyWorker worker, IThreadEventListener listener,
                                        String name) {
        TunWriteThread thread = new TunWriteThread(fd, worker, listener);
        thread.setName(name);
        thread.start();

        return thread;
    }

    public void cancel() {
        if (interrupted)
            return;
        interrupted = true;

        synchronized (writeQueue) {
            writeQueue.notify();
        }

        L.i(Settings.TAG_TUNWRITETHREAD, "Cancelled");
    }

    @Override
    public void run() {
        Utils.maximizeThreadPriority();

        ArrayDeque<Packet> packets;
        boolean exception = false;

        while (true) {
            Packet packet = null;
            int retryCount = 0;
            int size;

            synchronized (writeQueue) {
                //int size = writeQueue.size();
                //L.d(Settings.TAG_TUNWRITETHREAD, "writeQueue.size() = " + writeQueue.size());
                packets = writeQueue.pollFirst();
            }

            while (!exception && packets != null) {
                //L.d(Settings.TAG_TUNWRITETHREAD, "packets.size() = " + packets.size());

                while (!exception && (packet != null || (packet = packets.pollFirst()) != null)) {
                    try {
                        size = packet.frameLen;
                        if (size > 0) {
                            worker.pktDump(packet, false);

                            out.write(packet.frame, 0, size);
                        }
                    } catch (IOException e) {
                        //L.w(Settings.TAG_TUNWRITETHREAD, packet.toString());
                        //e.printStackTrace();

                        final String msg = e.getMessage();
                        if (msg != null && msg.indexOf("ENOBUFS") >= 0) {
                            // java.io.IOException: write failed: ENOBUFS (No buffer space available)
                            //L.a(Settings.TAG_TUNWRITETHREAD, "retry " + packet.frameLen);
                            retryCount++;
                        } else {
                            e.printStackTrace();
                            exception = true;
                        }
                    }

                    if (retryCount == 0 || retryCount > WRITE_RETRY_MAX) {
                        //if (retryCount > WRITE_RETRY_MAX) L.a(Settings.TAG_TUNWRITETHREAD, "retry drop");
                        PacketPool.release(packet);
                        packet = null;
                        retryCount = 0;
                    }
                }

                PacketDequePool.release(packets);

                // next
                synchronized (writeQueue) {
                    packets = writeQueue.pollFirst();
                }
            }

            synchronized (writeQueue) {
                if (interrupted || exception) // isInterrupted()
                {
                    writeQueue.clear();
                    break; // exit
                }

                //L.e(Settings.TAG_TUNWRITETHREAD, "Waiting on queue...");
                try {
                    writeQueue.wait();
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    //L.e(Settings.TAG_TUNWRITETHREAD, "Thread interrupted.");
                }
            }
        }

        //L.e(Settings.TAG_TUNWRITETHREAD, "Waiting finished!");

        try {
            out.close();
        } catch (IOException e) { /*if (!interrupted) e.printStackTrace();*/ }
        worker = null;

        if (listener != null)
            listener.onThreadFinished((exception && !interrupted));

        L.e(Settings.TAG_TUNWRITETHREAD, "Finished");
    }

    /*
     * write packets to cache (and write cache to tun at specified size)
     * also see forceWrite
     *
     * NOT THREAD SAFE
     */
    public void write(Packet packet) {
        if (packet == null)
            return;

        int size = PacketDequePool.DEQUE_CAPACITY;
//		  if (packet.protocol == Packet.IP_PROT_UDP)
//			  size = 5;

        if (packetsCache == null)
            packetsCache = PacketDequePool.alloc();

        packetsCache.addLast(packet);
        if (packetsCache.size() >= size) {
            writePackets(packetsCache);
            packetsCache = null;
        }
    }

    public void flush() {
        if (packetsCache != null) {
            writePackets(packetsCache);
            packetsCache = null;
        }
    }

    public void writePackets(ArrayDeque packets) {
        if (packets == null)
            return;

        synchronized (writeQueue) {
            writeQueue.addLast(packets);
            writeQueue.notify();
        }
    }
}
