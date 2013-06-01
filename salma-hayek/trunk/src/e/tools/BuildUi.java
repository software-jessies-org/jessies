package e.tools;

import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import org.jessies.cli.*;
import org.jessies.os.*;
import org.jessies.test.*;

// make && ./bin/build-ui 'cd ~/Projects/ctags/trunk;make clean;make --print-directory'
// make && ./bin/build-ui .generated/classes/ e.tools.BuildUi 'cd ~/Projects/terminator;make clean;make --print-directory'

// FIXME: we don't need a gigabyte heap.
// FIXME: we should remember where we were last time we ran, and re-position ourselves there.
public class BuildUi extends JFrame {
    // Matches addresses (such as "/some/path/to/filename.ext:line:(col:line:col)?: ").
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^([-A-Za-z0-9_/]{4,}\\.[a-z0-9]+:\\d+:) ");
    
    @Test private static void testAddressPattern() {
        Assert.matches(ADDRESS_PATTERN, "test.cpp:4: oops", "test.cpp:4:");
        Assert.matches(ADDRESS_PATTERN, "/usr/include/stdio.h:123: oops", "/usr/include/stdio.h:123:");
    }
    
    // FIXME: duplicated from Evergreen.
    // FIXME: gcc has started using UTF-8 quotes; will make?
    private static final Pattern MAKE_ENTERING_DIRECTORY_PATTERN = Pattern.compile("^make(?:\\[\\d+\\])?: Entering directory `(.*)'$");
    
    @Test private static void testMakeEnteringDirectoryPattern() {
        Assert.matches(MAKE_ENTERING_DIRECTORY_PATTERN, "make: Entering directory `/home/elliotth/Projects/terminator'", "/home/elliotth/Projects/terminator");
        Assert.matches(MAKE_ENTERING_DIRECTORY_PATTERN, "make[1]: Entering directory `/home/elliotth/Projects/terminator'", "/home/elliotth/Projects/terminator");
    }
    
    // When we see an address, we start copying lines to cope with javac's multi-line output.
    // When we see this pattern, we know javac's finished.
    private static final Pattern END_OF_JAVAC_OUTPUT_PATTERN = Pattern.compile("^\\d+ errors?");
    
    // Our command-line options and their defaults.
    private static class Options {
        @Option(names = { "--exit-on-success" })
        public boolean exitOnSuccess = true;
        
        @Option(names = { "--source-path" })
        public String sourcePath = ".";
        
        @Option(names = { "--editor" })
        public String editor = "evergreen";
    }
    
    private final Options options;
    private final String command;
    
    private final ELabel commandLabel;
    private final ELabel currentActionLabel;
    
    private final JProgressBar progressBar;
    private final JButton stopButton;
    
    private final PTextArea errors;
    private final PTextArea transcript;
    
    private Process runningProcess;
    
    public BuildUi(Options options, String command) {
        setTitle("BuildUi");
        
        this.options = options;
        this.command = command;
        this.commandLabel = new ELabel();
        this.currentActionLabel = new ELabel("Starting up...");
        this.progressBar = makeProgressBar();
        this.stopButton = makeStopButton();
        this.errors = makeErrors();
        this.transcript = makeTranscript();
        
        setSize(new Dimension(800, 300));
        setContentPane(makeUi());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JFrameUtilities.constrainToScreen(this);
        JFrameUtilities.setFrameIcon(this);
        
        GuiUtilities.finishGnomeStartup();
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
        // FIXME: turn the spelling checker off; it's way too distracting in build transcripts.
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
            final AddressSplitter splitAddress = new AddressSplitter(address);
            
            String filename = splitAddress.filename;
            if (!filename.startsWith("/") && !filename.startsWith("~")) {
                // Try to work out where a relative path is supposed to point.
                final String[] path = options.sourcePath.split(";");
                for (String directory : path) {
                    final File file = FileUtilities.fileFromParentAndString(directory, filename);
                    if (file.exists()) {
                        filename = file.toString();
                        break;
                    }
                }
            }
            
            final File file = FileUtilities.fileFromString(filename);
            if (!file.exists()) {
                SimpleDialog.showAlert(BuildUi.this, "No such file.", "Can't find \"" + filename + "\".");
                return;
            }
            
            // FIXME: use environment variables to supply BUI_FILENAME, BUI_LINE_NUMBER, and BUI_ADDRESS to support other editors.
            // FIXME: don't use spawn because we should report failures.
            final String[] command = ProcessUtilities.makeShellCommandArray("'" + options.editor + "' '" + address + "'");
            ProcessUtilities.spawn(null, command);
        }
    }
    
    private static class AddressSplitter {
        public final String filename;
        public final String lineNumber;
        
        public AddressSplitter(String address) {
            // The link was probably a combination of filename and address within the file.
            // Break that into the two components.
            String name = address;
            String tail = "";
            final int colonIndex = name.indexOf(':');
            if (colonIndex != -1) {
                // A traditional Unix error such as "src/Trousers.cpp:109:26: parse error".
                name = name.substring(0, colonIndex);
                tail = address.substring(colonIndex + 1);
            } else if (name.endsWith(")")) {
                // Maybe a Microsoft-style error such as "src/Trousers.cs(109,26): error CS0103: The name `ferret' does not exist in the context of `Trousers'"?
                final int openParenthesisIndex = name.indexOf('(');
                if (openParenthesisIndex != -1) {
                    name = name.substring(0, openParenthesisIndex);
                    tail = address.substring(openParenthesisIndex + 1, address.length() - 1).replace(',', ':');
                }
            }
            
            // We only want the line number, so lose any more detailed context.
            tail = tail.replaceAll(":.*", "");
            
            this.filename = name;
            this.lineNumber = tail;
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
                final int status = ProcessUtilities.runCommand(null, ProcessUtilities.makeShellCommandArray(command), processListener, "", stdoutListener, stderrListener);
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
        
        if (success && options.exitOnSuccess) {
            // FIXME: something less brutal?
            System.exit(0);
        }
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
    /*
    @Test private static void testLineListener() {
        FIXME: examples to test.
    
        // Single error, relevant line, no extra detail.
        "make[1]: Entering directory `/tmp'",
        "javac source.java",
        "source.java:1: some error",
        "    x = something problematic;"
        "                 ^",
        "1 error",
        "make[1]: *** [/tmp/blah] Error 1",
        "make[1]: Leaving directory `/tmp'",
        "make: *** [recurse] Error 2",
    
        // Single error, relevant line, some extra detail.
        "make[1]: Entering directory `/tmp'",
        "src/terminator/terminal/PtyProcess.java:29: method read in class org.jessies.os.Posix cannot be applied to given types",
        "        while ((n = Posix.read(fd, bytes, arrayOffset, byteCount, 0)) < 0) {",
        "                         ^",
        "  required: int,byte[],int,int",
        "  found: int,byte[],int,int,int",
        "1 error",
        "make[1]: Leaving directory `/tmp'",
        
        // Multiple errors.
    
        // C++ errors.
    }
    */
    private class LineListener implements ProcessUtilities.LineListener {
        private boolean copyingMultiLineError = false;
        
        public void processLine(String line) {
            int newPercentage = -1;
            String newCurrentAction = null;
            boolean isError = false;
            
            final Matcher makeMatcher = MAKE_ENTERING_DIRECTORY_PATTERN.matcher(line);
            if (makeMatcher.matches()) {
                newCurrentAction = makeMatcher.group(1);
                copyingMultiLineError = false;
            }
            
            final Matcher addressMatcher = ADDRESS_PATTERN.matcher(line);
            if (addressMatcher.find()) {
                // FIXME: we probably want a different kind of object to represent errors, and to collect up all the output for a given error first.
                isError = true; // FIXME: not exactly; it could be a warning.
                // FIXME: we don't have to do this by default. we could recognize (probable) javac output from the .java filename in the match.
                copyingMultiLineError = true;
            }
            
            // FIXME: option to ignore javac "context" output (line and "^") but keep extra detail (lines matching "^  \S+: ").
            final Matcher endOfJavacOutputMatcher = END_OF_JAVAC_OUTPUT_PATTERN.matcher(line);
            if (endOfJavacOutputMatcher.find()) {
                copyingMultiLineError = false;
            }
            
            // FIXME: shouldn't have to hard-code support for arbitrary builds.
            if (line.startsWith("-- ")) {
                newCurrentAction = line.substring(3);
                copyingMultiLineError = false;
            }
            
            // FIXME: shouldn't have to hard-code support for arbitrary builds.
            if (line.startsWith("____")) {
                copyingMultiLineError = false;
                final Matcher detailedMatcher = Pattern.compile("^____(?:\\(... ... .. ..:..:.. ... ....\\) )?(?:\\[(\\d+)%\\] )?(.*)$").matcher(line);
                if (detailedMatcher.matches()) {
                    if (detailedMatcher.group(1) != null) {
                        newPercentage = Integer.parseInt(detailedMatcher.group(1));
                    }
                    newCurrentAction = detailedMatcher.group(2);
                }
            }
            
            final String appendableLine = line + "\n";
            EventQueue.invokeLater(new UiUpdater(appendableLine, newPercentage, newCurrentAction, isError || copyingMultiLineError));
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
        // FIXME: start a GUI anyway and report any problems that way?
        final Options options = new Options();
        final List<String> rest = new OptionParser(options).parse(args);
        if (rest.size() != 1) {
            System.err.println("usage: BuildUi <command>");
            System.exit(1);
        }
        final String command = rest.get(0);
        
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                GuiUtilities.initLookAndFeel();
                final BuildUi ui = new BuildUi(options, command);
                ui.setVisible(true);
                ui.startBuild();
            }
        });
    }
}
