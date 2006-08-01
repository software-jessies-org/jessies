package e.edit;

import e.gui.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;

public class AdvisorHtmlPane extends JComponent implements HyperlinkListener {
    private JTextPane textPane;
    private EStatusBar statusBar;
    
    public AdvisorHtmlPane() {
        statusBar = new EStatusBar();
        initTextPane();
        setLayout(new BorderLayout());
        add(new JScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }
    
    private void initTextPane() {
        this.textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setContentType("text/html");
        textPane.addHyperlinkListener(this);
        
        HTMLEditorKit editorKit = (HTMLEditorKit) textPane.getEditorKit();
        StyleSheet styleSheet = editorKit.getStyleSheet();
        
        Font bodyFont = ChangeFontAction.getConfiguredFont();
        styleSheet.removeStyle("body");
        String body = "body { font-family: \"" + bodyFont.getFamily() + "\", sans-serif; font-size: " + bodyFont.getSize() + "pt; margin: 0px 2px 0px 2px; }";
        styleSheet.addRule(body);
        //System.err.println(body);
        
        Font preFont = ChangeFontAction.getConfiguredFixedFont();
        styleSheet.removeStyle("pre");
        String pre = "pre { font-family: \"" + preFont.getFamily() + "\", monospace; font-size: " + preFont.getSize() + "pt; background-color: #eeeeff; border-style: solid; border-width: thin; border-color: #bbbbff; padding: 5px 5px 5px 5px; }";
        styleSheet.addRule(pre);
        //System.err.println(pre);
    }
    
    public void setText(String text) {
        textPane.setText(text);
        textPane.setCaretPosition(0);
    }
    
    public void hyperlinkUpdate(HyperlinkEvent e) {
        // Welcome to the wonderful world of OOP, featuring nested-if polymorphism.
        if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            statusBar.setText("Open " + e.getDescription());
        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
            statusBar.clearStatusBar();
        } else if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e instanceof HTMLFrameHyperlinkEvent) {
                ((HTMLDocument) textPane.getDocument()).processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) e);
            } else {
                Advisor.getInstance().linkClicked(e.getDescription());
            }
        }
    }
}
