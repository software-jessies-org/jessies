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

public class EErrorsWindow extends EWindow {
    private PTextArea textArea;
    
    public EErrorsWindow(String filename) {
        super(filename);
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
        textArea.setTextStyler(new ErrorLinkStyler(textArea));
        textArea.setWrapStyleWord(true);
        initTextAreaPopupMenu();
    }

    private static class ErrorLinkStyler extends PHyperlinkTextStyler {
        /**
         * Matches addresses (such as "filename.ext:line:col:line:col").
         * 
         * We could also insist that there are more than 2 characters,
         * and that an address can't end in a /. But this seems to work
         * pretty nicely.
         */
        private static final String ADDRESS_PATTERN = "(?:^| |\")([^ :\"]+(?:Makefile|\\w+\\.\\w+)([\\d:]+)?)";
        
        private static final Pattern MAKE_DIRECTORY_CHANGE = Pattern.compile("^make(?:\\[\\d+\\])?: (Entering|Leaving) directory `(.*)'$");
        
        private File currentDirectory;
        
        public ErrorLinkStyler(PTextArea textArea) {
            super(textArea, ADDRESS_PATTERN);
        }
        
        public void hyperlinkClicked(CharSequence hyperlinkText, Matcher matcher) {
            Edit.openFile(hyperlinkText.toString());
        }
        
        public boolean isAcceptableMatch(CharSequence line, Matcher address) {
            Matcher directoryChangeMatcher = MAKE_DIRECTORY_CHANGE.matcher(line);
            if (directoryChangeMatcher.find()) {
                if (directoryChangeMatcher.group(1).equals("Entering")) {
                    currentDirectory(directoryChangeMatcher.group(2));
                }
            }
            
            // We're most useful in providing links to grep matches, so we
            // need to avoid being confused by stuff like File.java:123.
            String name = address.group(1);
            int colonIndex = name.indexOf(':');
            if (colonIndex != -1) {
                name = name.substring(0, colonIndex);
            }
            
            // If the file doesn't exist, this wasn't a useful match.
            File file = null;
            if (name.startsWith("/") || name.startsWith("~")) {
                file = FileUtilities.fileFromString(name);
            } else {
                file = new File(currentDirectory(), name);
            }
            return file.exists();
        }
        
        private void currentDirectory(String directory) {
            currentDirectory = new File(directory);
        }
        
        private File currentDirectory() {
            return currentDirectory;
        }
        
        public void addKeywordsTo(Collection<String> collection) {
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
        SwingUtilities.invokeLater(new AppendRunnable(line));
    }
    
    public void clear() {
        SwingUtilities.invokeLater(new ClearRunnable());
    }
    
    public void drawHorizontalRule() {
        append("-------------------------------------------------------------------------");
    }
    
    public void resetAutoScroll() {
//        linkFormatter.setAutoScroll(true);
    }
    
    /** Errors windows have no initial content. */
    public void fillWithContent() {
    }
    
    public void initTextAreaPopupMenu() {
        textArea.getPopupMenu().addMenuItemProvider(new MenuItemProvider() {
            public void provideMenuItems(MouseEvent e, Collection<Action> actions) {
                actions.add(new OpenQuicklyAction());
                actions.add(new FindFilesContainingSelectionAction());
                actions.add(null);
                actions.add(new ClearErrorsAction());
            }
        });
    }
    
    public class ClearErrorsAction extends AbstractAction {
        public ClearErrorsAction() {
            super("Clear");
        }
        public void actionPerformed(ActionEvent e) {
            clear();
        }
    }
    
    /** Errors windows are never considered dirty because they're not worth preserving. */
    public boolean isDirty() {
        return false;
    }
    
    public String getContext() {
        return "";
    }
}
