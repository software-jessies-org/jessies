package e.util;

import java.io.*;
import java.net.*;

public interface IPC {
    public interface ConnectionListener {
        public Connection accept() throws IOException;
    }
    
    public interface Connection {
        public InputStream getInputStream() throws IOException;
        public OutputStream getOutputStream() throws IOException;
        public void close();
    }
    
    
    
    public class SocketConnectionListener implements ConnectionListener {
        private ServerSocket serverSocket;
        
        public SocketConnectionListener(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }
        
        public Connection accept() throws IOException {
            return new SocketConnection(serverSocket.accept());
        }
    }
    
    public class SocketConnection implements Connection {
        private Socket socket;
        
        public SocketConnection(Socket socket) {
            this.socket = socket;
        }
        
        public InputStream getInputStream() throws IOException {
            return socket.getInputStream();
        }
        
        public OutputStream getOutputStream() throws IOException {
            return socket.getOutputStream();
        }
        
        public void close() {
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    
    
    public class FifoConnectionListener implements ConnectionListener {
        private File serverFifo;
        private BufferedReader in;
        private OutputStream fifoWriterToKeepFifoFromClosingItself;
        
        public FifoConnectionListener(final File serverFifo) throws IOException {
            this.serverFifo = serverFifo;
            // Open the fifo for writing in another thread.  Opening for reading or writing
            // normally blocks until someone else has it open for writing or reading, so
            // putting the open-for-writing in another thread prevents us from deadlocking.
            // We must open an output stream to the fifo not just in order to open the
            // input stream, but we must also keep the output stream open to prevent the
            // fifo from closing.  Normally, a SIGPIPE is sent to the reader when the last
            // writer closes, so by keeping one writer always open we prevent this.
            new Thread(new Runnable() {
                public void run() {
                    try {
                        fifoWriterToKeepFifoFromClosingItself = new FileOutputStream(serverFifo);
                    } catch (IOException ex) {
                        Log.warn("Server FIFO " + serverFifo + " failed to open for writing.", ex);
                    }
                }
            }).start();
            in = new BufferedReader(new FileReader(serverFifo));
        }
        
        public Connection accept() throws IOException {
            String pid = in.readLine();
            if (pid == null) {
                throw new IOException("FIFO closed unexpectedly");
            }
            return new FifoConnection(serverFifo.getParentFile(), pid);
        }
    }
    
    public class FifoConnection implements Connection {
        private InputStream in;
        private OutputStream out;
        
        public FifoConnection(final File directory, final String fifoBaseName) throws IOException {
            // Since opening FIFOs can block until the other end has opened too,
            // force one of the streams to be opened in a different thread.  That
            // way, if the other side chooses to open the FIFOs in the opposite order,
            // we don't deadlock.
            new Thread(new Runnable() {
                public void run() {
                    try {
                        File inputFile = new File(directory, fifoBaseName + ".in");
                        in = new FileInputStream(inputFile);
                    } catch (IOException ex) {
                        Log.warn("Failed to open FIFO " + fifoBaseName + ".in", ex);
                    }
                }
            }).start();
            File outputFile = new File(directory, fifoBaseName + ".out");
            out = new FileOutputStream(outputFile);
        }
        
        public InputStream getInputStream() throws IOException {
            return in;
        }
        
        public OutputStream getOutputStream() throws IOException {
            return out;
        }
        
        public void close() {
            try {
                in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
