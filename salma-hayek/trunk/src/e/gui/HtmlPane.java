package e.gui;

import e.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;

public class HtmlPane extends JPanel implements Scrollable {
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
                    linkActivated(e);
                }
            }
        });
        
        HTMLEditorKit editorKit = (HTMLEditorKit) textPane.getEditorKit();
        StyleSheet styleSheet = editorKit.getStyleSheet();
        styleSheet.removeStyle("body");
        styleSheet.addRule("body { font-family: Arial, Helvetica, sans-serif }");
        styleSheet.addRule("body { font-size: 10 }");
    }
    
    private void linkActivated(HyperlinkEvent e) {
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
    
    public void setText(String text) {
        textPane.setText(text);
        textPane.setCaretPosition(0);
    }
    
    // Delegate the Scrollable interface to textPane...
    public Dimension getPreferredScrollableViewportSize() { return textPane.getPreferredScrollableViewportSize(); }
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return textPane.getScrollableUnitIncrement(visibleRect, orientation, direction); }
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return textPane.getScrollableBlockIncrement(visibleRect, orientation, direction); }
    public boolean getScrollableTracksViewportHeight() { return textPane.getScrollableTracksViewportHeight(); }
    // ...apart from this method.
    // We return true here because we really don't want a horizontal scroll bar: we want to wrap the content to fit the container.
    // FIXME: we might need to be clever here and detect cases where the content is simply too wide not to have a horizontal scroll bar.
    public boolean getScrollableTracksViewportWidth() { return true; }
}
