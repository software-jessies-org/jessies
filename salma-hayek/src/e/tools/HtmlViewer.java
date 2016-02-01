package e.tools;

import e.gui.*;
import e.util.*;
import java.awt.*;
import javax.swing.*;

/**
 * A stand-alone program to render an HTML file in a window.
 * This is sometimes a convenient thing to have, less overkill than IE or Mozilla, and less round-about than mailing it to yourself to view in Outlook.
 */
public class HtmlViewer extends MainFrame {
    private HtmlPane htmlPane;
    
    public HtmlViewer(String text) {
        Log.setApplicationName("HtmlViewer");
        htmlPane = new HtmlPane();
        htmlPane.setText(text);
        setTitle("HTML Viewer");
        getContentPane().add(new JScrollPane(htmlPane));
        setSize(new Dimension(640, 480));
    }
    
    public static void main(String[] arguments) {
        GuiUtilities.initLookAndFeel();
        for (String argument : arguments) {
            String text = StringUtilities.readFile(argument);
            new HtmlViewer(text).setVisible(true);
        }
    }
}
