package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

public class EErrorsWindow extends EWindow implements LinkListener {
    private JTextPane text;
    
    private LinkRecognizer linkRecognizer;
    private LinkFormatter linkFormatter;
    
    private StyleContext styles = new StyleContext();
    
    public EErrorsWindow(String filename) {
        super(filename);
        this.text = new JTextPane() {
            /**
             * Returns "" instead of null for the empty selection, for
             * compatibility with ETextArea.
             */
            public String getSelectedText() {
                String selection = super.getSelectedText();
                return (selection != null) ? selection : "";
            }
        };
        this.linkRecognizer = new LinkRecognizer(text, this);
        this.linkFormatter = new LinkFormatter(text);
        initTextStyles();
        add(new JScrollPane(text), BorderLayout.CENTER);
        attachPopupMenuTo(text);
        
        // Default to a fixed-pitch font in errors windows.
        text.setFont(ETextArea.getConfiguredFixedFont());
    }

    public void requestFocus() {
        text.requestFocus();
    }
    
    public void initTextStyles() {
        Style plainStyle = styles.addStyle("plain", text.getLogicalStyle());
        linkFormatter.setCurrentStyle(plainStyle);
        
        Style linkStyle = styles.addStyle("link", plainStyle);
        StyleConstants.setForeground(linkStyle, Color.BLUE);
        StyleConstants.setUnderline(linkStyle, true);
        linkFormatter.setLinkStyle(linkStyle);
    }
    
    public void append(String line) {
        linkFormatter.appendLine(line + "\n");
    }
    
    public void clear() {
        text.setText("");
        resetAutoScroll();
    }
    
    /** Appends a JSeparator to the document. */
    public void drawHorizontalRule() {
        // This is a reimplementation of JTextPane.insertComponent to use Document.insertString so that we're not scrolled to the position of the insertion.
        try {
            MutableAttributeSet inputAttributes = text.getInputAttributes();
            inputAttributes.removeAttributes(inputAttributes);
            StyleConstants.setComponent(inputAttributes, new JSeparator());
            text.getDocument().insertString(text.getDocument().getLength(), "\n", null);
            text.getDocument().insertString(text.getDocument().getLength(), "\n", inputAttributes.copyAttributes());
            //text.getDocument().insertString(text.getDocument().getLength(), "\n", null);
            inputAttributes.removeAttributes(inputAttributes);
            linkFormatter.autoScroll();
        } catch (BadLocationException ex) {
            // Can't happen.
            ex.printStackTrace();
        }
    }
    
    public void resetAutoScroll() {
        linkFormatter.setAutoScroll(true);
    }
    
    /** Errors windows have no initial content. */
    public void fillWithContent() {
    }
    
    public Collection getPopupMenuItems() {
        ArrayList items = new ArrayList();
        items.add(new OpenQuicklyAction());
        items.add(new FindFilesContainingSelectionAction());
        items.add(null);
        items.add(new ClearErrorsAction());
        return items;
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
    
    public String getWordSelectionStopChars() {
        return " \t\n";
    }
    
    public String getContext() {
        return "";
    }
    
    /** Removes this as the Workspace's errors window. */
    public void windowClosing() {
        //getWorkspace().errorsWindowClosing();
        getWorkspace().unregisterTextComponent(getText());
    }
    
    public void linkActivated(String link) {
        Edit.openFile(link);
    }
}
