package e.util;

import java.io.*;
import java.net.*;

/**
 * If your application wants to provide a simple server that users (or scripts)
 * can connect to and make requests, you might find this handy. Simply override
 * handleCommand to do some work, and instantiate! Edit uses this so you can
 * open files from the shell, and Terminator uses it so that successive
 * invocations don't need to start a new VM.
 */
public abstract class InAppServer extends Thread {
    private ServerSocket socket;
    
    /**
     * Override this and return true if you were able to interpret the
     * command, false otherwise.
     */
    public abstract boolean handleCommand(String line, PrintWriter out);
    
    public InAppServer(String name, int port) {
        setName(name + "Server");
        try {
            this.socket = new ServerSocket(port);
            start();
        } catch (Throwable th) {
            Log.warn("Couldn't start " + getName(), th);
        }
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
                handleRequest();
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
        
        private void handleRequest() throws IOException {
            String line = in.readLine();
            if (line == null || line.length() == 0) {
                Log.warn(getName() + " ignoring empty request");
                return;
            }
            if (handleCommand(line, out) == false) {
                out.println(getName() + ": didn't understand request \"" + line + "\".");
            }
        }
    }
}
