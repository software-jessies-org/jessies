package e.edit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

/**
Handles links in JTextPanes. If the mouse is over underlined blue text, switches the mouse
cursor to the appropriate 'hand' cursor. If the mouse is clicked, activates the link.
*/
public class LinkRecognizer extends MouseAdapter implements MouseMotionListener {
    private JTextPane text;
    private LinkListener listener;
    
    public LinkRecognizer(JTextPane text, LinkListener listener) {
        this.text = text;
        this.listener = listener;
        initListeners();
    }
    
    public void initListeners() {
        text.addMouseListener(this);
        text.addMouseMotionListener(this);
    }
    
    private static final Cursor LINK_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    
    private static final Cursor DEFAULT_CURSOR = Cursor.getDefaultCursor();
    
    public void setLinkCursor(boolean isLink) {
        Cursor newCursor = (isLink ? LINK_CURSOR : DEFAULT_CURSOR);
        if (text.getCursor() != newCursor) {
            text.setCursor(newCursor);
        }
    }
    
    public boolean isPartOfLink(StyledDocument document, int offset) {
        AttributeSet attributes = document.getCharacterElement(offset).getAttributes();
        return StyleConstants.isUnderline(attributes) && attributes.containsAttribute(StyleConstants.Foreground, Color.BLUE);
    }
    
    public int findEdgeOfLink(StyledDocument document, int offset, int extremity, int step) {
        int edge = offset;
        while (edge != extremity) {
            if (isPartOfLink(document, edge + step) == false) {
                break;
            }
            edge += step;
        }
        return edge;
    }
    
    /** Returns the link pointed at, or null. */
    public String getLink(MouseEvent e) {
        int offset = text.viewToModel(e.getPoint());
        StyledDocument document = text.getStyledDocument();
        if (isPartOfLink(document, offset) == false) {
            return null;
        }
        try {
            Element lineElement = javax.swing.text.Utilities.getParagraphElement(text, offset);
            int start = findEdgeOfLink(document, offset, lineElement.getStartOffset(), -1);
            int end = findEdgeOfLink(document, offset, lineElement.getEndOffset(), 1) + 1;
            return document.getText(start, end - start);
        } catch (BadLocationException ex) {
            return null;
        }
    }
    
    public void mouseClicked(MouseEvent e) {
        String link = getLink(e);
        if (link == null) {
            return;
        }
        listener.linkActivated(link);
    }
    
    /** Ensures the cursor changes when we're over a link. */
    public void mouseMoved(MouseEvent e) {
        setLinkCursor(getLink(e) != null);
    }
    
    /** Ignores mouse drag notifications. */
    public void mouseDragged(MouseEvent e) {
    }
}
