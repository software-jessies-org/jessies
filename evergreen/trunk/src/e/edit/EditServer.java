package e.edit;

import java.io.*;
import java.net.*;
import javax.swing.*;

import e.util.*;

public final class EditServer extends Thread {
    private Edit edit;
    private ServerSocket socket;
    
    public EditServer(Edit edit) throws IOException {
        this.edit = edit;
        this.socket = new ServerSocket(1948);
        setName("EditServer");
        start();
    }
    
    public void run() {
        for (;;) {
            try {
                Thread thread = new Thread(new ClientHandler(socket.accept()));
                thread.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public class ClientHandler implements Runnable {
        private Socket client;
        
        private BufferedReader in;
        private PrintWriter out;
        
        public ClientHandler(Socket client) {
            this.client = client;
        }
        
        public void run() {
            handleClient();
        }
        
        private void handleClient() {
            try {
                this.in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                this.out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
                handleCommand();
                out.flush();
                out.close();
                in.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                closeClientSocket();
            }
        }
        
        private void closeClientSocket() {
            try {
                client.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        private void handleCommand() throws IOException {
            String line = in.readLine();
            if (line == null || line.length() == 0) {
                Log.warn("EditServer ignoring empty request");
                return;
            }
            if (line.startsWith("open ")) {
                String filename = line.substring("open ".length());
                handleOpen(filename);
            } else if (line.equals("remember-state")) {
                edit.rememberState();
            } else if (line.equals("save-all")) {
                SaveAllAction.saveAll(false);
            } else {
                out.println("EditServer: didn't understand request \"" + line + "\".");
            }
        }
        
        private void handleOpen(final String filename) {
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
}
