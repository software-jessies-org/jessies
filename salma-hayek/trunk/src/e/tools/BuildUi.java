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
import org.jessies.test.*;

// make && ./bin/build-ui 'cd ~/Projects/ctags/trunk;make clean;make --print-directory'
// make && ./bin/build-ui .generated/classes/ e.tools.BuildUi 'cd ~/Projects/terminator;make clean;make --print-directory'

public class BuildUi extends MainFrame {
    // Matches addresses (such as "/some/path/to/filename.ext:line:(col:line:col)?: ").
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^([-A-Za-z0-9_/]{4,}\\.[a-z0-9]+:\\d+:) ");
    
    @Test private static void testAddressPattern() {
        Assert.matches(ADDRESS_PATTERN,"test.cpp:4: oops", "test.cpp:4:");
        Assert.matches(ADDRESS_PATTERN,"/usr/include/stdio.h:123: oops", "/usr/include/stdio.h:123:");
    }
    
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
        GnomeStockIcon.configureButton(stopButton);
        
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
        final JScrollPane scrollableErrors = makeScrollable(errors);
        final JScrollPane scrollableTranscript = makeScrollable(transcript);
        // Make the transcript automatically scroll as new output appears, on the assumption that the most recent output is the most interesting.
        // Don't scroll the errors, on the assumption that you'll want to start at the beginning.
        GuiUtilities.keepMaximumShowing(scrollableTranscript.getVerticalScrollBar());
        
        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Errors", scrollableErrors);
        tabbedPane.add("Transcript", scrollableTranscript);
        return tabbedPane;
    }
    
    private static JScrollPane makeScrollable(JComponent c) {
        return new JScrollPane(c, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }
    
    private PTextArea makeTextArea() {
        final PTextArea textArea = new PTextArea();
        // No margin, because all the text should be machine-generated.
        textArea.showRightHandMarginAt(PTextArea.NO_MARGIN);
        textArea.addStyleApplicator(new ErrorLinkStyler(textArea));
        // Traditionally we were editable in imitation of acme.
        // PTextArea currently decides whether you have to hold down control to follow a link based on whether the text area is editable.
        // The easiest fix, which may or may not be sufficient, is to make errors windows non-editable.
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        return textArea;
    }
    
    private PTextArea makeErrors() {
        // FIXME: is a text area what we want here?
        final PTextArea errors = makeTextArea();
        return errors;
    }
    
    private class ErrorLinkStyler extends RegularExpressionStyleApplicator {
        public ErrorLinkStyler(PTextArea textArea) {
            super(textArea, ADDRESS_PATTERN, PStyle.HYPERLINK);
        }
        
        @Override protected void configureSegment(PTextSegment segment, Matcher matcher) {
            segment.setLinkAction(new ErrorLinkActionListener(matcher.group(1)));
        }
    }
    
    private class ErrorLinkActionListener implements ActionListener {
        private final String address;
        
        public ErrorLinkActionListener(String address) {
            this.address = address;
        }
        
        public void actionPerformed(ActionEvent e) {
            // FIXME: expose --editor as a command-line option.
            // FIXME: use environment variables to supply BUI_FILENAME, BUI_LINE_NUMBER, and BUI_ADDRESS to support other editors.
            // FIXME: don't use spawn because we should report failures.
            final String[] command = ProcessUtilities.makeShellCommandArray("evergreen " + address);
            ProcessUtilities.spawn(null, command);
        }
    }
    
    private PTextArea makeTranscript() {
        final PTextArea transcript = makeTextArea();
        return transcript;
    }
    
    public void startBuild() {
        ThreadUtilities.newSingleThreadExecutor("build runner").execute(new Runnable() {
            public void run() {
                final ProcessUtilities.ProcessListener processListener = new ProcessListener();
                final ProcessUtilities.LineListener stdoutListener = new LineListener();
                final ProcessUtilities.LineListener stderrListener = stdoutListener; // FIXME: differentiate the two streams. different colors?
                final int status = ProcessUtilities.runCommand(null, ProcessUtilities.makeShellCommandArray(command), processListener, stdoutListener, stderrListener);
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        buildFinished(status);
                    }
                });
            }
        });
    }
    
    public void stopBuild() {
        final int pid = ProcessUtilities.getProcessId(runningProcess);
        Posix.killpg(pid, Signal.SIGINT);
        Posix.killpg(pid, Signal.SIGTERM);
    }
    
    // Invoked on the EDT.
    private void buildFinished(int exitStatus) {
        // The user can't stop what's already finished.
        runningProcess = null;
        stopButton.setEnabled(false);
        stopButton.setVisible(false);
        // And we're not going to make any more progress.
        progressBar.setVisible(false);
        
        final boolean success = (exitStatus == 0);
        // FIXME: extract something more specific from the transcript?
        currentActionLabel.setText(success ? "Built successfully." : "Build failed.");
        currentActionLabel.setForeground(success ? new Color(0x008800) : Color.RED);
        
        // FIXME: option to exit on success.
    }
    
    private class ProcessListener implements ProcessUtilities.ProcessListener {
        public void processStarted(Process process) {
            runningProcess = process;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    currentActionLabel.setText("Child started...");
                }
            });
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
            
            // FIXME: shouldn't have to-hard code support for arbitrary builds.
            if (line.startsWith("-- ")) {
                newCurrentAction = line.substring(3);
            }
            
            // FIXME: shouldn't have to-hard code support for arbitrary builds.
            if (line.startsWith("____")) {
                final Matcher detailedMatcher = Pattern.compile("^____\\(... ... .. ..:..:.. ... ....\\) \\[(\\d+)%\\] (.*)$").matcher(line);
                if (detailedMatcher.matches()) {
                    newPercentage = Integer.parseInt(detailedMatcher.group(1));
                    newCurrentAction = detailedMatcher.group(2);
                }
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
                progressBar.setStringPainted(true);
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
        // FIXME: options!
        // FIXME: --editor COMMAND
        // FIXME: --exit-on-success
        // FIXME: --source-path DIRS
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
