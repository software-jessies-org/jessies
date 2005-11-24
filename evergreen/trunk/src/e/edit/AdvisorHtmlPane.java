package e.edit;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;

public class AdvisorHtmlPane extends JComponent implements HyperlinkListener {
    private JTextPane textPane;
    
    public AdvisorHtmlPane() {
        initTextPane();
        setLayout(new BorderLayout());
        add(new JScrollPane(textPane), BorderLayout.CENTER);
    }
    
    private void initTextPane() {
        this.textPane = new JTextPane();
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
    }
    
    public void hyperlinkUpdate(HyperlinkEvent e) {
        // Welcome to the wonderful world of OOP, featuring nested-if polymorphism.
        if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            String tip = "Open " + e.getDescription();
            Edit.getInstance().showStatus(tip);
            textPane.setToolTipText(tip);
        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
            Edit.getInstance().showStatus("");
            textPane.setToolTipText(null);
        } else if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e instanceof HTMLFrameHyperlinkEvent) {
                ((HTMLDocument) textPane.getDocument()).processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) e);
            } else {
                Edit.getInstance().getAdvisor().linkClicked(e.getDescription());
            }
        }
    }
}
