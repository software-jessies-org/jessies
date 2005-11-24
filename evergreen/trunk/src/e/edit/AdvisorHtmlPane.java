package e.edit;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;

public class AdvisorHtmlPane extends JComponent implements HyperlinkListener {
    private JTextPane textPane;
    private JLabel label;
    
    public AdvisorHtmlPane() {
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
        styleSheet.addRule("body { font-family: \"Lucida Grande\", Arial, Helvetica, sans-serif }");
    }
    
    public void setText(String text) {
        textPane.setText(text);
        textPane.setCaretPosition(0);
        label = new JLabel(text);
    }
    
    public void hyperlinkUpdate(HyperlinkEvent e) {
        // Welcome to the wonderful world of OOP, featuring nested-if polymorphism.
        if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            Edit.getInstance().showStatus("Open " + e.getDescription());
        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
            Edit.getInstance().showStatus("");
        } else if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e instanceof HTMLFrameHyperlinkEvent) {
                ((HTMLDocument) textPane.getDocument()).processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) e);
            } else {
                Edit.getInstance().getAdvisor().linkClicked(e.getDescription());
            }
        }
    }
}
