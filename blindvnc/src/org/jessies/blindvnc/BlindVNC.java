package org.jessies.blindvnc;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

import java.util.List;

/**
 * BlindVNC: a faceless VNC server for use with x2vnc.
 * 
 * Things to be done:
 *   Support the basic mouse movement, key press stuff coming from the client.
 *   Notice when text has been put in the clipboard on our side, and send a message to the client.
 */

public class BlindVNC {
    public static final String OPTION_DEBUG = "debug";
    public static final String OPTION_PORT = "port";
    public static final String OPTION_BIND_ADDRESS = "bind-address";
    
    private static final int DEFAULT_PORT = 5900;
    
    private static final Options.Option[] VNC_OPTIONS = new Options.Option[] {
        new Options.Option(OPTION_DEBUG, false),
        new Options.Option(OPTION_PORT, 5900),
        new Options.Option(OPTION_BIND_ADDRESS, ""),
    };
    
    public static void main(String[] arguments) {
        Options options = new Options(VNC_OPTIONS);
        List<String> unparsedOptions = options.parseDashDashArguments(arguments);
        System.err.println("Got " + unparsedOptions.size() + " unparsed options:");
        for (String opt : unparsedOptions) {
            System.err.println("    " + opt);
        }
        options.dump();
        try {
            int port = options.getIntOption(OPTION_PORT);
            ServerSocket sock;
            String bindAddress = options.getStringOption(OPTION_BIND_ADDRESS);
            if (bindAddress.equals("")) {
                sock = new ServerSocket(port);
            } else {
                InetAddress addr = InetAddress.getByName(bindAddress);
                // Backlog of 10 - we don't care really.
                sock = new ServerSocket(port, 10, addr);
            }
            System.err.println("Server started up on port " + port);
            while (true) {
                Socket socket = sock.accept();
                System.err.println("Accepted connection");
                BlindServer server = new BlindServer(socket.getInputStream(), socket.getOutputStream(), options);
                (new Thread(server)).start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
