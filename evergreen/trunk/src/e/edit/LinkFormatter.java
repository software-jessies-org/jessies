package e.edit;

import java.awt.*;
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
    private Pattern addressPattern = Pattern.compile("(?:^| |\")(/[^ :\"]+\\.\\w+([\\d:]+)?)");
    
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
    private final String processAddress(StyledDocument document, String line, Matcher address) throws BadLocationException {
//        autoScroll = false; // Don't auto-scroll past a link.
        // Instead of just switching off auto-scrolling, take note that the window shouldn't auto-scroll such
        // that this address disappears off the top of the visible area.
        if (maxYDisplayStart == -1) {
            maxYDisplayStart = text.modelToView(document.getLength()).y;
        }

        document.insertString(document.getLength(), line.substring(0, address.start(1)), currentStyle);
        document.insertString(document.getLength(), FileUtilities.getUserFriendlyName(address.group(1)), linkStyle);
        return line.substring(address.end(1));
    }
    
    public void appendLine(String line) {
        StyledDocument document = text.getStyledDocument();
        try {
            while (true) {
                Matcher address = addressPattern.matcher(line);
                boolean foundAddress = address.find();
                
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
                    line = processAddress(document, line, address);
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
}
