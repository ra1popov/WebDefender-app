
package org.squareup.okio;

import android.net.VpnService;

import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Iterator;

import app.common.debug.L;
import app.internal.Settings;

/*
 * this class is used to:
 * - protect sockets to skip VPN if VPN established
 * - register/unregister not protected sockets if VPN disabled
 * - protect not protected sockets when VPN established
 */

public class VpnSocketEnumerator
{
    private static final boolean ENABLED = true;

    private static VpnService vpn = null;
    private static final HashSet sockets = new HashSet(); // not protected sockets

    public static void setVpnService(VpnService vpn)
    {
        if (!ENABLED)
            return;

        synchronized (sockets)
        {
            VpnSocketEnumerator.vpn = vpn;
            if (vpn == null || sockets.size() == 0)
                return;

            // protect all not protected sockets
            Iterator iterator = sockets.iterator();
            while (iterator.hasNext())
            {
                Socket socket = (Socket) iterator.next();
                protect(socket);
                iterator.remove(); // removes current element
            }
        }
    }

    // protect socket if VPN established or save it
    public static void registerSocket(Socket socket)
    {
        if (!ENABLED)
            return;

        synchronized (sockets)
        {
            if (vpn != null)
            {
                protect(socket);
                return; // ok, protected
            }

            // or save
            sockets.add(socket);
        }
    }

    public static void unregisterSocket(Socket socket)
    {
        if (!ENABLED)
            return;

        synchronized (sockets)
        {
            sockets.remove(socket);
        }
    }

    private static boolean protect(Socket socket)
    {
        // when socket object only created, internal socket impl. not created
        // force to create the underlying SocketImpl
        try { socket.getKeepAlive(); } catch (SocketException ex) { }

        boolean result = vpn.protect(socket);
        if (!result)
            L.a(Settings.TAG_VPNSOCKET, "protect failed");

        return result; // TODO XXX may be check this function result?
    }
}
