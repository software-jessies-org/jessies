package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
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
public class EErrorsWindow extends EWindow {
    private static final Pattern MAKE_ENTERING_DIRECTORY_PATTERN = Pattern.compile("^make(?:\\[\\d+\\])?: Entering directory `(.*)'$", Pattern.MULTILINE);
    
    private final Workspace workspace;
    private PTextArea textArea;
    
    public EErrorsWindow(Workspace workspace) {
        super("+Errors");
        this.workspace = workspace;
        initTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        GuiUtilities.keepMaximumShowing(scrollPane.getVerticalScrollBar());
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void initTextArea() {
        textArea = new PTextArea();
        // Default to a fixed-pitch font in errors windows.
        textArea.setFont(ChangeFontAction.getConfiguredFixedFont());
        // But no margin, because all the text should be machine-generated.
        textArea.showRightHandMarginAt(PTextArea.NO_MARGIN);
        textArea.addStyleApplicator(new ErrorLinkStyler(textArea));
        textArea.setWrapStyleWord(true);
        initTextAreaPopupMenu();
    }

    private class ErrorLinkStyler extends RegularExpressionStyleApplicator {
        /**
         * Matches addresses (such as "filename.ext:line:col:line:col").
         * 
         * We could also insist that there are more than 2 characters,
         * and that an address can't end in a /. But this seems to work
         * pretty nicely.
         */
        private static final String ADDRESS_PATTERN = "(?:^| |\")([^ :\"]+(?:Makefile|\\w+\\.\\w+)([\\d:]+)?)";
        
        public ErrorLinkStyler(PTextArea textArea) {
            super(textArea, ADDRESS_PATTERN, PStyle.HYPERLINK);
        }
        
        @Override
        protected void configureSegment(PTextSegment segment, Matcher matcher) {
            segment.setLinkAction(new ErrorLinkActionListener(matcher.group(1)));
        }
    }
    
    private class ErrorLinkActionListener implements ActionListener {
        private final String address;
        
        public ErrorLinkActionListener(String address) {
            this.address = address;
        }
        
        public void actionPerformed(ActionEvent e) {
            // We're most useful in providing links to grep matches, so we
            // need to avoid being confused by stuff like File.java:123.
            String name = address;
            int colonIndex = name.indexOf(':');
            if (colonIndex != -1) {
                name = name.substring(0, colonIndex);
            }
            String tail = address.substring(colonIndex);
            
            File file = null;
            if (name.startsWith("/") || name.startsWith("~")) {
                open(address);
            } else {
                // Try to resolve the non-canonical name.
                String currentDirectory = workspace.getRootDirectory();
                String errors = textArea.getText();
                // FIXME: we shouldn't search past the point at which the user clicked, but how do we know where that is?
                Matcher matcher = MAKE_ENTERING_DIRECTORY_PATTERN.matcher(errors);
                while (matcher.find()) {
                    currentDirectory = matcher.group(1);
                }
                open(currentDirectory + File.separator + name + tail);
            }
        }
        
        private void open(String address) {
            Edit.getInstance().openFile(address);
        }
    }
    
    public void requestFocus() {
        textArea.requestFocus();
    }
    
    private class AppendRunnable implements Runnable {
        private String line;
        
        public AppendRunnable(String line) {
            this.line = line + "\n";
        }
        
        public void run() {
            textArea.append(line);
        }
    }
    
    private class ClearRunnable implements Runnable {
        public void run() {
            textArea.setText("");
            textArea.getTextBuffer().getUndoBuffer().resetUndoBuffer();
            resetAutoScroll();
        }
    }
    
    public void append(String line) {
        EventQueue.invokeLater(new AppendRunnable(line));
    }
    
    public void clear() {
        EventQueue.invokeLater(new ClearRunnable());
    }
    
    public void drawHorizontalRule() {
        append("-------------------------------------------------------------------------");
    }
    
    public void resetAutoScroll() {
        // FIXME: differentiate between stdout and stderr, and pause scrolling as soon as we see anything on stderr.
        //linkFormatter.setAutoScroll(true);
    }
    
    public void initTextAreaPopupMenu() {
        textArea.getPopupMenu().addMenuItemProvider(new MenuItemProvider() {
            public void provideMenuItems(MouseEvent e, Collection<Action> actions) {
                actions.add(new OpenQuicklyAction());
                actions.add(new FindFilesContainingSelectionAction());
                actions.add(null);
                actions.add(new KillErrorsAction());
            }
        });
    }
    
    /** Errors windows are never considered dirty because they're not worth preserving. */
    public boolean isDirty() {
        return false;
    }
    
    public String getContext() {
        return "";
    }
}
