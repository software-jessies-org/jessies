package e.edit;

import java.awt.event.*;
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
    public abstract int getNewOffset(CharSequence text, int offset, String stopChars);
    
    /** The operation to perform when this action is triggered. */
    public void actionPerformed(ActionEvent e) {
        JTextComponent target = getTextComponent(e);
        if (target == null || target instanceof ETextArea == false) {
            return;
        }
        
        CharSequence text = ((ETextArea) target).charSequence();
        String stopChars = ((ETextArea) target).getWordSelectionStopChars();
        int offset = target.getCaretPosition();
        int newOffset = getNewOffset(text, offset, stopChars);
        if (select) {
            target.moveCaretPosition(newOffset);
        } else {
            target.setCaretPosition(newOffset);
        }
    }
    
    private static final String WHITESPACE = " \t";
    
    /** Returns the start of the word at 'offset'. */
    public static final int getWordStart(CharSequence text, int offset, String stopChars) {
        return scanBackwards(text, offset, stopChars, false);
    }
    
    /** Returns the end of the word at 'offset'. */
    public static final int getWordEnd(CharSequence text, int offset, String stopChars) {
        return scanForwards(text, offset, stopChars, false);
    }
    
    /** Returns the start of the whitespace at 'offset'. */
    public static final int getWhitespaceStart(CharSequence text, int offset) {
        return scanBackwards(text, offset, WHITESPACE, true);
    }
    
    /** Returns the start of the non-word at 'offset'. */
    public static final int getNonWordStart(CharSequence text, int offset, String stopChars) {
        return scanBackwards(text, offset, stopChars, true);
    }
    
    /** Returns the end of the non-word at 'offset'. */
    public static final int getNonWordEnd(CharSequence text, int offset, String stopChars) {
        return scanForwards(text, offset, stopChars, true);
    }
    
    private static final boolean charIsFoundIn(char c, String s) {
        return s.indexOf(c) != -1;
    }
    
    /** Tests whether the given offset is in a word in 'text'. */
    public static final boolean isInWord(CharSequence text, int offset, String stopChars) {
        return (isIn(text, offset, stopChars) == false);
    }
    
    /** Tests whether the given offset is in whitespace in 'text'. */
    public static final boolean isInWhitespace(CharSequence text, int offset) {
        return isIn(text, offset, WHITESPACE);
    }
    
    private static final boolean isIn(CharSequence text, int offset, String characters) {
        return charIsFoundIn(text.charAt(offset), characters);
    }
    
    private static final int scanBackwards(CharSequence text, int offset, String set, boolean shouldBeInSet) {
        while (offset > 0 && charIsFoundIn(text.charAt(offset - 1), set) == shouldBeInSet) {
            offset--;
        }
        return offset;
    }
    
    private static final int scanForwards(CharSequence text, int offset, String set, boolean shouldBeInSet) {
        int lastOffset = text.length();
        while (offset < lastOffset && charIsFoundIn(text.charAt(offset), set) == shouldBeInSet) {
            offset++;
        }
        return offset;
    }
}
