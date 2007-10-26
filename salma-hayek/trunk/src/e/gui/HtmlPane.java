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
        textPane.setContentType("text/html");
        
        // Create a new document.
        // This is (a) paranoia against unwanted heap retention and (b) a defense against persistence of styles.
        textPane.setDocument(textPane.getEditorKit().createDefaultDocument());
        
        HTMLEditorKit editorKit = (HTMLEditorKit) textPane.getEditorKit();
        
        // Fix up the default style sheet to look like a normal text component.
        // FIXME: should we do something with PRE text too?
        StyleSheet styleSheet = editorKit.getStyleSheet();
        Font bodyFont = UIManager.getFont("TextArea.font");
        styleSheet.removeStyle("body");
        styleSheet.addRule("body { font-family: \"" + bodyFont.getFamily() + "\", sans-serif; font-size: " + bodyFont.getSize() + "pt");
        
        // Work around "not a bug" bug 4233012.
        textPane.getDocument().putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        
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
