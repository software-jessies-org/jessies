package e.util;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

/**
 * If your application wants to provide a simple server that users (or scripts)
 * can connect to and make requests, you might find this handy. Simply supply
 * an interface listing available commands, and provide an object that
 * implements that interface to do the work, and you're off!
 * 
 * Edit uses this so you can open files from the shell, and Terminator uses it
 * so that successive invocations don't need to start a new VM.
 */
public final class InAppServer {
    private String fullName;
    private Class exportedInterface;
    private Object handler;
    
    public InAppServer(String name, String portFilename, Class exportedInterface, Object handler) {
        this.fullName = name + "Server";
        this.exportedInterface = exportedInterface;
        // FIXME: better type-safety?
        this.handler = handler;
        
        try {
            new Thread(new ConnectionAccepter(FileUtilities.fileFromString(portFilename)), fullName).start();
        } catch (Throwable th) {
            Log.warn("Couldn't start " + fullName, th);
        }
    }
    
    public boolean handleCommand(String line, PrintWriter out) {
        System.err.println(line);
        String[] split = line.split("[\t ]");
        String commandName = split[0];
        
        try {
            Method[] methods = exportedInterface.getMethods();
            for (Method method : methods) {
                if (method.getName().equals(commandName)) {
                    return invokeMethod(line, out, method, split);
                }
            }
            throw new NoSuchMethodException();
        } catch (NoSuchMethodException nsmex) {
            out.println(fullName + ": didn't understand request \"" + line + "\".");
        } catch (Exception ex) {
            ex.printStackTrace();
            String s = fullName + ": request denied \"" + line + "\" (" + ex.toString() + ").";
            out.println(s);
        } finally {
            out.flush();
            out.close();
        }
        return false;
    }
    
    private boolean invokeMethod(String line, PrintWriter out, Method method, String[] fields) throws IllegalAccessException, InvocationTargetException {
        ArrayList<Object> methodArguments = new ArrayList<Object>();
        
        Class[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 2 && parameterTypes[0] == PrintWriter.class && parameterTypes[1] == String.class) {
            // FIXME: there must be a better way to say "I want to parse the line myself."
            methodArguments.add(out);
            methodArguments.add(line);
        } else {
            int nextField = 1;
            for (Class parameterType : parameterTypes) {
                if (parameterType == PrintWriter.class) {
                    methodArguments.add(out);
                } else if (parameterType == String.class) {
                    methodArguments.add(fields[nextField++]);
                }
                // FIXME: support other common types. "int" seems a likely first candidate.
            }
        }
        
        Object result = method.invoke(handler, methodArguments.toArray());
        out.println((result == null) ? "OK" : result.toString());
        return true;
    }
    
    private class ConnectionAccepter implements Runnable {
        private ServerSocket socket;
        
        private ConnectionAccepter(File portFile) throws IOException {
            this.socket = new ServerSocket();
            socket.bind(null);
            writeHostAndPortToFile(portFile);
        }
        
        private void writeHostAndPortToFile(File portFile) {
            String host = socket.getInetAddress().getHostName();
            int port = socket.getLocalPort();
            // The motivation for the Log.warn would be better satisfied by Bug 38.
            Log.warn("echo " + host + ":" + port + " > " + portFile);
            StringUtilities.writeFile(portFile, host + ":" + port + "\n");
        }
        
        public void run() {
            acceptConnections();
        }
        
        private void acceptConnections() {
            for (;;) {
                try {
                    String handlerName = Thread.currentThread().getName() + "-Handler-" + Thread.activeCount();
                    new Thread(new ClientHandler(socket.accept()), handlerName).start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    private final class ClientHandler implements Runnable {
        private Socket client;
        
        private BufferedReader in;
        private PrintWriter out;
        
        private ClientHandler(Socket client) {
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
                Log.warn(Thread.currentThread().getName() + ": ignoring empty request");
                return;
            }
            if (handleCommand(line, out) == false) {
                out.println(Thread.currentThread().getName() + ": didn't understand request \"" + line + "\"");
            }
        }
    }
}
