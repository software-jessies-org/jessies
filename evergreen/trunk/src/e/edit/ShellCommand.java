package e.edit;

import java.io.*;
import java.awt.event.*;

import e.gui.*;
import e.util.*;

public class ShellCommand {
    private Workspace workspace;
    private String command;
    private String context;
    private String name;
    private String[] envp;
    
    /** The count of open streams. */
    private int openStreamCount = 0;
    
    private Process proc;
    
    public ShellCommand(String command) throws IOException {
        this("", 0, Edit.getCurrentWorkspace(), "/", command);
    }
    
    /** Starts a new task. */
    public ShellCommand(String filename, int lineNumber, Workspace workspace, String context, String command) throws IOException {
        this.command = command.trim();

        /* FIXME: we also need to mangle UTF-8. for each byte, we need to have the 'character' corresponding to the byte. the JVM 'translates' non-ASCII characters by just sending the first byte, it seems. */
        
        this.workspace = workspace;
        this.context = context;
        this.name = command;

        initEnvironmentVariables(filename, lineNumber);
        runCommand();
    }

    public void initEnvironmentVariables(String filename, int lineNumber) {
        envp = new String[] {
            "EDIT_CURRENT_DIRECTORY=" + context,
            "EDIT_CURRENT_FILENAME=" + filename,
            "EDIT_CURRENT_LINE_NUMBER=" + lineNumber,
            "EDIT_WORKSPACE_ROOT=" + getWorkspace().getRootDirectory(),
            /* Thanks, Sun. */
            makePassThroughVariable("DISPLAY"),
            makePassThroughVariable("HOME"),
            makePassThroughVariable("JAVA_HOME"),
            makePassThroughVariable("PATH")
        };
    }

    public String makePassThroughVariable(String name) {
        String result = name + "=" + System.getProperty("env." + name);
        //System.err.println("made passthrough " + result);
        return result;
    }

    public void runCommand() throws IOException {
        File directory = FileUtilities.fileFromString(context);
        Process proc = Runtime.getRuntime().exec(makeCommandLine(command), envp, directory);

        startMonitoringStream(proc.getInputStream());
        startMonitoringStream(proc.getErrorStream());
        
        workspace.getErrorsWindow().resetAutoScroll();
        Edit.showStatus("Started task '" + command + "'");
        
// this implements wily-style |cmd
//        InputStream inStream = proc.getInputStream();
//        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
//        int count;
//        char[] chars = new char[8192];
//        StringBuffer buf = new StringBuffer();
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
    
    /** Invoked by StreamMonitor when one of this task's streams is opened. */
    public synchronized void streamOpened() {
        openStreamCount++;
    }
    
    /**
    * Invoked by StreamMonitor when one of this task's streams is closed. If there are no
    * streams left open, this task has finished and Edit is notified.
    */
    public synchronized void streamClosed() {
        openStreamCount--;
        if (openStreamCount == 0) {
            workspace.getErrorsWindow().drawHorizontalRule();
            Edit.showStatus("Task '" + command + "' finished");
        }
    }
    
    /**
    * Returns the arguments to exec to invoke a command interpreter to run the given
    * command. The details of this are obviously OS-specific. Under Windows, cmd.exe
    * is used as a command interpreter. Under other operating systems, the SHELL
    * environment variable is queried. If this isn't set, a default of /bin/sh is used.
    */
    public String[] makeCommandLine(String command) {
        boolean windows = System.getProperty("os.name").indexOf("Windows") != -1;
        if (windows) {
            return new String[] { "cmd", "/c", command };
        } else {
            String shell = System.getProperty("SHELL");
            if (shell == null) {
                shell = "/bin/sh";
            }
            return new String[] { shell, /*"--login",*/ "-c", command };
        }
    }
    
    public Workspace getWorkspace() {
        return workspace;
    }
    
    /** Returns the context for this task. */
    public String getContext() {
        return this.context;
    }
    
    /** Returns the name of this task. Currently this is the whole text of the command. */
    public String getName() {
        return this.name;
    }
}
