package e.edit;

import javax.swing.text.*;

public class NextWordAction extends WordAction {
    /** Create this action with the appropriate identifier. */
    public NextWordAction(boolean shouldSelect) {
        super(shouldSelect ? DefaultEditorKit.selectionNextWordAction : DefaultEditorKit.nextWordAction, shouldSelect);
    }
    
    public int getNewOffset(CharSequence text, int offset, String stopChars) {
        // If we're at the end of the document, we're not going far.
        if (offset == text.length()) {
            return offset;
        }
        
        // If we're in a word, go to the end of this word.
        if (isInWord(text, offset, stopChars)) {
            return getWordEnd(text, offset, stopChars);
        }
        
        // Otherwise go to the start of the next word.
        return getWordEnd(text, getNonWordEnd(text, getWordEnd(text, offset, stopChars), stopChars), stopChars);
    }
}
