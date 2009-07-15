package terminator;

import e.util.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import terminator.view.*;

public class TerminatorOpener implements Runnable {
    private List<String> arguments;
    private PrintWriter err;
    private TerminatorFrame window;
    
    public TerminatorOpener(final List<String> arguments, final PrintWriter err) {
        this.arguments = arguments;
        this.err = err;
    }
    
    private static void showUsage(final PrintWriter out) {
        out.println("Usage: terminator [--help] [[-n <name>] [--working-directory <directory>] [<command>]]...");
    }
    
    public boolean showUsageIfRequested(final PrintWriter out) {
        if (arguments.contains("-h") || arguments.contains("-help") || arguments.contains("--help")) {
            showUsage(out);
            return true;
        }
        return false;
    }
    
    /**
     * Sets up the user interface on the AWT event thread.
     */
    public TerminatorFrame openFromBackgroundThread() {
        try {
            EventQueue.invokeAndWait(this);
        } catch (Exception ex) {
            Log.warn("an unexpected checked exception was thrown", ex);
        }
        return window;
    }
    
    private static class UsageError extends RuntimeException {
        public UsageError(final String message) {
            super(message);
        }
    }
    
    public TerminatorFrame createUi() {
        try {
            this.window = new TerminatorFrame(getInitialTerminals());
            return window;
        } catch (UsageError ex) {
            err.println(ex.getMessage());
            showUsage(err);
        } catch (Exception ex) {
            err.println(ex.getMessage());
            Log.warn("failed to open window", ex);
        }
        return null;
    }
    
    public void run() {
        createUi();
    }
    
    private List<JTerminalPane> getInitialTerminals() {
        ArrayList<JTerminalPane> result = new ArrayList<JTerminalPane>();
        String name = null;
        String workingDirectory = null;
        for (int i = 0; i < arguments.size(); ++i) {
            String word = arguments.get(i);
            if (word.equals("-n") || word.equals("-T")) {
                name = arguments.get(++i);
                continue;
            }
            if (word.equals("--working-directory")) {
                String previousWorkingDirectory = workingDirectory;
                String workingDirectoryArgument = arguments.get(++i);
                if (FileUtilities.fileFromString(workingDirectoryArgument).isAbsolute() == false && previousWorkingDirectory != null) {
                    workingDirectory = new File(previousWorkingDirectory, workingDirectoryArgument).getPath();
                } else {
                    workingDirectory = workingDirectoryArgument;
                }
                continue;
            }
            if (word.equals("-e")) {
                List<String> argV = arguments.subList(++i, arguments.size());
                if (argV.isEmpty()) {
                    throw new UsageError("-e requires further arguments");
                }
                result.add(JTerminalPane.newCommandWithArgV(name, workingDirectory, argV));
                break;
            }
            
            // We can't hope to imitate the shell's parsing of a string, so pass it unmolested to the shell.
            String command = word;
            result.add(JTerminalPane.newCommandWithName(command, name, workingDirectory));
            name = null;
        }
        
        if (result.isEmpty()) {
            result.add(JTerminalPane.newShellWithName(name, workingDirectory));
        }
        return result;
    }
}
