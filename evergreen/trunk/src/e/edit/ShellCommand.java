package e.edit;

import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class ShellCommand {
    private String command;
    private final ToolInputDisposition inputDisposition;
    private final ToolOutputDisposition outputDisposition;
    
    private final Workspace workspace;
    private final EErrorsWindow errorsWindow;
    
    private final StringBuilder capturedOutput = new StringBuilder();
    
    private ETextWindow textWindow;
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
    public ShellCommand(Workspace workspace, String command, ToolInputDisposition inputDisposition, ToolOutputDisposition outputDisposition) {
        this.workspace = workspace;
        this.errorsWindow = workspace.createErrorsWindow("Command Output"); // FIXME: be more specific.
        setCommand(command);
        this.inputDisposition = inputDisposition;
        this.outputDisposition = outputDisposition;
        setTextWindow(null);
        setContext(System.getProperty("java.io.tmpdir"));
    }
    
    private ProcessBuilder makeProcessBuilder() {
        ProcessBuilder processBuilder = new ProcessBuilder(ProcessUtilities.makeShellCommandArray(command));
        processBuilder.directory(FileUtilities.fileFromString(context));
        Map<String, String> environment = processBuilder.environment();
        environment.put("EDIT_CURRENT_DIRECTORY", FileUtilities.parseUserFriendlyName(context));
        environment.put("EDIT_WORKSPACE_ROOT", FileUtilities.parseUserFriendlyName(workspace.getRootDirectory()));
        if (textWindow != null) {
            environment.put("EDIT_CURRENT_FILENAME", FileUtilities.parseUserFriendlyName(textWindow.getFilename()));
            
            // Humans number lines from 1, text components from 0.
            // FIXME: we should pass full selection information.
            PTextArea textArea = textWindow.getTextArea();
            final int currentLineNumber = 1 + textArea.getLineOfOffset(textArea.getSelectionStart());
            environment.put("EDIT_CURRENT_LINE_NUMBER", Integer.toString(currentLineNumber));
        }
        return processBuilder;
    }

    public void runCommand() throws IOException {
        final CharSequence data = chooseStandardInputData();
        
        process = makeProcessBuilder().start();

        EventQueue.invokeLater(launchRunnable);
        
        // This causes ugly flickering if the window's already on the top of the stack, but it fixes the problem on small screens where your main window covers your build window and you don't remember/want to close the build window before you start editing.
        // As usual, we can't use toFront because the GNOME morons (okay, well-intentioned fascists, but aren't fascists always well-intentioned in their own minds?) broke it for us, and Sun hasn't worked around the breakage.
        // I don't know of any way to test whether we're already on top.
        // FIXME: this isn't relevant at the moment, because each build gets a new EErrorsWindow.
        //errorsWindow.setVisible(false);
        if (outputDisposition == ToolOutputDisposition.ERRORS_WINDOW) {
            errorsWindow.setVisible(true);
        }
        
        errorsWindow.showStatus("Started task \"" + command + "\"");
        errorsWindow.taskDidStart(process);
        
        Thread standardInputPump = new Thread(new Runnable() {
            public void run() {
                pumpStandardInput(data);
            }
        });
        standardInputPump.start();
                
        startMonitoringStream(process.getInputStream(), false);
        startMonitoringStream(process.getErrorStream(), true);
    }
    
    private void pumpStandardInput(CharSequence data) {
        OutputStream os = process.getOutputStream();
        try {
            // As of Java 6, using PrintWriter so we can "append" a CharSequence is no more efficient than converting it to a String ourselves and passing it directly to BufferedWriter, but one can only hope that this will improve in future...
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, "UTF-8")));
            out.append(data);
            out.flush();
            out.close();
        } catch (Exception ex) {
            Log.warn("Problem pumping standard input for task \"" + command + "\"", ex);
            errorsWindow.appendLines(true, Collections.singletonList("Problem pumping standard input for task \"" + command + "\": " + ex.getMessage() + "."));
        } finally {
            try {
                os.close();
            } catch (IOException ex) {
                errorsWindow.appendLines(true, Collections.singletonList("Couldn't close standard input for task \"" + command + "\": " + ex.getMessage() + "."));
            }
        }
    }
    
    private String chooseStandardInputData() {
        String result = "";
        if (textWindow != null) {
            PTextArea textArea = textWindow.getTextArea();
            switch (inputDisposition) {
            case NO_INPUT:
                break;
            case SELECTION_OR_DOCUMENT:
                result = textArea.hasSelection() ? textArea.getSelectedText() : textArea.getTextBuffer().toString();
                break;
            case DOCUMENT:
                result = textArea.getTextBuffer().toString();
                break;
            }
        }
        return result;
    }
    
    public void startMonitoringStream(InputStream stream, boolean isStdErr) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(stream, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StreamMonitor streamMonitor = new StreamMonitor(bufferedReader, this, isStdErr);
        // Circumvent SwingWorker's MAX_WORKER_THREADS limit, as a ShellCommand may run for arbitrarily long.
        final String threadName = (isStdErr ? "stderr" : "stdout") + " pump for " + command;
        ThreadUtilities.newSingleThreadExecutor(threadName).execute(streamMonitor);
    }
    
    /**
     * Invoked by StreamMonitor.process, on the EDT.
     */
    public void processLines(boolean isStdErr, List<String> lines) {
        switch (outputDisposition) {
        case CREATE_NEW_DOCUMENT:
            Log.warn("CREATE_NEW_DOCUMENT not yet implemented.");
            break;
        case DISCARD:
            break;
        case ERRORS_WINDOW:
            errorsWindow.appendLines(isStdErr, lines);
            break;
        case CLIPBOARD:
        case DIALOG:
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
        errorsWindow.showStatus("Task \"" + command + "\" finished");
        switch (outputDisposition) {
        case CLIPBOARD:
            StringSelection selection = new StringSelection(capturedOutput.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            break;
        case CREATE_NEW_DOCUMENT:
            Log.warn("CREATE_NEW_DOCUMENT not yet implemented.");
            break;
        case DIALOG:
            JFrameUtilities.showTextWindow(Evergreen.getInstance().getFrame(), "Subprocess Output", capturedOutput.toString());
            break;
        case DISCARD:
            break;
        case ERRORS_WINDOW:
            // We dealt with the sub-process output as we went along.
            break;
        case INSERT:
            textWindow.getTextArea().replaceSelection(capturedOutput);
            break;
        case REPLACE:
            PTextArea textArea = textWindow.getTextArea();
            if (textArea.hasSelection()) {
                textArea.replaceSelection(capturedOutput);
            } else {
                textArea.setText(capturedOutput);
            }
            break;
        }
        
        // A non-zero exit status is always potentially interesting.
        if (exitStatus != 0) {
            errorsWindow.appendLines(true, Collections.singletonList("Task \"" + command + "\" failed with exit status " + exitStatus));
        }
        errorsWindow.taskDidExit(exitStatus);
    }
    
    /**
     * Invoked by StreamMonitor when one of this task's streams is opened.
     */
    public synchronized void streamOpened() {
        ++openStreamCount;
    }
    
    /**
     * Invoked by StreamMonitor on the EDT when one of this task's streams is closed.
     * If there are no streams left open, this task has finished and the completion runnable is run on the EDT.
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
            
            processFinished(exitStatus);
            EventQueue.invokeLater(completionRunnable);
        }
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
}
