package e.edit;

import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;
import org.jdesktop.swingworker.SwingWorker;

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
        environment.put("EVERGREEN_CURRENT_DIRECTORY", FileUtilities.parseUserFriendlyName(context));
        environment.put("EVERGREEN_LAUNCHER", Evergreen.getApplicationFilename());
        environment.put("EVERGREEN_WORKSPACE_ROOT", FileUtilities.parseUserFriendlyName(workspace.getRootDirectory()));
        if (textWindow != null) {
            environment.put("EVERGREEN_CURRENT_FILENAME", FileUtilities.parseUserFriendlyName(textWindow.getFilename()));
            
            // Humans number lines from 1, text components from 0.
            // FIXME: we should pass full selection information.
            PTextArea textArea = textWindow.getTextArea();
            final int currentLineNumber = 1 + textArea.getLineOfOffset(textArea.getSelectionStart());
            environment.put("EVERGREEN_CURRENT_LINE_NUMBER", Integer.toString(currentLineNumber));
        }
        return processBuilder;
    }

    public void runCommand() throws IOException {
        final String data = chooseStandardInputData();
        
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
        
        ThreadUtilities.newSingleThreadExecutor("stdin pump for " + command).execute(new StandardInputPump(data));
        startMonitoringStream(process.getInputStream(), false);
        startMonitoringStream(process.getErrorStream(), true);
    }
    
    private class StandardInputPump implements Runnable {
        private final String utf8;
        
        private StandardInputPump(String utf8) {
            this.utf8 = utf8;
        }
        
        public void run() {
            OutputStream os = process.getOutputStream();
            try {
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                out.append(utf8);
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
    
    private void startMonitoringStream(InputStream stream, boolean isStdErr) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(stream, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StreamMonitor streamMonitor = new StreamMonitor(bufferedReader, isStdErr);
        // Circumvent SwingWorker's MAX_WORKER_THREADS limit, as a ShellCommand may run for arbitrarily long.
        final String threadName = (isStdErr ? "stderr" : "stdout") + " pump for " + command;
        ThreadUtilities.newSingleThreadExecutor(threadName).execute(streamMonitor);
    }
    
    // We use SwingWorker to batch up groups of lines rather than process each one individually.
    private class StreamMonitor extends SwingWorker<Void, String> {
        private final BufferedReader stream;
        private final boolean isStdErr;
        
        private StreamMonitor(BufferedReader stream, boolean isStdErr) {
            this.stream = stream;
            this.isStdErr = isStdErr;
        }
        
        @Override protected Void doInBackground() throws IOException {
            streamOpened();
            String line;
            while ((line = stream.readLine()) != null) {
                publish(line);
            }
            return null;
        }
        
        @Override protected void done() {
            try {
                // Wait for the stream to empty and all lines to have been processed.
                get();
            } catch (Exception ex) {
                Log.warn("Unexpected failure", ex);
            }
            streamClosed();
        }
        
        @Override protected void process(List<String> lines) {
            processLines(isStdErr, lines);
        }
    }
    
    /**
     * Invoked on the EDT by StreamMonitor.process.
     */
    private void processLines(boolean isStdErr, List<String> lines) {
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
     * Invoked on the EDT when the StreamMonitors finish.
     */
    private void processFinished() {
        // Get the process' exit status.
        // FIXME: strictly, we don't know the process exited, only that it closed its streams.
        int exitStatus = 0;
        try {
            exitStatus = process.waitFor();
        } catch (InterruptedException ex) {
            Log.warn("Process.waitFor interrupted", ex);
        }
        
        // Deal with the output we may have collected.
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
        
        // Keep the errors window informed.
        errorsWindow.showStatus("Task \"" + command + "\" finished");
        if (exitStatus != 0) {
            // A non-zero exit status is always potentially interesting.
            errorsWindow.appendLines(true, Collections.singletonList("Task \"" + command + "\" failed with exit status " + exitStatus));
        }
        errorsWindow.taskDidExit(exitStatus);
        
        // Run any user-specified completion code.
        EventQueue.invokeLater(completionRunnable);
    }
    
    /**
     * Invoked by StreamMonitor when one of this task's streams is opened.
     */
    private synchronized void streamOpened() {
        ++openStreamCount;
    }
    
    /**
     * Invoked on the EDT by StreamMonitor when one of this task's streams is closed.
     * If there are no streams left open, we assume the process has exited.
     */
    private synchronized void streamClosed() {
        openStreamCount--;
        if (openStreamCount == 0) {
            processFinished();
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
