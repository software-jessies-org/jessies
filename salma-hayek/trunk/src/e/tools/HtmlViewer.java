package e.tools;

import e.gui.*;
import e.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;

/**
 * A stand-alone program to render an HTML file in a window. This is sometimes
 * a convenient thing to have, less overkill than IE or Mozilla, and less
 * round-about than mailing it to yourself to view in Outlook.
 */
public class HtmlViewer extends MainFrame {
    private JTextPane textPane;
    
    public HtmlViewer(String text) {
        Log.setApplicationName("HtmlViewer");
        initTextPane();
        setText(text);
        setTitle("HTML Viewer");
        getContentPane().add(new JScrollPane(textPane));
        setSize(new Dimension(640, 480));
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
                            SimpleDialog.showDetails(HtmlViewer.this, "Problem opening URL", th);
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
    
    public static void main(String[] arguments) {
        GuiUtilities.initLookAndFeel();
        for (String argument : arguments) {
            String text = StringUtilities.readFile(argument);
            new HtmlViewer(text).setVisible(true);
        }
    }
}
