package e.edit;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

public abstract class WordAction extends TextAction {
    /** Whether to extend the selection when changing the caret position.*/
    protected boolean select;
    
    /** Create this action with the appropriate identifier. */
    public WordAction(String actionName, boolean select) {
        super(actionName);
        this.select = select;
    }
    
    /** Returns the offset the subclass wants the caret (or selection) to be moved to. */
    public abstract int getNewOffset(ETextArea text, int offset);
    
    /** The operation to perform when this action is triggered. */
    public void actionPerformed(ActionEvent e) {
        JTextComponent target = getTextComponent(e);
        if (target == null || target instanceof ETextArea == false) {
            return;
        }
        
        ETextArea text = (ETextArea) target;
        int offset = target.getCaretPosition();
        int newOffset = getNewOffset(text, offset);
        if (select) {
            target.moveCaretPosition(newOffset);
        } else {
            target.setCaretPosition(newOffset);
        }
    }
    
    private static final String WHITESPACE = " \t";
    
    /** Returns the start of the word at 'offset'. */
    public static final int getWordStart(ETextArea text, int offset) {
        return scanBackwards(text, offset, text.getWordSelectionStopChars(), false);
    }
    
    /** Returns the end of the word at 'offset'. */
    public static final int getWordEnd(ETextArea text, int offset) {
        return scanForwards(text, offset, text.getWordSelectionStopChars(), false);
    }
    
    /** Returns the start of the whitespace at 'offset'. */
    public static final int getWhitespaceStart(ETextArea text, int offset) {
        return scanBackwards(text, offset, WHITESPACE, true);
    }
    
    /** Returns the start of the non-word at 'offset'. */
    public static final int getNonWordStart(ETextArea text, int offset) {
        return scanBackwards(text, offset, text.getWordSelectionStopChars(), true);
    }
    
    /** Returns the end of the non-word at 'offset'. */
    public static final int getNonWordEnd(ETextArea text, int offset) {
        return scanForwards(text, offset, text.getWordSelectionStopChars(), true);
    }
    
    private static final boolean charIsFoundIn(char c, String s) {
        return s.indexOf(c) != -1;
    }
    
    /** Tests whether the given offset is in a word in 'text'. */
    public static final boolean isInWord(ETextArea text, int offset) {
        return (isIn(text, offset, text.getWordSelectionStopChars()) == false);
    }
    
    /** Tests whether the given offset is in whitespace in 'text'. */
    public static final boolean isInWhitespace(ETextArea text, int offset) {
        return isIn(text, offset, WHITESPACE);
    }
    
    private static final boolean isIn(ETextArea text, int offset, String characters) {
        try {
            return charIsFoundIn(text.getCharAt(offset), characters);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    private static final int scanBackwards(ETextArea text, int offset, String set, boolean shouldBeInSet) {
        try {
            while (offset > 0 && charIsFoundIn(text.getCharAt(offset - 1), set) == shouldBeInSet) {
                offset--;
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
        return offset;
    }
    
    private static final int scanForwards(ETextArea text, int offset, String set, boolean shouldBeInSet) {
        try {
            int lastOffset = text.getDocument().getLength();
            while (offset < lastOffset && charIsFoundIn(text.getCharAt(offset), set) == shouldBeInSet) {
                offset++;
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
        return offset;
    }
}
