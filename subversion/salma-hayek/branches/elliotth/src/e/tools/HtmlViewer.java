package e.tools;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import e.gui.*;
import e.util.*;

/**
 * A stand-alone program to render an HTML file in a window. This is sometimes
 * a convenient thing to have, less overkill than IE or Mozilla, and less
 * round-about than mailing it to yourself to view in Outlook.
 */
public class HtmlViewer extends JFrame {
    private JTextPane textPane;
    
    public HtmlViewer(String text) {
        Log.setApplicationName("HtmlViewer");
        initTextPane();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setText(text);
        setTitle("HTML Viewer");
        getContentPane().add(new JScrollPane(textPane));
        setSize(new Dimension(640, 480));
        setLocationRelativeTo(null);
    }
    
    public void initTextPane() {
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setContentType("text/html");
        
        textPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (e instanceof HTMLFrameHyperlinkEvent) {
                        ((HTMLDocument) textPane.getDocument()).processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) e);
                    } else {
                        Log.warn("can't yet follow links out of the document");
                        System.exit(0);
                        //Edit.showDocument(e.getURL().toString());
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
    
    public static void main(String[] args) {
        GuiUtilities.initLookAndFeel();
        for (int i = 0; i < args.length; i++) {
            String text = StringUtilities.readFile(args[i]);
            HtmlViewer htmlViewer = new HtmlViewer(text);
            htmlViewer.setVisible(true);
        }
    }
}
