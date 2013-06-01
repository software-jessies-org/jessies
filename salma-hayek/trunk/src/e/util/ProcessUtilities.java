package e.util;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;
import org.jessies.os.*;

public class ProcessUtilities {
    private static List<String> prependCygwinShell(final List<String> command) {
        if (OS.isWindows() == false) {
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
        String windowsShell = FileUtilities.fileFromString(cygwinShell).toString();
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
    public static int backQuote(final File directory, final String[] command, final String input, final ArrayList<String> lines, final ArrayList<String> errors) {
        if (lines == null) {
            throw new IllegalArgumentException("ArrayList 'lines' may not be null");
        }
        if (errors == null) {
            throw new IllegalArgumentException("ArrayList 'errors' may not be null");
        }
        return backQuote(directory, command, input, new ArrayListLineListener(lines), new ArrayListLineListener(errors));
    }
    
    // Tens of users were written before we supported providing input.
    public static int backQuote(final File directory, final String[] command, final ArrayList<String> lines, final ArrayList<String> errors) {
        return backQuote(directory, command, "", lines, errors);
    }
    
    /**
     * Runs 'command'. Returns the command's exit status.
     * Lines written to standard output are passed to 'outputLineListener'.
     * Lines written to standard error are passed to 'errorLineListener'.
     *
     * If directory is null, the subprocess inherits our working directory.
     */
    public static int backQuote(final File directory, final String[] commandArray, final String input, final LineListener outputLineListener, final LineListener errorLineListener) {
        return runCommand(directory, commandArray, null, input, outputLineListener, errorLineListener);
    }
    
    /**
     * Runs 'command'. Returns the command's exit status.
     * Any 'processListener' will be notified when the process starts and exits.
     * Lines written to standard output are passed to 'outputLineListener'.
     * Lines written to standard error are passed to 'errorLineListener'.
     * 
     * If directory is null, the subprocess inherits our working directory.
     */
    public static int runCommand(final File directory, final String[] commandArray, final ProcessListener processListener, final String input, final LineListener outputLineListener, final LineListener errorLineListener) {
        List<String> command = Arrays.asList(commandArray);
        int status = 1; // FIXME: should we signal "something went wrong" more distinctively?
        try {
            final Process p = exec(command, directory);
            if (processListener != null) {
                processListener.processStarted(p);
            }
            Thread inputWriterThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), "UTF-8"));
                        out.append(input);
                        out.flush();
                        out.close();
                    } catch (Exception ex) {
                        feedExceptionToLineListener(errorLineListener, ex);
                    }
                }
            }, "Process Input: " + shellQuotedFormOf(command));
            inputWriterThread.start();
            Thread errorReaderThread = new Thread(new Runnable() {
                public void run() {
                    readLinesFromStream(errorLineListener, p.getErrorStream());
                }
            }, "Process Back-Quote: " + shellQuotedFormOf(command));
            errorReaderThread.start();
            readLinesFromStream(outputLineListener, p.getInputStream());
            inputWriterThread.join();
            errorReaderThread.join();
            status = p.waitFor();
        } catch (Exception ex) {
            feedExceptionToLineListener(errorLineListener, ex);
        } finally {
            if (processListener != null) {
                processListener.processExited(status);
            }
        }
        return status;
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
     * 'listener' may be null, but if supplied will be notified when the process starts and exits.
     */
    public static Process spawn(final File directory, final String[] commandArray, final ProcessListener listener) {
        final List<String> command = Arrays.asList(commandArray);
        final String quotedCommand = shellQuotedFormOf(command);
        Process result = null;
        try {
            final Process p = exec(command, directory);
            if (listener != null) {
                listener.processStarted(p);
            }
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
        public void processStarted(Process process);
        public void processExited(int status);
    }
    
    public static void readLinesFromStream(LineListener listener, InputStream stream) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                if (listener != null) {
                    listener.processLine(line);
                }
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
        if (listener == null) {
            return;
        }
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
     * Returns the process id of the current JVM.
     */
    public static int getVmProcessId() {
        return Posix.getpid();
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
        } else if (OS.isWindows() && FileUtilities.findOnPath(shell) == null) {
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
    
    /**
     * Sends the given signal to the given process. Returns false if
     * the signal could not be sent.
     */
    public static boolean terminateProcess(Process process) {
        int pid = getProcessId(process);
        if (pid == -1) {
            return false;
        }
        // FIXME: is this really what we want to do?
        // FIXME: move this into Evergreen and have the "kill" button escalate from SIGINT to SIGTERM to SIGKILL?
        return (Posix.kill(-pid, Signal.SIGTERM) == 0) && (Posix.kill(pid, Signal.SIGTERM) == 0);
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
    
    private static String checkCandidateDirectory(String path) {
        try {
            // fileFromString canonicalizes the path on Cygwin.
            // If we want to reopen terminals in the directory they were in when closed,
            // or otherwise "bookmark" them, then we need to canonicalize on all platforms.
            // Matt Hillsdon's Eclipse embedding does this and Costantino mentioned it too.
            File directory = FileUtilities.fileFromString(path).getCanonicalFile();
            // If Terminator can't start a process with this as the cwd, then it's no use to us.
            if (directory.canRead()) {
                return directory.toString();
            }
        } catch (IOException ex) {
            ex = ex;
        }
        return null;
    }
    
    /**
     * Silently returns null if the directory can't be determined.
     */
    public static String findCurrentWorkingDirectory(int pid) {
        String[] paths = new String[] {
            // Solaris 10 exposes the path in a readable form here.
            // All versions of Solaris make readlink("/proc/<pid>/cwd") return an empty string, though it can be successfully used with opendir.
            "/proc/" + pid + "/path/cwd",
            // Linux and Cygwin use a symlink of this form.
            "/proc/" + pid + "/cwd"
        };
        for (String path : paths) {
            String checkedPath = checkCandidateDirectory(path);
            if (checkedPath != null) {
                return checkedPath;
            }
        }
        // Mac OS has no /proc but comes with lsof.
        ArrayList<String> output = new ArrayList<String>();
        ArrayList<String> errors = new ArrayList<String>();
        int status = backQuote(null, new String[] { "lsof", "-a", "-p", Integer.toString(pid), "-d", "cwd", "-F0n" }, output, errors);
        if (status != 0) {
            return null;
        }
        // p18316\0
        // n/private/var/tmp\0
        String cwdLine = output.get(1);
        String path = cwdLine.substring(1, cwdLine.length() - 1);
        return checkCandidateDirectory(path);
    }
    
    /** Prevents instantiation. */
    private ProcessUtilities() {
    }
}
