package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
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
        textPane.setContentType("text/html");
        textPane.setDragEnabled(false);
        textPane.setEditable(false);
        textPane.addHyperlinkListener(this);
        
        initKeyBindings(textPane);
        
        HTMLEditorKit editorKit = (HTMLEditorKit) textPane.getEditorKit();
        StyleSheet styleSheet = editorKit.getStyleSheet();
        
        Font bodyFont = ChangeFontAction.getConfiguredFont();
        styleSheet.removeStyle("body");
        String body = "body { font-family: \"" + bodyFont.getFamily() + "\", sans-serif; font-size: " + bodyFont.getSize() + "pt; margin: 0px 2px 20px 2px; }";
        styleSheet.addRule(body);
        //System.err.println(body);
        
        Font preFont = ChangeFontAction.getConfiguredFixedFont();
        styleSheet.removeStyle("pre");
        String pre = "pre { font-family: \"" + preFont.getFamily() + "\", monospace; font-size: " + preFont.getSize() + "pt; background-color: #eeeeff; border-style: solid; border-width: thin; border-color: #bbbbff; padding: 5px 5px 5px 5px; }";
        styleSheet.addRule(pre);
        //System.err.println(pre);
    }
    
    private static class ScrollAction extends AbstractAction {
        private JComponent c;
        private boolean byBlock;
        private int direction;
        
        public ScrollAction(KeyStroke keyStroke, JComponent c, boolean byBlock, int direction) {
            this.c = c;
            this.byBlock = byBlock;
            this.direction = direction;
            putValue(Action.NAME, (byBlock ? "page" : "line") + (direction < 0 ? "Up" : "Down") + "Action");
            putValue(Action.ACCELERATOR_KEY, keyStroke);
        }
        
        public void actionPerformed(ActionEvent e) {
            // You can't -- as far as I know -- specify a KeyStroke that insists on a modifier key *not* being down. "No modifiers" really means "any".
            // So rather than use a KeyStroke with the shift modifier and a KeyStroke without (which would mean both would fire when shift was down), we just use one KeyStroke and check for shift here.
            boolean shiftDown = ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
            ComponentUtilities.scroll(c, byBlock, (shiftDown ? -direction : direction));
        }
    }
    
    private static void initKeyBindings(JTextPane textPane) {
        // Add C-F, C-D, and C-G.
        JTextComponentUtilities.addFindFunctionalityTo(textPane);
        
        // Add home and end, which are provided by DefaultEditorKit, but don't seem to be set up by default.
        textPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0, false), DefaultEditorKit.beginAction);
        textPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0, false), DefaultEditorKit.endAction);
        
        // Make space and shift-space synonyms for page down and page up, like in real web browsers.
        ComponentUtilities.initKeyBinding(textPane, new ScrollAction(KeyStroke.getKeyStroke(' '), textPane, true, 1));
        
        // Connect the up and down arrow keys to the scroll bar.
        ComponentUtilities.initKeyBinding(textPane, new ScrollAction(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), textPane, false, 1));
        ComponentUtilities.initKeyBinding(textPane, new ScrollAction(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), textPane, false, -1));
    }
    
    public void setText(String text) {
        textPane.setContentType("text/html");
        textPane.setText(text);
        textPane.setCaretPosition(0);
    }
    
    public void setPage(String url) {
        try {
            textPane.setPage(url);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Gets the "best" information from a HyperlinkEvent.
     * If you're dealing with a Sun-supported scheme, getURL is best.
     * If you're not (because you have one of our custom schemes for RubyDoc, say), you have to use getDescription.
     */
    private static String stringFromHyperlinkEvent(HyperlinkEvent e) {
        URL url = e.getURL();
        if (url != null) {
            return url.toString();
        }
        return e.getDescription();
    }
    
    public void hyperlinkUpdate(HyperlinkEvent e) {
        // Welcome to the wonderful world of OOP, featuring nested-if polymorphism.
        if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            statusBar.setText("Open " + stringFromHyperlinkEvent(e));
        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
            statusBar.clearStatusBar();
        } else if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e instanceof HTMLFrameHyperlinkEvent) {
                ((HTMLDocument) textPane.getDocument()).processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) e);
            } else {
                Advisor.getInstance().linkClicked(stringFromHyperlinkEvent(e));
            }
        }
    }
}
