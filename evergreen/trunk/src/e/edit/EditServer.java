package e.edit;

import java.io.*;
import java.net.*;
import javax.swing.*;

import e.util.*;

public class EditServer extends Thread {
    private ServerSocket socket;
    
    public EditServer() throws IOException {
        socket = new ServerSocket(1948);
        setName("EditServer");
        start();
    }
    
    public void run() {
        for (;;) {
            try {
                Socket client = socket.accept();
                handleClient(client);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void handleClient(Socket client) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
            handleCommand(in, out);
            out.flush();
            out.close();
            in.close();
            client.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void handleCommand(final BufferedReader in, final PrintWriter out) throws IOException {
        String line = in.readLine();
        if (line == null || line.length() == 0) {
            Log.warn("EditServer ignoring empty request");
            return;
        }
        if (line.startsWith("open ")) {
            String filename = line.substring("open ".length());
            open(out, filename);
        } else {
            out.println("EditServer: didn't understand request \"" + line + "\".");
        }
    }
    
    public void open(final PrintWriter out, final String filename) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        Edit.openFileNonInteractively(filename);
                        Edit.getFrame().toFront();
                        out.println("File '" + filename + "' opened OK.");
                    } catch (Exception ex) {
                        out.println(ex.getMessage());
                    }
                }
            });
        } catch (Exception ex) {
            out.println(ex.getMessage());
        }
    }
}
