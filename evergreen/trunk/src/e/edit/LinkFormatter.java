package e.edit;

import java.awt.*;
import java.io.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.text.*;
import e.util.*;

/**
Formats potential links in text to make them look like links in a web browser. The
class LinkRecognizer in turn spots when the user's pointing at such links.
*/
public class LinkFormatter {
    private JTextPane text;
    
    private boolean autoScroll = true;
    private int maxYDisplayStart = -1;
    
    private StyleContext styles = new StyleContext();
    
    private Style currentStyle;
    
    private Style linkStyle;
    
    private File currentDirectory;
    
    public LinkFormatter(JTextPane text) {
        this.text = text;
    }
    
    public void setAutoScroll(boolean shouldAutoScroll) {
        this.autoScroll = shouldAutoScroll;
        maxYDisplayStart = -1;
    }
    
    public void setCurrentStyle(Style baseStyle) {
        initStyles(baseStyle);
        this.currentStyle = baseStyle;
    }
    
    public void setLinkStyle(Style linkStyle) {
        this.linkStyle = linkStyle;
    }
    
    public void initStyles(Style baseStyle) {
        styles.addStyle("0", baseStyle);
        
        // We don't have a bold style because we ignore requests to embolden text.
        // They cause trouble for coloring, because we don't overlay styles, so anything
        // that asks for 'green' 'bold' will get black bold, because black is the color
        // for the 'bold' style.
        
        Style style = styles.addStyle("4", baseStyle);
        StyleConstants.setUnderline(style, true);
        
        addColorStyle("30", Color.BLACK);
        addColorStyle("31", Color.RED);
        addColorStyle("32", Color.GREEN.darker().darker());
        addColorStyle("33", Color.ORANGE);
        addColorStyle("34", Color.BLUE);
        addColorStyle("35", Color.MAGENTA);
        addColorStyle("36", Color.CYAN);
        addColorStyle("37", Color.LIGHT_GRAY);
    }
    
    public void addColorStyle(String number, Color color) {
        Style defaultStyle = styles.getStyle("0");
        Style style = styles.addStyle(number, defaultStyle);
        StyleConstants.setForeground(style, color);
        StyleConstants.setBold(style, true); // Colors don't show up well otherwise.
    }
    
    /**
     * Matches addresses (such as "filename.ext:line:col:line:col").
     * 
     * We could also insist that there are more than 2 characters,
     * and that an address can't end in a /. But this seems to work
     * pretty nicely.
     */
    private Pattern addressPattern = Pattern.compile("(?:^| |\")([^ :\"]+(?:Makefile|\\w+\\.\\w+)([\\d:]+)?)");
    
    private Pattern colorPattern = Pattern.compile("\\e\\[(\\d+)m");
    
    /** Changes the current style to correspond to what's asked for by the ANSI escape we've matched. */
    private final String processColor(StyledDocument document, String line, Matcher color) throws BadLocationException {
        String styleName = color.group(1);
        Style newStyle = styles.getStyle(styleName);
        if (newStyle != null) {
            currentStyle = newStyle;
        }
        return line.substring(color.end());
    }
    
    /** Inserts an address using the link style. Text leading up to the address gets the current style. */
    private final String processAddress(StyledDocument document, String line, Matcher address, String linkText) throws BadLocationException {
        // Instead of just switching off auto-scrolling, take note that the window shouldn't auto-scroll such
        // that this address disappears off the top of the visible area.
        if (maxYDisplayStart == -1) {
            maxYDisplayStart = text.modelToView(document.getLength()).y;
        }
        
        document.insertString(document.getLength(), line.substring(0, address.start(1)), currentStyle);
        document.insertString(document.getLength(), linkText, linkStyle);
        return line.substring(address.end(1));
    }
    
    private static final Pattern MAKE_DIRECTORY_CHANGE = Pattern.compile("^make(?:\\[\\d+\\])?: (Entering|Leaving) directory `(.*)'$");
    
    public void appendLine(String line) {
        StyledDocument document = text.getStyledDocument();
        try {
            Matcher directoryChangeMatcher = MAKE_DIRECTORY_CHANGE.matcher(line);
            if (directoryChangeMatcher.find()) {
                if (directoryChangeMatcher.group(1).equals("Entering")) {
                    currentDirectory(directoryChangeMatcher.group(2));
                }
            }
            
            while (true) {
                Matcher address = addressPattern.matcher(line);
                File file = null;
                String tail = "";
                boolean foundAddress = address.find();
                if (foundAddress) {
                    /*
                     * We're most useful in providing links to grep matches, so we
                     * need to avoid being confused by stuff like File.java:123.
                     */
                    String name = address.group(1);
                    int colonIndex = name.indexOf(':');
                    if (colonIndex != -1) {
                        tail = name.substring(colonIndex);
                        name = name.substring(0, colonIndex);
                    }
                    
                    /*
                     * If the file doesn't exist, this wasn't a useful match.
                     */
                    if (name.startsWith("/") || name.startsWith("~")) {
                        file = FileUtilities.fileFromString(name);
                    } else {
                        file = new File(currentDirectory(), name);
                    }
                    foundAddress = file.exists();
                }
                
                Matcher color = colorPattern.matcher(line);
                boolean foundColor = color.find();
                
                if (!foundAddress && !foundColor) {
                    break;
                }
                
                int addressStart = foundAddress ? address.start() : Integer.MAX_VALUE;
                int colorStart = foundColor ? color.start() : Integer.MAX_VALUE;
                
                // Only do whichever comes first (because it will invalidate the other's group start and end).
                // An alternative implementation might keep offsets into the line rather than substringing it.
                if (addressStart < colorStart) {
                    String linkText = FileUtilities.getUserFriendlyName(file) + tail;
                    line = processAddress(document, line, address, linkText);
                } else {
                    line = processColor(document, line, color);
                }
            }
            document.insertString(document.getLength(), line, currentStyle);
            autoScroll();
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }
    
    public void autoScroll() {
        if (autoScroll == false) {
            return;
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    int lastDocumentOffset = text.getDocument().getLength();
                    Rectangle viewRect = text.modelToView(lastDocumentOffset);
                    if (viewRect != null) {
                        text.scrollRectToVisible(getViewRectangleIncludingMaxY(viewRect));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
    
    public Rectangle getViewRectangleIncludingMaxY(Rectangle rect) {
        if (maxYDisplayStart == -1) {
            return rect;
        }
        Dimension displaySize = ((JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, text)).getExtentSize();
        Rectangle result = new Rectangle(0, rect.y + rect.height - displaySize.height, displaySize.width, displaySize.height);
        result.y = Math.max(0, result.y);
        result.y = Math.min(maxYDisplayStart, result.y);
        return result;
    }
    
    private void currentDirectory(String directory) {
        currentDirectory = new File(directory);
    }
    
    private File currentDirectory() {
        return currentDirectory;
    }
}
