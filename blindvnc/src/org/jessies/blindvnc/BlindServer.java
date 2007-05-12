package org.jessies.blindvnc;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;

public class BlindServer implements Runnable {
    private static final String PROTOCOL_VERSION = "RFB 003.003\n";
    private static final int NO_AUTHENTICATION = 1;
    
    private static final int MSG_SET_PIXEL_FORMAT = 0;
    private static final int MSG_FIX_COLOR_MAP_ENTRIES = 1;
    private static final int MSG_SET_ENCODINGS = 2;
    private static final int MSG_UPDATE_REQUEST = 3;
    private static final int MSG_KEY_EVENT = 4;
    private static final int MSG_MOUSE_EVENT = 5;
    private static final int MSG_CLIENT_CUT_TEXT = 6;
    
    private Desktop desktop;
    private DataInputStream in;
    private DataOutputStream out;
    private Options options;
    private KeyCodeTranslator keyCodeTranslator = new KeyCodeTranslator();
    
    public BlindServer(InputStream in, OutputStream out, Options options) throws AWTException {
        this.in = new DataInputStream(in);
        this.out = new DataOutputStream(out);
        this.options = options;
        desktop = new Desktop();
    }
    
    public void debug(String message) {
        if (options.getBooleanOption(BlindVNC.OPTION_DEBUG)) {
            System.err.println("DEBUG: " + message);
        }
    }
    
    public void run() {
        try {
            out.writeBytes(PROTOCOL_VERSION);
            debug("Got protocol version (ignoring)");
            out.flush();
            discardBytes(12);  // Ignore the client's reply of required protocol version.
            out.writeInt(NO_AUTHENTICATION);
            out.flush();
            discardBytes(1);  // The client attempts to tell us to kick others off.  We ignore it.
            debug("Sending server initialisation");
            sendServerInitialization();
            debug("Finished initialization");
            while (true) {
                readMessageFromClient();
            }
        } catch (Exception ex) {
            System.err.println("Terminating connection");
            ex.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException ignoreMe) {
                return;
            }
        }
    }
    
    private void readMessageFromClient() throws IOException {
        byte messageType = in.readByte();
        switch (messageType) {
        case MSG_SET_PIXEL_FORMAT:
            discardBytes(3 + 16);  // Ignore padding plus PIXEL_FORMAT data.
            break;
            
        case MSG_FIX_COLOR_MAP_ENTRIES:
            discardBytes(1 + 2);  // Ignore padding plus first color.
            int numberOfColors = readUnsignedShort();
            discardBytes(numberOfColors * 6);
            break;
            
        case MSG_SET_ENCODINGS:
            discardBytes(1);  // Ignore padding.
            int numberOfEncodings = readUnsignedShort();
            discardBytes(numberOfEncodings * 4);
            break;
            
        case MSG_UPDATE_REQUEST:
            discardBytes(1 + 2 * 4);  // Ignore padding, and x, y, w, h (16 bits each).
            break;
            
        case MSG_KEY_EVENT:
            boolean isDown = (readUnsignedByte() != 0);
            discardBytes(2);  // Ignore padding.
            int vncKeyCode = in.readInt();
            desktop.fireKeyEvent(keyCodeTranslator.getJavaKeyCode(vncKeyCode), isDown);
            break;
            
        case MSG_MOUSE_EVENT:
            // Mouse buttons 1, 2 and 3 are normal mouse buttons.  Special magic
            // fake mouse buttons 4 and 5 are upwards and downwards on the scroll
            // wheel (respectively).  Each scroll movement is represented by a button-down
            // followed by a button-up event for the relevant button.
            int buttonMask = readUnsignedByte();
            int x = readUnsignedShort();
            int y = readUnsignedShort();
            // There seems to be a bug in x2vnc, whereby the x coordinate is sometimes 65535.
            if (x == 65535) {
                x = 0;
            }
            if (y == 65535) {
                y = 0;
            }
            desktop.fireMouseEvent(x, y, buttonMask);
            break;
            
        case MSG_CLIENT_CUT_TEXT:
            discardBytes(3);  // Ignore padding.
            int length = in.readInt();
            byte[] buffer = new byte[length];
            in.readFully(buffer);
            fireClientCutText(new String(buffer));
            break;
            
        default:
            System.err.printf("Warning: unrecognised command %x", messageType);
            break;
        }
    }
    
    private void fireClientCutText(String buffer) {
        if (buffer.length() > 0) {
            StringSelection selection = new StringSelection(buffer);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemSelection();
            if (clipboard != null) {
                clipboard.setContents(selection, new ClipboardOwner() {
                    public void lostOwnership(Clipboard clipboard, Transferable contents) { }
                });
            } else {
                System.err.println("Could not find clipboard.");
            }
        }
    }
    
    private int readUnsignedShort() throws IOException {
        return ((int) in.readShort()) & 0xffff;
    }
    
    private int readUnsignedByte() throws IOException {
        return ((int) in.readByte()) & 0xff;
    }
    
    private void sendServerInitialization() throws IOException {
        Dimension size = desktop.getSize();
        out.writeShort(size.width);
        out.writeShort(size.height);
        out.write(new byte[16]);  // PIXEL_FORMAT - just send a load of zeroes: we don't do visual stuff.
        out.writeInt(1);  // Length of name string.
        out.writeBytes("0");
        out.flush();
    }
    
    private void discardBytes(int count) throws IOException {
        byte[] bytes = new byte[count];
        in.read(bytes);
    }
}
