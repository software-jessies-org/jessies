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
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
                handleCommand(in, out);
                in.close();
                client.close();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }
    
    public void handleCommand(BufferedReader in, PrintWriter out) throws IOException {
        String line = in.readLine();
        if (line == null || line.length() == 0) {
            Log.warn("EditServer ignoring empty request");
            return;
        }
        if (line.startsWith("open ")) {
            final String what = line.substring("open ".length());
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Edit.getFrame().toFront();
                    Edit.openFile(what);
                }
            });
        } else {
            out.println("EditServer: didn't understand request \"" + line + "\".");
        }
        out.flush();
        out.close();
    }
}
