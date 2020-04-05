/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;


import androidx.annotation.NonNull;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Vector;

import ht.vpn.android.BuildConfig;
import ht.vpn.android.R;
import ht.vpn.android.VpnProfile;
import de.blinkt.openvpn.core.VpnStatus.ConnectionStatus;

public class OpenVpnManagementThread implements Runnable, OpenVPNManagement {

    private static final String TAG = "openvpn";
    private LocalSocket mSocket;
    private VpnProfile mProfile;
    private OpenVPNService mOpenVPNService;
    private LinkedList<FileDescriptor> mFDList = new LinkedList<FileDescriptor>();
    private LocalServerSocket mServerSocket;
    private boolean mReleaseHold = true;
    private boolean mWaitingForRelease = false;
    private long mLastHoldRelease = 0;

    private static final Vector<OpenVpnManagementThread> active = new Vector<OpenVpnManagementThread>();
    private LocalSocket mServerSocketLocal;

    private pauseReason lastPauseReason = pauseReason.noNetwork;

    public OpenVpnManagementThread(VpnProfile profile, OpenVPNService openVpnService) {
        mProfile = profile;
        mOpenVPNService = openVpnService;


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(openVpnService);
        boolean managemeNetworkState = prefs.getBoolean("netchangereconnect", true);
        if (managemeNetworkState)
            mReleaseHold = false;

    }

    public boolean openManagementInterface(@NonNull Context c) {
        // Could take a while to open connection
        int tries = 8;

        String socketName = (c.getCacheDir().getAbsolutePath() + "/" + "mgmtsocket");
        // The mServerSocketLocal is transferred to the LocalServerSocket, ignore warning

        mServerSocketLocal = new LocalSocket();

        while (tries > 0 && !mServerSocketLocal.isConnected()) {
            try {
                mServerSocketLocal.bind(new LocalSocketAddress(socketName,
                        LocalSocketAddress.Namespace.FILESYSTEM));
            } catch (IOException e) {
                // wait 300 ms before retrying
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e1) {
                }

            }
            tries--;
        }

        try {

            mServerSocket = new LocalServerSocket(mServerSocketLocal.getFileDescriptor());
            return true;
        } catch (IOException e) {
            VpnStatus.logException(e);
        }
        return false;


    }

    public void managmentCommand(String cmd) {
        try {
            if (mSocket != null && mSocket.getOutputStream() != null) {
                mSocket.getOutputStream().write(cmd.getBytes());
                mSocket.getOutputStream().flush();
            }
        } catch (IOException e) {
            // Ignore socket stack traces
        }
    }


    @Override
    public void run() {
        byte[] buffer = new byte[2048];
        //	mSocket.setSoTimeout(5); // Setting a timeout cannot be that bad

        String pendingInput = "";
        synchronized (active) {
            active.add(this);
        }

        try {
            // Wait for a client to connect
            mSocket = mServerSocket.accept();
            InputStream instream = mSocket.getInputStream();
            // Close the management socket after client connected

            mServerSocket.close();
            // Closing one of the two sockets also closes the other
            //mServerSocketLocal.close();

            while (true) {
                int numbytesread = instream.read(buffer);
                if (numbytesread == -1)
                    return;

                FileDescriptor[] fds = null;
                try {
                    fds = mSocket.getAncillaryFileDescriptors();
                } catch (IOException e) {
                    VpnStatus.logException("Error reading fds from socket", e);
                }
                if (fds != null) {
                    Collections.addAll(mFDList, fds);
                }

                String input = new String(buffer, 0, numbytesread, "UTF-8");

                pendingInput += input;

                pendingInput = processInput(pendingInput);


            }
        } catch (IOException e) {
            if (!e.getMessage().equals("socket closed") && !e.getMessage().equals("Connection reset by peer"))
                VpnStatus.logException(e);
        }
        synchronized (active) {
            active.remove(this);
        }
    }

    //! Hack O Rama 2000!
    private void protectFileDescriptor(FileDescriptor fd) {
        Exception exp;
        try {
            Method getInt = FileDescriptor.class.getDeclaredMethod("getInt$");
            int fdint = (Integer) getInt.invoke(fd);

            // You can even get more evil by parsing toString() and extract the int from that :)

            boolean result = mOpenVPNService.protect(fdint);
            if (!result)
                VpnStatus.logWarning("Could not protect VPN socket");


            //ParcelFileDescriptor pfd = ParcelFileDescriptor.fromFd(fdint);
            //pfd.close();
            NativeUtils.jniclose(fdint);
            return;
        } catch (NoSuchMethodException e) {
            exp = e;
        } catch (IllegalArgumentException e) {
            exp = e;
        } catch (IllegalAccessException e) {
            exp = e;
        } catch (InvocationTargetException e) {
            exp = e;
        } catch (NullPointerException e) {
            exp = e;
        }

        Log.d("Openvpn", "Failed to retrieve fd from socket: " + fd);
        VpnStatus.logException("Failed to retrieve fd from socket (" + fd + ")", exp);
    }

    private String processInput(String pendingInput) {


        while (pendingInput.contains("\n")) {
            String[] tokens = pendingInput.split("\\r?\\n", 2);
            processCommand(tokens[0]);
            if (tokens.length == 1)
                // No second part, newline was at the end
                pendingInput = "";
            else
                pendingInput = tokens[1];
        }
        return pendingInput;
    }


    private void processCommand(String command) {
        //Log.i(TAG, "Line from managment" + command);


        if (command.startsWith(">") && command.contains(":")) {
            String[] parts = command.split(":", 2);
            String cmd = parts[0].substring(1);
            String argument = parts[1];


            if (cmd.equals("INFO")) {
                /* Ignore greeting from management */
                return;
            } else if (cmd.equals("PASSWORD")) {
                processPWCommand(argument);
            } else if (cmd.equals("HOLD")) {
                handleHold();
            } else if (cmd.equals("NEED-OK")) {
                processNeedCommand(argument);
            } else if (cmd.equals("BYTECOUNT")) {
                processByteCount(argument);
            } else if (cmd.equals("STATE")) {
                processState(argument);
            } else if (cmd.equals("PROXY")) {
                processProxyCMD(argument);
            } else if (cmd.equals("LOG")) {
                processLogMessage(argument);
            } else if (cmd.equals("RSA_SIGN")) {
                processSignCommand(argument);
            } else {
                VpnStatus.logWarning("MGMT: Got unrecognized command" + command);
                Log.i(TAG, "Got unrecognized command" + command);
            }
        } else if (command.startsWith("SUCCESS:")) {
            /* Ignore this kind of message too */
            return;
        } else if (command.startsWith("PROTECTFD: ")) {
            FileDescriptor fdtoprotect = mFDList.pollFirst();
            if (fdtoprotect != null)
                protectFileDescriptor(fdtoprotect);
        } else {
            Log.i(TAG, "Got unrecognized line from managment" + command);
            VpnStatus.logWarning("MGMT: Got unrecognized line from management:" + command);
        }
    }

    private void processLogMessage(String argument) {
        String[] args = argument.split(",", 4);
        // 0 unix time stamp
        // 1 log level N,I,E etc.
                /*
                  (b) zero or more message flags in a single string:
          I -- informational
          F -- fatal error
          N -- non-fatal error
          W -- warning
          D -- debug, and
                 */
        // 2 log message

        Log.d("OpenVPN", argument);

        VpnStatus.LogLevel level;
        if (args[1].equals("I")) {
            level = VpnStatus.LogLevel.INFO;
        } else if (args[1].equals("W")) {
            level = VpnStatus.LogLevel.WARNING;
        } else if (args[1].equals("D")) {
            level = VpnStatus.LogLevel.VERBOSE;
        } else if (args[1].equals("F")) {
            level = VpnStatus.LogLevel.ERROR;
        } else {
            level = VpnStatus.LogLevel.INFO;
        }

        int ovpnlevel = Integer.parseInt(args[2]) & 0x0F;
        String msg = args[3];

        if (msg.startsWith("MANAGEMENT: CMD"))
            ovpnlevel = Math.max(4, ovpnlevel);

        VpnStatus.logMessageOpenVPN(level, ovpnlevel, msg);
    }

    private void handleHold() {
        if (mReleaseHold) {
            releaseHoldCmd();
        } else {
            mWaitingForRelease = true;

            VpnStatus.updateStatePause(lastPauseReason);


        }
    }

    private void releaseHoldCmd() {
        if ((System.currentTimeMillis() - mLastHoldRelease) < 5000) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }

        }
        mWaitingForRelease = false;
        mLastHoldRelease = System.currentTimeMillis();
        managmentCommand("hold release\n");
        managmentCommand("bytecount " + mBytecountInterval + "\n");
        managmentCommand("state on\n");
        //managmentCommand("log on all\n");
    }

    public void releaseHold() {
        mReleaseHold = true;
        if (mWaitingForRelease)
            releaseHoldCmd();

    }

    private void processProxyCMD(String argument) {
        String[] args = argument.split(",", 3);
        SocketAddress proxyaddr = ProxyDetection.detectProxy(mProfile);


        if (args.length >= 2) {
            String proto = args[1];
            if (proto.equals("UDP")) {
                proxyaddr = null;
            }
        }

        if (proxyaddr instanceof InetSocketAddress) {
            InetSocketAddress isa = (InetSocketAddress) proxyaddr;

            VpnStatus.logInfo(R.string.using_proxy, isa.getHostName(), isa.getPort());

            String proxycmd = String.format(Locale.ENGLISH, "proxy HTTP %s %d\n", isa.getHostName(), isa.getPort());
            managmentCommand(proxycmd);
        } else {
            managmentCommand("proxy NONE\n");
        }

    }

    private void processState(String argument) {
        String[] args = argument.split(",", 3);
        String currentstate = args[1];

        if (args[2].equals(",,"))
            VpnStatus.updateStateString(currentstate, "");
        else
            VpnStatus.updateStateString(currentstate, args[2]);
    }


    private void processByteCount(String argument) {
        //   >BYTECOUNT:{BYTES_IN},{BYTES_OUT}
        int comma = argument.indexOf(',');
        long in = Long.parseLong(argument.substring(0, comma));
        long out = Long.parseLong(argument.substring(comma + 1));

        VpnStatus.updateByteCount(in, out);

    }


    private void processNeedCommand(String argument) {
        int p1 = argument.indexOf('\'');
        int p2 = argument.indexOf('\'', p1 + 1);

        String needed = argument.substring(p1 + 1, p2);
        String extra = argument.split(":", 2)[1];

        String status = "ok";


        if (needed.equals("PROTECTFD")) {
            FileDescriptor fdtoprotect = mFDList.pollFirst();
            protectFileDescriptor(fdtoprotect);
        } else if (needed.equals("DNSSERVER")) {
            mOpenVPNService.addDNS(extra);
        } else if (needed.equals("DNSDOMAIN")) {
            mOpenVPNService.setDomain(extra);
        } else if (needed.equals("ROUTE")) {
            String[] routeparts = extra.split(" ");

            /*
            buf_printf (&out, "%s %s %s dev %s", network, netmask, gateway, rgi->iface);
            else
            buf_printf (&out, "%s %s %s", network, netmask, gateway);
            */

            if (routeparts.length == 5) {
                mOpenVPNService.addRoute(routeparts[0], routeparts[1], routeparts[2], routeparts[4]);
            } else if (routeparts.length >= 3) {
                mOpenVPNService.addRoute(routeparts[0], routeparts[1], routeparts[2], null);
            } else {
                VpnStatus.logError("Unrecognized ROUTE cmd:" + Arrays.toString(routeparts) + " | " + argument);
            }

        } else if (needed.equals("ROUTE6")) {
            String[] routeparts = extra.split(" ");
            mOpenVPNService.addRoutev6(routeparts[0], routeparts[1]);
        } else if (needed.equals("IFCONFIG")) {
            String[] ifconfigparts = extra.split(" ");
            int mtu = Integer.parseInt(ifconfigparts[2]);
            mOpenVPNService.setLocalIP(ifconfigparts[0], ifconfigparts[1], mtu, ifconfigparts[3]);
        } else if (needed.equals("IFCONFIG6")) {
            mOpenVPNService.setLocalIPv6(extra);

        } else if (needed.equals("PERSIST_TUN_ACTION")) {
            // check if tun cfg stayed the same
            status = mOpenVPNService.getTunReopenStatus();
        } else if (needed.equals("OPENTUN")) {
            if (sendTunFD(needed, extra))
                return;
            else
                status = "cancel";
            // This not nice or anything but setFileDescriptors accepts only FilDescriptor class :(

        } else {
            Log.e(TAG, "Unkown needok command " + argument);
            return;
        }

        String cmd = String.format("needok '%s' %s\n", needed, status);
        managmentCommand(cmd);
    }

    private boolean sendTunFD(String needed, String extra) {
        Exception exp;
        if (!extra.equals("tun")) {
            // We only support tun
            VpnStatus.logError(String.format("Device type %s requested, but only tun is possible with the Android API, sorry!", extra));

            return false;
        }
        ParcelFileDescriptor pfd = mOpenVPNService.openTun();
        if (pfd == null)
            return false;

        Method setInt;
        int fdint = pfd.getFd();
        try {
            setInt = FileDescriptor.class.getDeclaredMethod("setInt$", int.class);
            FileDescriptor fdtosend = new FileDescriptor();

            setInt.invoke(fdtosend, fdint);

            FileDescriptor[] fds = {fdtosend};
            mSocket.setFileDescriptorsForSend(fds);

            // Trigger a send so we can close the fd on our side of the channel
            // The API documentation fails to mention that it will not reset the file descriptor to
            // be send and will happily send the file descriptor on every write ...
            String cmd = String.format("needok '%s' %s\n", needed, "ok");
            managmentCommand(cmd);

            // Set the FileDescriptor to null to stop this mad behavior
            mSocket.setFileDescriptorsForSend(null);

            pfd.close();

            return true;
        } catch (NoSuchMethodException e) {
            exp = e;
        } catch (IllegalArgumentException e) {
            exp = e;
        } catch (IllegalAccessException e) {
            exp = e;
        } catch (InvocationTargetException e) {
            exp = e;
        } catch (IOException e) {
            exp = e;
        }
        VpnStatus.logException("Could not send fd over socket", exp);

        return false;
    }

    private void processPWCommand(String argument) {
        //argument has the form 	Need 'Private Key' password
        // or  ">PASSWORD:Verification Failed: '%s' ['%s']"
        String needed;


        try {

            int p1 = argument.indexOf('\'');
            int p2 = argument.indexOf('\'', p1 + 1);
            needed = argument.substring(p1 + 1, p2);
            if (argument.startsWith("Verification Failed")) {
                proccessPWFailed(needed, argument.substring(p2 + 1));
                return;
            }
        } catch (StringIndexOutOfBoundsException sioob) {
            VpnStatus.logError("Could not parse management Password command: " + argument);
            return;
        }

        String pw = null;

        if (needed.equals("Private Key")) {
            pw = mProfile.getPasswordPrivateKey();
        } else if (needed.equals("Auth")) {
            String usercmd = String.format("username '%s' %s\n",
                    needed, VpnProfile.openVpnEscape(mProfile.mUsername));
            managmentCommand(usercmd);
            pw = mProfile.getPasswordAuth();
        }
        if (pw != null) {
            String cmd = String.format("password '%s' %s\n", needed, VpnProfile.openVpnEscape(pw));
            managmentCommand(cmd);
        } else {
            VpnStatus.logError(String.format("Openvpn requires Authentication type '%s' but no password/key information available", needed));
        }

    }


    private void proccessPWFailed(String needed, String args) {
        VpnStatus.updateStateString("AUTH_FAILED", needed + args, R.string.state_auth_failed, ConnectionStatus.LEVEL_AUTH_FAILED);
    }


    private static boolean stopOpenVPN() {
        synchronized (active) {
            boolean sendCMD = false;
            for (OpenVpnManagementThread mt : active) {
                mt.managmentCommand("signal SIGINT\n");
                sendCMD = true;
                try {
                    if (mt.mSocket != null)
                        mt.mSocket.close();
                } catch (IOException e) {
                    // Ignore close error on already closed socket
                }
            }
            return sendCMD;
        }
    }

    @Override
    public void networkChange(boolean samenetwork) {
        if (!mWaitingForRelease)
            if (samenetwork)
                managmentCommand("network-change samenetwork\n");
            else
                managmentCommand("network-change\n");
    }

    public void signalusr1() {
        mReleaseHold = false;

        if (!mWaitingForRelease)
            managmentCommand("signal SIGUSR1\n");
        else
            // If signalusr1 is called update the state string
            // if there is another for stopping
            VpnStatus.updateStatePause(lastPauseReason);
    }

    public void reconnect() {
        signalusr1();
        releaseHold();
    }

    private void processSignCommand(String b64data) {

        String signed_string = mProfile.getSignedData(b64data);
        if (signed_string == null) {
            managmentCommand("rsa-sig\n");
            managmentCommand("\nEND\n");
            stopOpenVPN();
            return;
        }
        managmentCommand("rsa-sig\n");
        managmentCommand(signed_string);
        managmentCommand("\nEND\n");
    }

    @Override
    public void pause(pauseReason reason) {
        lastPauseReason = reason;
        signalusr1();
    }

    @Override
    public void resume() {
        releaseHold();
        /* Reset the reason why we are disconnected */
        lastPauseReason = pauseReason.noNetwork;
    }

    @Override
    public boolean stopVPN() {
        return stopOpenVPN();
    }
}
