package e.edit;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

/**
A caret implementation that strives to stay visible, and selects more
sensibly.

@author Elliott Hughes <enh@acm.org>
*/
public class ECaret extends DefaultCaret {
    /** Constructs a new caret. */
    public ECaret() {
        setBlinkRate(0);
    }
    
    public void install(final JTextComponent component) {
        super.install(component);
        component.setCaretColor(Color.RED);
        setSelectionVisible(true);
    }

    public JTextComponent getTextComponent() {
        return super.getComponent();
    }
    
    /** Ensures the caret remains visible by ignoring requests to hide the caret. */
    public void setSelectionVisible(boolean ignored) {
        super.setSelectionVisible(true);
    }
    
    /** Selects the line at the cursor. */
    public void selectLineAtCursor(JTextComponent text) {
        Element line = javax.swing.text.Utilities.getParagraphElement(text, text.getSelectionStart());
        int lineStart = line.getStartOffset();
        int lineEnd = Math.min(line.getEndOffset(), text.getDocument().getLength());
        setDot(lineStart);
        moveDot(lineEnd);
    }
    
    public static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t';
    }
    
    private static final String BRACKETS = "(<[{}]>)";
    
    private static final String PARTNERS = ")>]}{[<(";
    
    public boolean isBracket(char ch) {
        return BRACKETS.indexOf(ch) != -1;
    }
    
    public boolean isOpenBracket(char ch) {
        return BRACKETS.indexOf(ch) < PARTNERS.indexOf(ch);
    }
    
    public boolean isCloseBracket(char ch) {
        return isBracket(ch) && isOpenBracket(ch) == false;
    }
    
    public char getPartnerForBracket(char bracket) {
        return PARTNERS.charAt(BRACKETS.indexOf(bracket));
    }
    
    public char getCharAt(int offset) throws BadLocationException {
        return getComponent().getDocument().getText(offset, 1).charAt(0);
    }
    
    /* Some handy examples to try the bracket matching on.
    
        // if (hello > (int) )
        // if (hello > (int))
        // if ((int) x > 2)
        // if ( (int) x > 2)
    */
    
    /** Returns the offset of the matching bracket, or -1. */
    public int findMatchingBracket(JTextComponent text, int offset) throws BadLocationException {
        //Log.warn("findMatchingBracket(offset="+offset+")");
        char bracket = getCharAt(offset);
        if (isCloseBracket(bracket)) {
            return findMatchingBracket(text, offset, false);
        }
        if (offset > 0 && isOpenBracket(getCharAt(offset - 1))) {
            return findMatchingBracket(text, offset - 1, true);
        }
        return -1;
    }
    
    /** Returns the offset of the matching bracket, scanning in the given direction, or -1. */
    public int findMatchingBracket(JTextComponent text, int offset, boolean scanForwards) throws BadLocationException {
        //Log.warn("findMatchingBracket(offset="+offset+",scanForwards="+scanForwards+")");
        char bracket = getCharAt(offset);
        if (isBracket(bracket) == false) {
            return -1;
        }
        char partner = getPartnerForBracket(bracket);
        int nesting = 1;
        int step = scanForwards ? +1 : -1;
        int stop = scanForwards ? text.getDocument().getLength() : 0;
        for (offset += step; nesting != 0 && offset != stop; offset += step) {
            char ch = getCharAt(offset);
            if (ch == bracket) {
                nesting++;
            } else if (ch == partner) {
                nesting--;
            }
        }
        if (offset != stop) {
            // We actually want to stop just before the matching bracket.
            offset += (scanForwards ? 1 : 2) * -step;
        }
        return offset;
    }
    
    /** Selects the word scanning out from the dot. */
    public void selectWordAtCursor(JTextComponent text) throws BadLocationException {
        int offset = getDot();
        Element line = javax.swing.text.Utilities.getParagraphElement(text, offset);
        int lineStart = line.getStartOffset();
        int lineEnd = Math.min(line.getEndOffset(), text.getDocument().getLength());
        
        //FIXME: first try to match with a bracket.
        int bracketOffset = findMatchingBracket(text, offset);
        if (bracketOffset != -1) {
            moveDot(bracketOffset);
            return;
        }
        
        /* If we're just before the newline ending a paragraph element, select the whole line. */
        if (offset == lineEnd - 1) {
            setDot(lineStart);
            moveDot(lineEnd);
            return;
        }
        
        String s = text.getDocument().getText(lineStart, lineEnd - lineStart);
        if (s.length() == 0) {
            // What could we select in the empty document? Nothing.
            return;
        }
        
        int stringOffset = offset - lineStart;
        
        if (isWhitespace(s.charAt(stringOffset))) {
            // Select a contiguous block of whitespace.
            while (stringOffset > 0 && isWhitespace(s.charAt(stringOffset - 1))) {
                stringOffset--;
            }
            setDot(lineStart + stringOffset);
            while (stringOffset < s.length() && isWhitespace(s.charAt(stringOffset))) {
                stringOffset++;
            }
            moveDot(lineStart + stringOffset);
            return;
        }
        
        // Select from the start to the end of the current word...
        setDot(WordAction.getWordStart((ETextArea) text, offset));
        moveDot(WordAction.getWordEnd((ETextArea) text, offset));
    }
    
    /** Invokes appropriate selection methods for multiple presses. */
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger() == false && e.getClickCount() == 1) {
            super.mousePressed(e);
            return;
        }
        if (SwingUtilities.isLeftMouseButton(e) == false) {
            return;
        }
        if (e.getClickCount() == 2) {
            try {
                selectWordAtCursor(getComponent());
            } catch (BadLocationException ex) { ex.printStackTrace(); }
        } else if (e.getClickCount() == 3) {
            selectLineAtCursor(getComponent());
        }
    }
    
    /**
    * Ignores mouse presses that correspond to the popup trigger so the selection isn't
    * lost. Also forces a redraw as a workaround for a Sun highlighter bug.
    */
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger() == false) {
            super.mouseReleased(e);
            ((Component) e.getSource()).repaint();
        }
    }
    
    /**
     * Handles X11 middle-button pastes from XA_SELECTION (known
     * to Java as the 'system selection'.
     */
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isMiddleMouseButton(e) && e.getClickCount() == 1) {
            JTextComponent textComponent = getTextComponent();
            if (textComponent != null) {
                pasteSystemSelectionInto(textComponent);
            }
        }
    }
    
    public void pasteSystemSelectionInto(JTextComponent textComponent) {
        Clipboard selection = textComponent.getToolkit().getSystemSelection();
        if (selection == null) {
            return;
        }
        TransferHandler transferHandler = textComponent.getTransferHandler();
        if (transferHandler == null) {
            return;
        }
        Transferable transferable = selection.getContents(null);
        if (transferable == null) {
            return;
        }
        transferHandler.importData(textComponent, transferable);
        textComponent.requestFocusInWindow();
    }
}
