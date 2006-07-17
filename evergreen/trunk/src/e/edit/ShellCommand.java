package e.edit;

import e.ptextarea.*;
import e.util.*;
import java.awt.EventQueue;
import java.io.*;
import java.util.*;

public class ShellCommand {
    private String command;
    private ToolInputDisposition inputDisposition;
    private ToolOutputDisposition outputDisposition;
    
    private ETextWindow textWindow;
    private Workspace workspace;
    private String context;
    
    private Process process;
    
    /** The count of open streams. */
    private int openStreamCount = 0;
    
    private Runnable launchRunnable = new NoOpRunnable();
    private Runnable completionRunnable = new NoOpRunnable();
    
    private StringBuilder capturedOutput;
    
    /**
     * Creates a ShellCommand that, when runCommand is invoked, will start a
     * new task in the system's temporary directory (probably /tmp on Unix).
     * Until you invoke runCommand, you're at liberty to change any of the
     * command's properties through the relevant accessor methods.
     */
    public ShellCommand(String command, ToolInputDisposition inputDisposition, ToolOutputDisposition outputDisposition) {
        setCommand(command);
        this.inputDisposition = inputDisposition;
        this.outputDisposition = outputDisposition;
        setTextWindow(null);
        setWorkspace(Evergreen.getInstance().getCurrentWorkspace());
        setContext(System.getProperty("java.io.tmpdir"));
    }
    
    private ProcessBuilder makeProcessBuilder() {
        ProcessBuilder processBuilder = new ProcessBuilder(ProcessUtilities.makeShellCommandArray(command));
        processBuilder.directory(FileUtilities.fileFromString(context));
        Map<String, String> environment = processBuilder.environment();
        environment.put("EDIT_CURRENT_DIRECTORY", FileUtilities.parseUserFriendlyName(context));
        environment.put("EDIT_WORKSPACE_ROOT", FileUtilities.parseUserFriendlyName(getWorkspace().getRootDirectory()));
        if (textWindow != null) {
            environment.put("EDIT_CURRENT_FILENAME", FileUtilities.parseUserFriendlyName(textWindow.getFilename()));
            environment.put("EDIT_CURRENT_LINE_NUMBER", Integer.toString(textWindow.getCurrentLineNumber()));
        }
        return processBuilder;
    }

    public void runCommand() throws IOException {
        process = makeProcessBuilder().start();

        EventQueue.invokeLater(launchRunnable);
        
        Evergreen.getInstance().showStatus("Started task '" + command + "'");
        
        Thread standardInputPump = new Thread(new Runnable() {
            public void run() {
                pumpStandardInput();
            }
        });
        standardInputPump.start();
        
        capturedOutput = new StringBuilder();
        
        startMonitoringStream(process.getInputStream());
        startMonitoringStream(process.getErrorStream());
        
        workspace.getErrorsWindow().resetAutoScroll();
    }
    
    private void pumpStandardInput() {
        OutputStream os = process.getOutputStream();
        try {
            CharSequence data = chooseStandardInputData();
            if (data != null) {
                // As of Java 6, using PrintWriter so we can "append" a CharSequence is no more efficient than converting it to a String ourselves and passing it directly to BufferedWriter, but one can only hope that this will improve in future...
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, "UTF-8")));
                out.append(data);
                out.flush();
                out.close();
            }
        } catch (Exception ex) {
            Log.warn("Problem pumping standard input for task '" + command + "'", ex);
            workspace.getErrorsWindow().append(new String[] { "Problem pumping standard input for task '" + command + "': " + ex.getMessage() + "." });
        } finally {
            try {
                os.close();
            } catch (IOException ex) {
                workspace.getErrorsWindow().append(new String[] { "Couldn't close standard input for task '" + command + "': " + ex.getMessage() + "." });
            }
        }
    }
    
    private CharSequence chooseStandardInputData() {
        CharSequence result = null;
        if (textWindow != null) {
            PTextArea text = textWindow.getText();
            switch (inputDisposition) {
            case NO_INPUT:
                break;
            case SELECTION_OR_DOCUMENT:
                result = text.hasSelection() ? text.getSelectedText() : text.getTextBuffer();
                break;
            case DOCUMENT:
                result = text.getTextBuffer();
                break;
            }
        }
        return result;
    }
    
    public void startMonitoringStream(InputStream stream) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(stream, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StreamMonitor streamMonitor = new StreamMonitor(bufferedReader, this);
        streamMonitor.execute();
    }
    
    /**
     * Invoked by StreamMonitor.process, on the EDT.
     */
    public void process(String... lines) {
        switch (outputDisposition) {
        case CREATE_NEW_DOCUMENT:
            Log.warn("CREATE_NEW_DOCUMENT not yet implemented.");
            break;
        case DIALOG:
            Log.warn("DIALOG not yet implemented.");
            break;
        case DISCARD:
            break;
        case ERRORS_WINDOW:
            getWorkspace().getErrorsWindow().append(lines);
            break;
        case INSERT:
        case REPLACE:
            for (String line : lines) {
                capturedOutput.append(line);
                capturedOutput.append('\n');
            }
            break;
        }
    }
    
    /**
     * Invoked when the StreamMonitors finish, on the EDT.
     */
    private void processFinished(int exitStatus) {
        Evergreen.getInstance().showStatus("Task '" + command + "' finished");
        
        ArrayList<String> errorsWindowFooter = new ArrayList<String>();
        
        switch (outputDisposition) {
        case CREATE_NEW_DOCUMENT:
        case DIALOG:
        case DISCARD:
        case ERRORS_WINDOW:
            // We dealt with the sub-process output as we went along.
            errorsWindowFooter.add("-------------------------------------------------------------------------");
            break;
        case INSERT:
            textWindow.getText().insert(capturedOutput);
            break;
        case REPLACE:
            PTextArea text = textWindow.getText();
            if (text.hasSelection()) {
                text.replaceSelection(capturedOutput);
            } else {
                text.setText(capturedOutput);
            }
            break;
        }
        
        // A non-zero exit status is always potentially interesting.
        if (exitStatus != 0) {
            errorsWindowFooter.add("Task '" + command + "' failed with exit status " + exitStatus);
        }
        workspace.getErrorsWindow().append(errorsWindowFooter.toArray(new String[errorsWindowFooter.size()]));
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
            int exitStatus = 0;
            try {
                exitStatus = process.waitFor();
            } catch (InterruptedException ex) {
                /* Ignore what we don't understand. */
                ex = ex;
            }
            final int finalExitStatus = exitStatus;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    processFinished(finalExitStatus);
                }
            });
            EventQueue.invokeLater(completionRunnable);
        }
    }
    
    public Workspace getWorkspace() {
        return workspace;
    }
    
    public void setWorkspace(Workspace newWorkspace) {
        this.workspace = newWorkspace;
    }
    
    public void setTextWindow(ETextWindow textWindow) {
        this.textWindow = textWindow;
    }
    
    /** Returns the context (current working directory) for this task. */
    public String getContext() {
        return this.context;
    }
    
    /** Sets the context (current working directory) for this task. */
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
