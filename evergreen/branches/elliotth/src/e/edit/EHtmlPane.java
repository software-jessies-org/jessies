package e.edit;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

public class EHtmlPane extends JComponent implements HyperlinkListener {
    private JTextPane textPane;
    private JLabel label;
    
    public EHtmlPane() {
        createTextPane();
        JScrollPane scrollPane = new JScrollPane(textPane);
        //scrollPane.setBorder(javax.swing.border.LineBorder.createBlackLineBorder());
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public void createTextPane() {
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setContentType("text/html");
        textPane.addHyperlinkListener(this);
        HTMLEditorKit editorKit = (HTMLEditorKit) textPane.getEditorKit();
        StyleSheet styleSheet = editorKit.getStyleSheet();
        styleSheet.removeStyle("body");
        styleSheet.addRule("body { font-family: Arial, Helvetica, sans-serif }");
    }
    
    public void setText(String text) {
        textPane.setText(text);
        textPane.setCaretPosition(0);
        label = new JLabel(text);
    }
    
    public void clear() {
        setText("");
    }
    
    public void hyperlinkUpdate(HyperlinkEvent e) {
        // Welcome to the wonderful world of OOP, featuring nested-if polymorphism.
        if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            Edit.showStatus("Open " + e.getDescription());
        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
            Edit.showStatus("");
        } else if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e instanceof HTMLFrameHyperlinkEvent) {
                ((HTMLDocument) textPane.getDocument()).processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) e);
            } else {
                linkClicked(e.getDescription());
            }
        }
    }
    
    public void linkClicked(String url) {
        // TODO: should this code be in Edit.openFile?
        if (url.startsWith("man:")) {
            String page = url.substring(4);
            try {
                new ShellCommand("man -a " + page + " | col -b");
            } catch (Throwable th) {
                Edit.showAlert("Man Page", "Can't run man(1) (" + th.getMessage() + ").");
            }
            return;
        }
        
        // Hand it on to Edit to work out what to do with it.
        Edit.openFile(url);
    }
}
