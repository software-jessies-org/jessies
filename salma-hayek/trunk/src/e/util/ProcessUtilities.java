package e.util;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class ProcessUtilities {
    /**
     * Runs 'command'. Returns the command's exit status.
     * Lines written to standard output are appended to 'lines'.
     * Lines written to standard error are appended to 'errors'.
     */
    public static int backQuote(final File directory, final String[] command, final ArrayList lines, final ArrayList errors) {
        if (lines == null) {
            throw new IllegalArgumentException("`lines' may not be null");
        }
        if (errors == null) {
            throw new IllegalArgumentException("`errors' may not be null");
        }
        return backQuote(directory, command, new ArrayListLineListener(lines), new ArrayListLineListener(errors));
    }
    
    /**
     * Runs 'command'. Returns the command's exit status.
     * Lines written to standard output are passed to 'outputLineListener'.
     * Lines written to standard error are passed to 'errorLineListener'.
     *
     * If directory is null, the subprocess inherits our working directory.
     *
     * You can use the same ArrayList for 'lines' and 'errors'. All the error
     * lines will appear after all the output lines.
     */
    public static int backQuote(final File directory, final String[] command, final LineListener outputLineListener, final LineListener errorLineListener) {
        ArrayList result = new ArrayList();
        try {
            Process p = Runtime.getRuntime().exec(command, null, directory);
            p.getOutputStream().close();
            readLinesFromStream(outputLineListener, p.getInputStream());
            readLinesFromStream(errorLineListener, p.getErrorStream());
            return p.waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
            errorLineListener.processLine(ex.getMessage());
            return 1;
        }
    }
    
    public interface LineListener {
        public void processLine(String line);
    }
    
    /**
     * Collects lines into an ArrayList.
     */
    private static class ArrayListLineListener implements LineListener {
        private ArrayList arrayList;
        
        public ArrayListLineListener(final ArrayList arrayList) {
            this.arrayList = arrayList;
            if (arrayList == null) {
                throw new IllegalArgumentException("`arrayList' may not be null");
            }
        }
        
        public void processLine(String line) {
            arrayList.add(line);
        }
    }

    public static void spawn(final File directory, final String[] command) {
        spawn(directory, command, null);
    }
    
    /**
     * Runs a command and ignores the output. The child is waited for on a
     * separate thread, so there's no indication of whether the spawning was
     * successful or not. A better design might be to exec in the current
     * thread, and hand the Process over to another Thread; you'd still not
     * get the return code (but losing that is part of the deal), but you
     * would at least know that Java had no trouble in exec. Is that worth
     * anything?
     * 
     * listener may be null.
     */
    public static void spawn(final File directory, final String[] command, final ProcessListener listener) {
        System.err.println("Spawning '" + StringUtilities.join(command, " ") + "'.");
        try {
            final Process p = Runtime.getRuntime().exec(command, null, directory);
            new Thread() {
                public void run() {
                    try {
                        p.getOutputStream().close();
                        int status = p.waitFor();
                        if (listener != null) {
                            listener.processExited(status);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public interface ProcessListener {
        public void processExited(int status);
    }
    
    private static void readLinesFromStream(LineListener listener, InputStream stream) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = in.readLine()) != null) {
                listener.processLine(line);
            }
            in.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            listener.processLine(ex.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    listener.processLine(ex.getMessage());
                }
            }
        }
    }
    
    /**
     * Returns the process id of the given process, or -1 if we couldn't
     * work it out.
     */
    public static int getProcessId(Process process) {
        try {
            Field pidField = process.getClass().getDeclaredField("pid");
            pidField.setAccessible(true);
            return pidField.getInt(process);
        } catch (Exception ex) {
            return -1;
        }
    }
    
    /** The HUP (hang up) signal. */
    public static final int SIGHUP = 1;
    
    /** The INT (interrupt) signal. */
    public static final int SIGINT = 2;
    
    /** The QUIT (quit) signal. */
    public static final int SIGQUIT = 3;
    
    /** The KILL (non-catchable, non-ignorable kill) signal. */
    public static final int SIGKILL = 9;
    
    /** The TERM (soft termination) signal. */
    public static final int SIGTERM = 15;
    
    /**
     * Sends the given signal to the given process. Returns false if
     * the signal could not be sent.
     */
    public static boolean signalProcess(Process process, int signal) {
        int pid = getProcessId(process);
        if (pid == -1) {
            return false;
        }
        try {
            Runtime.getRuntime().exec("kill -" + signal + " " + pid);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
    
    /**
     * Returns the integer file descriptor corresponding to one of
     * FileDescriptor.in, FileDescriptor.out or FileDescriptor.err
     * for the given process. Returns -1 on error.
     */
    public static int getFd(Process process, FileDescriptor which) {
        if (which == FileDescriptor.in) {
            return getFd(process, "stdin_fd");
        } else if (which == FileDescriptor.out) {
            return getFd(process, "stdout_fd");
        } else if (which == FileDescriptor.err) {
            return getFd(process, "stderr_fd");
        } else {
            return -1;
        }
    }
    
    private static int getFd(Process process, String which) {
        try {
            Field fileDescriptorField = process.getClass().getDeclaredField(which);
            fileDescriptorField.setAccessible(true);
            FileDescriptor fileDescriptor = (FileDescriptor) fileDescriptorField.get(process);
            Field fdField = FileDescriptor.class.getDeclaredField("fd");
            fdField.setAccessible(true);
            return fdField.getInt(fileDescriptor);
        } catch (Exception ex) {
            return -1;
        }
    }
    
    /** Prevents instantiation. */
    private ProcessUtilities() {
    }
}
