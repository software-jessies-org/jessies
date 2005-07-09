package e.edit;

import e.util.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class ShellCommand {
    private String filename;
    private int lineNumber;
    private Workspace workspace;
    private String command;
    private String context;
    
    private Process process;
    
    /** The count of open streams. */
    private int openStreamCount = 0;
    
    private Runnable launchRunnable = new NoOpRunnable();
    private Runnable completionRunnable = new NoOpRunnable();
    
    /**
     * Creates a ShellCommand that, when runCommand is invoked, will start a
     * new task in the system's temporary directory (probably /tmp on Unix).
     * Until you invoke runCommand, you're at liberty to change any of the
     * command's properties through the relevant accessor methods.
     */
    public ShellCommand(String command) {
        setCommand(command);
        setFilename("");
        setLineNumber(0);
        setWorkspace(Edit.getInstance().getCurrentWorkspace());
        setContext(System.getProperty("java.io.tmpdir"));
    }
    
    private ProcessBuilder makeProcessBuilder() {
        ProcessBuilder processBuilder = new ProcessBuilder(makeCommandLine(command));
        processBuilder.directory(FileUtilities.fileFromString(context));
        Map<String, String> environment = processBuilder.environment();
        environment.put("EDIT_CURRENT_DIRECTORY", FileUtilities.parseUserFriendlyName(context));
        environment.put("EDIT_CURRENT_FILENAME", FileUtilities.parseUserFriendlyName(filename));
        environment.put("EDIT_CURRENT_LINE_NUMBER", Integer.toString(lineNumber));
        environment.put("EDIT_WORKSPACE_ROOT", FileUtilities.parseUserFriendlyName(getWorkspace().getRootDirectory()));
        return processBuilder;
    }

    public void runCommand() throws IOException {
        process = makeProcessBuilder().start();

        SwingUtilities.invokeLater(launchRunnable);
        
        Edit.getInstance().showStatus("Started task '" + command + "'");
        
        startMonitoringStream(process.getInputStream());
        startMonitoringStream(process.getErrorStream());
        
        workspace.getErrorsWindow().resetAutoScroll();
        
// this implements wily-style |cmd
//        InputStream inStream = process.getInputStream();
//        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
//        int count;
//        char[] chars = new char[8192];
//        StringBuilder buf = new StringBuilder();
//        while ((count = in.read(chars)) > 0) {
//            buf.append(chars, 0, count);
//        }
//        setSelectedText(buf.toString());
    }

    public void startMonitoringStream(InputStream stream) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(stream, "UTF-8");
        StreamMonitor streamMonitor = new StreamMonitor(new BufferedReader(inputStreamReader), this);
        streamMonitor.start();
    }
    
    /**
     * Invoked by StreamMonitor when one of this task's streams is opened.
     */
    public synchronized void streamOpened() {
        ++openStreamCount;
    }
    
    /**
     * Invoked by StreamMonitor when one of this task's streams is closed. If
     * there are no streams left open, this task has finished and the
     * completion runnable is run on the event dispatch thread.
     */
    public synchronized void streamClosed() {
        openStreamCount--;
        if (openStreamCount == 0) {
            try {
                int exitStatus = process.waitFor();
                if (exitStatus != 0) {
                    workspace.reportError(context, "Task '" + command + "' failed with exit status " + exitStatus);
                }
            } catch (InterruptedException ex) {
                /* Ignore what we don't understand. */
                ex = ex;
            }
            workspace.getErrorsWindow().drawHorizontalRule();
            Edit.getInstance().showStatus("Task '" + command + "' finished");
            SwingUtilities.invokeLater(completionRunnable);
        }
    }
    
    /**
     * Returns the arguments to exec to invoke a command interpreter to run the
     * given command. The details of this are obviously OS-specific. Under
     * Windows, cmd.exe is used as a command interpreter. Under other operating
     * systems, the SHELL environment variable is queried. If this isn't set,
     * a default of /bin/sh is used.
     */
    private String[] makeCommandLine(String command) {
        if (GuiUtilities.isWindows()) {
            return new String[] { "cmd", "/c", command };
        } else {
            String shell = System.getenv("SHELL");
            if (shell == null) {
                shell = "/bin/sh";
            }
            return new String[] { shell, "--login", "-c", command };
        }
    }
    
    public Workspace getWorkspace() {
        return workspace;
    }
    
    public void setWorkspace(Workspace newWorkspace) {
        this.workspace = newWorkspace;
    }
    
    public void setLineNumber(int newLineNumber) {
        this.lineNumber = newLineNumber;
    }
    
    public void setFilename(String newFilename) {
        this.filename = newFilename;
    }
    
    /** Returns the context for this task. */
    public String getContext() {
        return this.context;
    }
    
    public void setContext(String newContext) {
        this.context = newContext;
    }
    
    public String getCommand() {
        return this.command;
    }
    
    public void setCommand(String newCommand) {
        this.command = newCommand.trim();
    }
    
    /**
     * Sets the Runnable to be invoked on the event dispatch thread when the
     * shell command completes.
     */
    public void setCompletionRunnable(Runnable completionRunnable) {
        this.completionRunnable = completionRunnable;
    }
    
    /**
     * Sets the Runnable to be invoked on the event dispatch thread when the
     * shell command is started.
     */
    public void setLaunchRunnable(Runnable launchRunnable) {
        this.launchRunnable = launchRunnable;
    }
    
    /**
     * Returns the underlying Process instance.
     */
    public Process getProcess() {
        return process;
    }
}
