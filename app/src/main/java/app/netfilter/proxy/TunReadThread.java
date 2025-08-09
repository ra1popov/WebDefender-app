package app.netfilter.proxy;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import app.common.LibNative;
import app.common.Utils;
import app.common.debug.L;
import app.internal.Settings;

class TunReadThread extends Thread {

    private final FileInputStream in;
    private final IThreadEventListener listener;
    private final ReentrantLock lock = new ReentrantLock();
    private ProxyWorker worker;
    public volatile long Id = 0;
    private volatile boolean interrupted = false;

    Packet[] packets = null;
    private final ArrayList<Packet> readQueue = new ArrayList(100);

    public TunReadThread(FileDescriptor fd, ProxyWorker worker, IThreadEventListener eventListener) {
        this.in = new FileInputStream(fd);
        this.worker = worker;
        this.listener = eventListener;
    }

    public static TunReadThread create(FileDescriptor fd, ProxyWorker worker, IThreadEventListener eventListener,
                                       String name) {
        TunReadThread thread = new TunReadThread(fd, worker, eventListener);
        thread.setName(name);
        thread.start();

        return thread;
    }

    public void cancel() {
        if (interrupted)
            return;
        interrupted = true;

        // nothing of this didn't work util pfd not closed !!!! (see ProxyWorker.stop)
        //sendDummyPacket();
        interrupt();
        interruptBySignal();

        L.i(Settings.TAG_TUNREADTHREAD, "Cancelled");
    }

    // interrupt in.read call by sending signal to tunWriteThread
    public void interruptBySignal() {
        if (Id != 0)
            LibNative.threadSendSignal(Id, 1); // TODO XXX may CRASH!
    }

    public void run() {
        //L.i(Settings.TAG_TUNREADTHREAD, "TID java " + Thread.currentThread().getId());
        //L.i(Settings.TAG_TUNREADTHREAD, "TID native " + LibNative.threadGetSelfId());

        Utils.maximizeThreadPriority();

        boolean exception = false;
        Id = LibNative.threadGetSelfId(); // we can use Process.myTid

        lock.lock();
        try {
            while (true) {
                // TODO XXX why 4kb? because we use mtu 4kb?
                Packet packet = PacketPool.alloc(PacketPool.POOL3_PACKET_SIZE);

                int size;
                try {
                    size = in.read(packet.frame);
                } catch (IOException e) {
                    e.printStackTrace();
                    size = 0;
                    exception = true;
                }

                if (interrupted || exception) {
                    PacketPool.release(packet);
                    break;
                } else if (size <= 0) {
                    L.i(Settings.TAG_TUNREADTHREAD, "Read zero size");

                    PacketPool.release(packet);
                    continue;
                }

                //
                if (Settings.DEBUG_NET) L.a(Settings.TAG_TUNREADTHREAD, "New packet has been read! The size is: " + size);

                packet.frameLen = size;

                worker.pktDump(packet, true);

                synchronized (readQueue) // TODO XXX rewrite without this lock?
                {
                    readQueue.add(packet);
                }
                worker.selectorWakeup();
            }

            try {
                in.close();
            } catch (IOException e) {
            }
            worker = null;
            Id = 0;
        } finally {
            lock.unlock();
        }

        if (listener != null)
            listener.onThreadFinished((exception && !interrupted));

        L.e(Settings.TAG_TUNREADTHREAD, "Finished");
    }

    private void sendDummyPacket() {
        InetSocketAddress addr = new InetSocketAddress("8.8.8.8", 53);
        byte[] arrayOfByte = "".getBytes();
        try {
            DatagramPacket packet = new DatagramPacket(arrayOfByte, arrayOfByte.length, addr);
            new DatagramSocket().send(packet);
        } catch (SocketException e) {
            //L.i(Settings.TAG_TUNREADTHREAD, "Caught exception: " + e);
        } catch (IOException e1) {
            //L.i(Settings.TAG_TUNREADTHREAD, "Caught exception: " + e1);
        }
    }

    public int getQueueSize() {
        synchronized (readQueue) {
            return readQueue.size();
        }
    }

    /*
     * return array with packets (last element == null) or null
     * this packet contains valid data only until next getPackets call. call clearPackets after processing.
     *
     * NOT THREAD SAFE
     *
     * TODO XXX test cpu perfomance
     */
    public Packet[] getPackets() {
        synchronized (readQueue) {
            int size = readQueue.size();
            if (size == 0)
                return null;

            if (packets == null || packets.length < size + 1)
                packets = new Packet[size + 1];

            int i = 0;
            while (size-- != 0)
                packets[i++] = readQueue.remove(0);
            packets[i] = null;

            return packets;
        }
    }

    public void clearPackets() {
        Packet packet;
        int i = 0;

        while ((packet = packets[i++]) != null)
            PacketPool.release(packet);
    }
}
