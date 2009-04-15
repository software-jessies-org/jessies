package e.util;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

public class ProcessUtilities {
    private static List<String> prependCygwinShell(final List<String> command) {
        if (GuiUtilities.isWindows() == false) {
            return command;
        }
        if (command.isEmpty()) {
            return command;
        }
        String filename = command.get(0);
        // There is no standard for #! lines.
        // This is the longest length given in http://www.in-ulm.de/~mascheck/various/shebang/ (for FreeBSD).
        int bufferLength = 8192;
        char[] buffer = new char[bufferLength];
        int charactersRead;
        try {
            FileReader in = new FileReader(filename);
            try {
                charactersRead = in.read(buffer, 0, bufferLength);
            } finally {
                in.close();
            }
        } catch (Exception ex) {
            ex = ex;
            return command;
        }
        String line = new String(buffer, 0, charactersRead);
        // Cygwin and Linux pass second and subsequent words as a single argument.
        Matcher matcher = Pattern.compile("^#![ \\t]*(\\S+)[ \\t]*([^\\r\\n]+)?[\\r\\n]").matcher(line);
        if (matcher.find() == false) {
            return command;
        }
        String cygwinShell = matcher.group(1);
        // We won't recurse indefinitely because we won't be able to open "cygpath", even if run in Cygwin's bin directory, because the file is "cygpath.exe".
        String windowsShell = FileUtilities.rewriteCygwinFilename(cygwinShell);
        String flag = matcher.group(2);
        List<String> arguments = new ArrayList<String>();
        arguments.add(windowsShell);
        if (flag != null) {
            arguments.add(flag);
        }
        arguments.addAll(command);
        return arguments;
    }
    
    private static Process exec(final List<String> command, final File directory) throws Exception {
        String[] commandArray = prependCygwinShell(command).toArray(new String[command.size()]);
        return Runtime.getRuntime().exec(commandArray, null, directory);
    }
    
    /**
     * Runs 'command'. Returns the command's exit status.
     * Lines written to standard output are appended to 'lines'.
     * Lines written to standard error are appended to 'errors'.
     */
    public static int backQuote(final File directory, final String[] command, final ArrayList<String> lines, final ArrayList<String> errors) {
        if (lines == null) {
            throw new IllegalArgumentException("ArrayList 'lines' may not be null");
        }
        if (errors == null) {
            throw new IllegalArgumentException("ArrayList 'errors' may not be null");
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
    public static int backQuote(final File directory, final String[] commandArray, final LineListener outputLineListener, final LineListener errorLineListener) {
        List<String> command = Arrays.asList(commandArray);
        try {
            final Process p = exec(command, directory);
            p.getOutputStream().close();
            Thread errorReaderThread = new Thread(new Runnable() {
                public void run() {
                    readLinesFromStream(errorLineListener, p.getErrorStream());
                }
            }, "Process Back-Quote: " + shellQuotedFormOf(command));
            errorReaderThread.start();
            readLinesFromStream(outputLineListener, p.getInputStream());
            errorReaderThread.join();
            return p.waitFor();
        } catch (Exception ex) {
            feedExceptionToLineListener(errorLineListener, ex);
            return 1;
        }
    }
    
    public interface LineListener {
        public void processLine(String line);
    }
    
    /**
     * Collects lines into an ArrayList.
     */
    public static class ArrayListLineListener implements LineListener {
        private ArrayList<String> arrayList;
        
        public ArrayListLineListener(final ArrayList<String> arrayList) {
            this.arrayList = arrayList;
            if (arrayList == null) {
                throw new IllegalArgumentException("ArrayList 'arrayList' may not be null");
            }
        }
        
        public void processLine(String line) {
            arrayList.add(line);
        }
    }

    public static Process spawn(final File directory, String... command) {
        return spawn(directory, command, null);
    }
    
    /**
     * Runs a command and ignores the output.
     * The Process corresponding to the command is returned.
     * 'listener' may be null, but if supplied will be notified when the process exits.
     */
    public static Process spawn(final File directory, final String[] commandArray, final ProcessListener listener) {
        final List<String> command = Arrays.asList(commandArray);
        final String quotedCommand = shellQuotedFormOf(command);
        Process result = null;
        try {
            final Process p = exec(command, directory);
            result = p;
            new Thread("Process Spawn: " + quotedCommand) {
                public void run() {
                    try {
                        p.getOutputStream().close();
                        p.getInputStream().close();
                        p.getErrorStream().close();
                        int status = p.waitFor();
                        if (listener != null) {
                            listener.processExited(status);
                        }
                    } catch (Exception ex) {
                        Log.warn("Problem waiting for command to finish: " + quotedCommand, ex);
                    }
                }
            }.start();
        } catch (Exception ex) {
            Log.warn("Failed to spawn command: " + quotedCommand, ex);
        }
        return result;
    }

    public interface ProcessListener {
        public void processExited(int status);
    }
    
    public static void readLinesFromStream(LineListener listener, InputStream stream) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                listener.processLine(line);
            }
            in.close();
        } catch (Exception ex) {
            feedExceptionToLineListener(listener, ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ex) {
                    feedExceptionToLineListener(listener, ex);
                }
            }
        }
    }
    
    private static void feedExceptionToLineListener(LineListener listener, Exception ex) {
        listener.processLine(ex.getMessage());
        String stackTrace = StringUtilities.stackTraceFromThrowable(ex);
        for (String line : stackTrace.split("\n")) {
            listener.processLine(line);
        }
    }
    
    /**
     * Returns the process id of the given process, or -1 if we couldn't work it out.
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
    
    /**
     * Returns the process id of the current JVM, or -1 if we couldn't work it out.
     */
    public static int getVmProcessId() {
        try {
            if (GuiUtilities.isMacOs()) {
                // This only works if we were started with -Xdock, but that's true for our applications, and I don't have a better solution.
                Map<String, String> env = System.getenv();
                for (String key : env.keySet()) {
                    if (key.startsWith("APP_NAME_")) {
                        System.err.println(key);
                        System.err.println(key.substring(9));
                        return Integer.parseInt(key.substring(9));
                    }
                }
                return -1;
            } else {
                // We can't use StringUtilities.readFile because most files in /proc (including this one) report their length as 0.
                BufferedReader in = new BufferedReader(new FileReader("/proc/self/stat"));
                String content = in.readLine();
                in.close();
                return Integer.parseInt(content.substring(0, content.indexOf(' ')));
            }
        } catch (Exception ex) {
            return -1;
        }
    }
    
    /**
     * Returns a String[] suitable as argument to Runtime.exec or
     * ProcessBuilder's constructor. The arguments ask the system's default
     * command interpreter to interpret and run the given command.
     * 
     * The details of all this are obviously OS-specific, but it should be
     * suitable for executing user input in a manner that's unsurprising to
     * the user.
     * 
     * On Win32, cmd.exe is used as a command interpreter.
     * 
     * On other operating systems (assumed to be Unixes), the SHELL environment
     * variable is queried. If this isn't set, a default of /bin/sh is used,
     * though it probably won't work unless that happens to be bash(1).
     */
    public static String[] makeShellCommandArray(String command) {
        String shell = System.getenv("SHELL");
        if (shell == null) {
            shell = "bash";
        }
        ArrayList<String> result = new ArrayList<String>();
        // Try to put the command in its own process group, so it's easier to kill it and its children.
        File setsidBinary = FileUtilities.findSupportBinary("setsid");
        if (setsidBinary != null) {
            result.add(setsidBinary.toString());
        } else if (GuiUtilities.isWindows() && FileUtilities.findOnPath(shell) == null) {
            // If we found setsid, then setsid will be able to find /bin/bash.
            // The JVM won't, as it's not a Cygwin program and so is not aware of Cygwin's /bin mount.
            // /bin/bash is what SHELL will be if Evergreen's started from Terminator.
            return new String[] { "cmd", "/c", command };
        }
        result.add(shell);
        result.add("--login");
        result.add("-c");
        result.add(command);
        return result.toArray(new String[result.size()]);
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
    public static boolean terminateProcess(Process process) {
        int pid = getProcessId(process);
        if (pid == -1) {
            return false;
        }
        try {
            System.out.println("killing " + pid);
            Runtime.getRuntime().exec("kill -" + SIGTERM + " -" + pid);
            Runtime.getRuntime().exec("kill -" + SIGTERM + " " + pid);
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
    
    /**
     * Returns a single string representing the given command, quoted for
     * passing to a shell or, more usually, for unambiguous human-readable
     * output.
     */
    public static String shellQuotedFormOf(List<String> command) {
        // FIXME: we only cope with spaces in commands, not other shell meta-characters.
        StringBuilder result = new StringBuilder();
        for (String word : command) {
            if (result.length() > 0) {
                result.append(' ');
            }
            if (word.contains(" ")) {
                result.append('"');
                result.append(word);
                result.append('"');
            } else {
                result.append(word);
            }
        }
        return result.toString();
    }
    
    /** Prevents instantiation. */
    private ProcessUtilities() {
    }
}
