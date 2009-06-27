package e.tools;

import e.forms.*;
import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import javax.swing.*;
import org.jessies.os.*;

// make && ./bin/build-ui 'cd ~/Projects/ctags/trunk;make clean;make --print-directory'
// make && ./bin/build-ui .generated/classes/ e.tools.BuildUi 'cd ~/Projects/terminator;make clean;make --print-directory'

public class BuildUi extends MainFrame {
    
    // Matches addresses (such as "filename.ext:line:(col:line:col)?: ").
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^([-A-Za-z0-9_]{4,}\\.[a-z0-9]+:\\d+:) ");
    // We avoid matching the " or ' before a filename.
    // We insist that an interesting extension has between 1 and 4 characters, and contains only alphabetic characters.
    // (There's an additional check later that the extension isn't known to be uninteresting, such as ".o" or ".class".)
    // FIXME: we don't yet have such a check.
    // FIXME: duplicated from Evergreen.
    //private static final Pattern ADDRESS_PATTERN = Pattern.compile("(?:^| |\"|')([^ :\"']+(?:Makefile|\\w+\\.[A-Za-z]{1,4}\\b)([\\d:]+|\\([\\d,]+\\))?)");
    // FIXME: duplicated from Evergreen.
    private static final Pattern MAKE_ENTERING_DIRECTORY_PATTERN = Pattern.compile("^make(?:\\[\\d+\\])?: Entering directory `(.*)'$");
    
    private final String command;
    
    private final ELabel commandLabel;
    private final ELabel currentActionLabel;
    
    private final JProgressBar progressBar;
    private final JButton stopButton;
    
    private final PTextArea errors;
    private final PTextArea transcript;
    
    private Process runningProcess;
    
    public BuildUi(String command) {
        setTitle("BuildUi");
        
        this.command = command;
        this.commandLabel = new ELabel();
        this.currentActionLabel = new ELabel("Starting up...");
        this.progressBar = makeProgressBar();
        this.stopButton = makeStopButton();
        this.errors = makeErrors();
        this.transcript = makeTranscript();
        
        setSize(new Dimension(800, 300));
        setContentPane(makeUi());
    }
    
    private JComponent makeUi() {
        commandLabel.setFont(commandLabel.getFont().deriveFont(Font.ITALIC | Font.BOLD));
        commandLabel.setText(command);
        
        final JPanel panel = new JPanel(new GridBagLayout());
        final RowLayer rowLayer = new RowLayer(panel);
        rowLayer.add(2, commandLabel, 0);
        rowLayer.add(0, currentActionLabel, 0);
        rowLayer.add(4, makeProgressAndStopPanel(), 0);
        rowLayer.addGreedyComponent(10, makeTabbedPane(), 0);
        return panel;
    }
    
    private static JProgressBar makeProgressBar() {
        final JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        return progressBar;
    }
    
    private JButton makeStopButton() {
        final JButton stopButton = new JButton("Stop");
        
        // Make it a nice easy target.
        final Dimension newSize = stopButton.getPreferredSize();
        newSize.width *= 1.5;
        stopButton.setPreferredSize(newSize);
        
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopBuild();
            }
        });
        
        return stopButton;
    }
    
    private JPanel makeProgressAndStopPanel() {
        final JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(stopButton, BorderLayout.EAST);
        return panel;
    }
    
    private static class RowLayer {
        private final JComponent container;
        private final GridBagConstraints constraints;
        private int nextRow;
        
        public RowLayer(JComponent container) {
            this.container = container;
            this.constraints = makeGridBagConstraints();
            this.nextRow = 0;
        }
        
        private static GridBagConstraints makeGridBagConstraints() {
            final GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.insets = new Insets(0, 3, 0, 3); // FIXME: the 3 is to line up the other components with the JTabbedPane.
            constraints.weightx = 1.0;
            constraints.anchor = GridBagConstraints.LINE_START;
            constraints.fill = GridBagConstraints.BOTH;
            return constraints;
        }
        
        public void add(int pixelsBefore, JComponent row, int pixelsAfter) {
            constraints.insets.top = pixelsBefore;
            constraints.insets.bottom = pixelsAfter;
            constraints.gridy = nextRow;
            container.add(row, constraints);
            ++nextRow;
        }
        
        public void addGreedyComponent(int pixelsBefore, JComponent greedy, int pixelsAfter) {
            constraints.weighty = 1.0;
            final Insets oldInsets = constraints.insets;
            constraints.insets = new Insets(0, 0, 0, 0);
            add(pixelsBefore, greedy, pixelsAfter);
            constraints.insets = oldInsets;
            constraints.weighty = 0.0;
        }
    }
    
    private JComponent makeTabbedPane() {
        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Errors", makeScrollable(errors));
        tabbedPane.add("Transcript", makeScrollable(transcript));
        return tabbedPane;
    }
    
    private static JScrollPane makeScrollable(JComponent c) {
        return new JScrollPane(c, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }
    
    private static PTextArea makeErrors() {
        // FIXME: is a text area what we want here?
        final PTextArea errors = new PTextArea();
        return errors;
    }
    
    private static PTextArea makeTranscript() {
        final PTextArea transcript = new PTextArea();
        return transcript;
    }
    
    public void startBuild() {
        new Thread(new Runnable() {
            public void run() {
                final ProcessUtilities.ProcessListener processListener = new ProcessListener();
                final ProcessUtilities.LineListener stdoutListener = new LineListener();
                final ProcessUtilities.LineListener stderrListener = stdoutListener; // FIXME: differentiate the two streams. different colors?
                final int status = ProcessUtilities.runCommand(null, ProcessUtilities.makeShellCommandArray(command), processListener, stdoutListener, stderrListener);
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        runningProcess = null;
                        currentActionLabel.setText("Finished."); // FIXME: cull something more useful from the transcript? at least include the exit status?
                        currentActionLabel.setForeground(status == 0 ? new Color(0x008800) : Color.RED);
                        progressBar.setIndeterminate(false); // FIXME: and setVisible(false)?
                        stopButton.setEnabled(false);
                    }
                });
            }
        }).start();
    }
    
    public void stopBuild() {
        final int pid = ProcessUtilities.getProcessId(runningProcess);
        Posix.killpg(pid, Signal.SIGINT);
        Posix.killpg(pid, Signal.SIGTERM);        
    }
    
    private class ProcessListener implements ProcessUtilities.ProcessListener {
        public void processStarted(Process process) {
            runningProcess = process;
        }
        
        public void processExited(int status) {
            // We don't care. We're blocked waiting for this notification elsewhere anyway.
        }
    }
    
    private class LineListener implements ProcessUtilities.LineListener {
        public void processLine(String line) {
            int newPercentage = -1;
            String newCurrentAction = null;
            boolean isError = false;
            
            final Matcher makeMatcher = MAKE_ENTERING_DIRECTORY_PATTERN.matcher(line);
            if (makeMatcher.matches()) {
                newCurrentAction = makeMatcher.group(1);
            }
            
            final Matcher addressMatcher = ADDRESS_PATTERN.matcher(line);
            if (addressMatcher.find()) {
                // FIXME: we probably want a different kind of object to represent errors, and to collect up all the output for a given error first.
                isError = true; // FIXME: not exactly; it could be a warning.
            }
            
            if (line.startsWith("-- ")) {
                newCurrentAction = line.substring(3);
            }
            
            final String appendableLine = line + "\n";
            EventQueue.invokeLater(new UiUpdater(appendableLine, newPercentage, newCurrentAction, isError));
        }
    }
    
    private class UiUpdater implements Runnable {
        private final String appendableLine;
        private final int newPercentage;
        private final String newCurrentAction;
        private final boolean isError;
        
        public UiUpdater(String appendableLine, int newPercentage, String newCurrentAction, boolean isError) {
            this.appendableLine = appendableLine;
            this.newPercentage = newPercentage;
            this.newCurrentAction = newCurrentAction;
            this.isError = isError;
        }
        
        public void run() {
            if (newPercentage != -1) {
                progressBar.setIndeterminate(false);
                progressBar.setValue(newPercentage);
            }
            if (newCurrentAction != null) {
                currentActionLabel.setText(newCurrentAction);
            }
            transcript.append(appendableLine);
            if (isError) {
                errors.append(appendableLine);
            }
        }
    }
    
    public static void main(String[] args) {
        // FIXME: other options?
        if (args.length != 1) {
            System.err.println("usage: BuildUi <command>");
            System.exit(1);
        }
        final String command = args[0];
        
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                GuiUtilities.initLookAndFeel();
                BuildUi ui = new BuildUi(command);
                ui.setVisible(true);
                ui.startBuild();
            }
        });
    }
}
