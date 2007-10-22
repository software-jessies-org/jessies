package e.gui;

import e.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;

public class HtmlPane extends JPanel {
    private JTextPane textPane;
    
    public HtmlPane() {
        super(new BorderLayout());
        initTextPane();
        add(textPane);
    }
    
    public void initTextPane() {
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setContentType("text/html");
        
        textPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                    textPane.setToolTipText(e.getURL().toString());
                } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
                    textPane.setToolTipText(null);
                } else if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (e instanceof HTMLFrameHyperlinkEvent) {
                        ((HTMLDocument) textPane.getDocument()).processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) e);
                    } else {
                        String url = e.getURL().toString();
                        try {
                            BrowserLauncher.openURL(url);
                        } catch (Throwable th) {
                            SimpleDialog.showDetails(null, "Problem opening URL", th);
                        }
                    }
                }
            }
        });
        
        HTMLEditorKit editorKit = (HTMLEditorKit) textPane.getEditorKit();
        StyleSheet styleSheet = editorKit.getStyleSheet();
        styleSheet.removeStyle("body");
        styleSheet.addRule("body { font-family: Arial, Helvetica, sans-serif }");
        styleSheet.addRule("body { font-size: 10 }");
    }
    
    public void setText(String text) {
        textPane.setText(text);
        textPane.setCaretPosition(0);
    }
}
