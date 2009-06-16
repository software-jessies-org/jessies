package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import e.ptextarea.*;
import e.gui.*;
import e.util.*;

/**
 * Keeps track of the current directory based on the messages about
 * entering and leaving directories in GNU Make output.
 * 
 * This was called on the event dispatch thread via EErrorsWindow.append,
 * rather than ErrorLinkStyler (where it used to be) because the link
 * styler is called over and over again as we're redrawn or the mouse
 * moves across us, et cetera. Unfortunately, this wasn't the right place
 * either, because the styler still has to use this information to try
 * to turn a relative name into an absolute name, and a big recursive
 * build may have moved on since the output we were currently redrawing.
 * 
 * As far as I know, the best solution is to make sure that your
 * build output only contains absolute names, and to not rely on this
 * code to do anything useful.
 */
public class EErrorsWindow extends JFrame {
    private static final Pattern MAKE_ENTERING_DIRECTORY_PATTERN = Pattern.compile("^make(?:\\[\\d+\\])?: Entering directory `(.*)'$", Pattern.MULTILINE);
    
    /**
     * Matches lines in a Java stack trace, such as "package.Class$Inner$1.method(Class.java:line)"
     */
    private static final Pattern JAVA_STACK_TRACE_PATTERN = Pattern.compile("([\\.\\w]+)(?:(?:\\$\\w+)*?\\.)[\\w\\$<>]+\\(\\w+\\.java(:\\d+)");
    
    private static final ClearErrorsAction CLEAR_ERRORS_ACTION = new ClearErrorsAction();
    
    private final Workspace workspace;
    private JButton killButton;
    private PTextArea textArea;
    private JScrollPane scrollPane;
    private EStatusBar statusBar;
    private int currentBuildErrorCount;
    private Process process;
    
    private boolean shouldAutoScroll;
    private ChangeListener autoScroller;
    
    public EErrorsWindow(Workspace workspace, String title) {
        super(title);
        this.workspace = workspace;
        initKillButton();
        initTextArea();
        initStatusBar();
        this.scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        JPanel bottomLine = new JPanel(new BorderLayout(4, 0));
        bottomLine.add(killButton, BorderLayout.WEST);
        bottomLine.add(statusBar, BorderLayout.CENTER);
        
        add(scrollPane, BorderLayout.CENTER);
        add(bottomLine, BorderLayout.SOUTH);
        pack();
        JFrameUtilities.setFrameIcon(this);
        setLocationRelativeTo(workspace);
        JFrameUtilities.restoreBounds(this.getTitle(), this);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                JFrameUtilities.storeBounds(getTitle(), EErrorsWindow.this);
            }
            @Override
            public void componentResized(ComponentEvent e) {
                JFrameUtilities.storeBounds(getTitle(), EErrorsWindow.this);
            }
        });
        initKeyboardEquivalents();
        
        enableAutoScroll();
    }
    
    private void initKeyboardEquivalents() {
        final String CLEAR_ERRORS_ACTION_NAME = "e.edit.ClearErrorsAction";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), CLEAR_ERRORS_ACTION_NAME);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put((KeyStroke) CLEAR_ERRORS_ACTION.getValue(Action.ACCELERATOR_KEY), CLEAR_ERRORS_ACTION_NAME);
        getRootPane().getActionMap().put(CLEAR_ERRORS_ACTION_NAME, CLEAR_ERRORS_ACTION);
    }
    
    private void initKillButton() {
        killButton = Buttons.makeStopButton();
        killButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ProcessUtilities.terminateProcess(process);
            }
        });
    }
    
    private void initTextArea() {
        textArea = new PTextArea(20, 80);
        initFont();
        // No margin, because all the text should be machine-generated.
        textArea.showRightHandMarginAt(PTextArea.NO_MARGIN);
        textArea.addStyleApplicator(new ErrorLinkStyler(textArea));
        // Traditionally we were editable in imitation of acme.
        // PTextArea currently decides whether you have to hold down control to follow a link based on whether the text area is editable.
        // The easiest fix, which may or may not be sufficient, is to make errors windows non-editable.
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        initTextAreaPopupMenu();
    }
    
    public void initFont() {
        // Default to a fixed-pitch font in errors windows.
        textArea.setFont(ChangeFontAction.getConfiguredFixedFont());
    }
    
    private void initStatusBar() {
        statusBar = new EStatusBar();
    }
    
    public void showStatus(String status) {
        statusBar.setText(status);
    }
    
    public void taskDidStart(Process process) {
        EventQueue.invokeLater(new ClearRunnable());
        this.currentBuildErrorCount = 0;
        this.process = process;
        killButton.setEnabled(true);
    }
    
    public void taskDidExit(int exitStatus) {
        killButton.setEnabled(false);
        this.process = null;
        if (exitStatus == 0 && currentBuildErrorCount == 0) {
            Thread waitThenHide = new Thread(new Runnable() {
                public void run() {
                    // Add a short pause before hiding, so the user gets chance to see that everything went okay.
                    try {
                        Thread.sleep(500);
                    } catch (Exception ex) {
                    }
                    // Now hide.
                    // We invokeLater because append does, and we might need to catch up with pending output before hiding.
                    EventQueue.invokeLater(new HideRunnable());
                }
            });
            waitThenHide.start();
        }
    }
    
    private class ErrorLinkStyler extends RegularExpressionStyleApplicator {
        /**
         * Matches addresses (such as "filename.ext:line:col:line:col").
         * 
         * We avoid matching the " or ' before a filename.
         * We insist that an interesting extension has between 1 and 4 characters, and contains only alphabetic characters.
         * (There's an additional check later that the extension isn't known to be uninteresting, such as ".o" or ".class".)
         */
        private static final String ADDRESS_PATTERN = "(?:^| |\"|')([^ :\"']+(?:Makefile|\\w+\\.[A-Za-z]{1,4}\\b)([\\d:]+|\\([\\d,]+\\))?)";
        
        public ErrorLinkStyler(PTextArea textArea) {
            super(textArea, ADDRESS_PATTERN, PStyle.HYPERLINK);
        }
        
        @Override
        public boolean isAcceptableMatch(CharSequence line, Matcher matcher) {
            String match = matcher.group(1);
            
            FileIgnorer fileIgnorer = workspace.getFileList().getFileIgnorer();
            if (fileIgnorer.isIgnoredExtension(match)) {
                return false;
            }
            
            return true;
        }
        
        @Override
        protected void configureSegment(PTextSegment segment, Matcher matcher) {
            segment.setLinkAction(new ErrorLinkActionListener(matcher.group(1), segment.getOffset()));
        }
    }
    
    private class ErrorLinkActionListener implements ActionListener {
        private final String address;
        private final int offset;
        
        public ErrorLinkActionListener(String address, int offset) {
            this.address = address;
            this.offset = offset;
        }
        
        public void actionPerformed(ActionEvent e) {
            // The link was probably a combination of filename and address within the file.
            // Break that into the two components.
            String name = address;
            String tail ="";
            int colonIndex = name.indexOf(':');
            if (colonIndex != -1) {
                // A traditional Unix error such as "src/Trousers.cpp:109:26: parse error".
                name = name.substring(0, colonIndex);
                tail = address.substring(colonIndex);
            } else if (name.endsWith(")")) {
                // Maybe a Microsoft-style error such as "src/Trousers.cs(109,26): error CS0103: The name `ferret' does not exist in the context of `Trousers'"?
                int openParenthesisIndex = name.indexOf('(');
                if (openParenthesisIndex != -1) {
                    name = name.substring(0, openParenthesisIndex);
                    tail = ":" + address.substring(openParenthesisIndex + 1, address.length() - 1).replace(',', ':');
                }
            }
            
            if (name.startsWith("/") || name.startsWith("~")) {
                open(address);
                return;
            }
            
            Matcher matcher = JAVA_STACK_TRACE_PATTERN.matcher(address);
            if (matcher.matches()) {
                handleJavaStackTraceMatch(matcher);
            } else {
                handleNonCanonicalFilename(name, tail);
            }
        }
        
        private void handleJavaStackTraceMatch(Matcher matcher) {
            String dottedClassName = matcher.group(1);
            String colonAddress = matcher.group(2);
            
            List<String> candidates = JavaDoc.findSourceFilenames(dottedClassName);
            if (candidates.size() != 1) {
                // FIXME: if there's any reason why this should ever occur in real life, we could offer a dialog so the user can disambiguate.
                Evergreen.getInstance().showAlert("Can't find matching class", "The class name \"" + dottedClassName + "\" is ambiguous.");
                return;
            }
            
            String fullFilename = candidates.get(0);
            open(fullFilename + colonAddress);
        }
        
        private void handleNonCanonicalFilename(String name, String tail) {
            // Try to resolve the non-canonical filename by finding the last-known directory.
            String currentDirectory = workspace.getRootDirectory();
            String errors = textArea.getText();
            Matcher matcher = MAKE_ENTERING_DIRECTORY_PATTERN.matcher(errors);
            while (matcher.find() && matcher.start() < offset) {
                currentDirectory = matcher.group(1);
            }
            open(currentDirectory + File.separator + name + tail);
        }
        
        private void open(String address) {
            Evergreen.getInstance().openFile(address);
        }
    }
    
    public void requestFocus() {
        textArea.requestFocus();
    }
    
    private class AppendRunnable implements Runnable {
        private boolean isStdErr;
        private String text;
        
        public AppendRunnable(boolean isStdErr, List<String> lines) {
            this.isStdErr = isStdErr;
            this.text = StringUtilities.join(lines, "\n") + "\n";
        }
        
        public void run() {
            // This conditional stops the errors window from grabbing the focus every time it's updated.
            if (isVisible() == false) {
                setVisible(true);
            }
            textArea.append(text);
            if (isStdErr) {
                disableAutoScroll();
            }
        }
    }
    
    private class ClearRunnable implements Runnable {
        public void run() {
            textArea.setText("");
            textArea.getTextBuffer().getUndoBuffer().resetUndoBuffer();
            enableAutoScroll();
        }
    }
    
    private class HideRunnable implements Runnable {
        public void run() {
            setVisible(false);
            if (process == null) {
                workspace.destroyErrorsWindow(EErrorsWindow.this);
                dispose();
            }
        }
    }
    
    public void appendLines(boolean isStdErr, List<String> lines) {
        for (String line : lines) {
            // FIXME: this is a bit weak, and no longer necessary for our builds. The FIXME in this file about treating stderr specially might be a better way forward if we want to keep a hack.
            if (line.contains("***") || line.contains("warning:")) {
                ++currentBuildErrorCount;
            }
        }
        EventQueue.invokeLater(new AppendRunnable(isStdErr, lines));
    }
    
    public void clearErrors() {
        EventQueue.invokeLater(new ClearRunnable());
        EventQueue.invokeLater(new HideRunnable());
    }
    
    public synchronized void enableAutoScroll() {
        if (shouldAutoScroll == false) {
            this.shouldAutoScroll = true;
            this.autoScroller = GuiUtilities.keepMaximumShowing(scrollPane.getVerticalScrollBar());
        }
    }
    
    public synchronized void disableAutoScroll() {
        if (shouldAutoScroll == true) {
            this.shouldAutoScroll = false;
            GuiUtilities.stopMaximumShowing(scrollPane.getVerticalScrollBar(), autoScroller);
            this.autoScroller = null;
        }
    }
    
    private void initTextAreaPopupMenu() {
        textArea.getPopupMenu().addMenuItemProvider(new MenuItemProvider() {
            public void provideMenuItems(MouseEvent e, Collection<Action> actions) {
                actions.add(new OpenQuicklyAction());
                actions.add(new FindInFilesAction());
                actions.add(null);
                actions.add(new CheckInChangesAction());
                actions.add(null);
                actions.add(CLEAR_ERRORS_ACTION);
                EPopupMenu.addNumberInfoItems(actions, textArea.getSelectedText());
            }
        });
    }
}
