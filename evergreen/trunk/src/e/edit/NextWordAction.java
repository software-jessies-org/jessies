package e.edit;

import javax.swing.text.*;

public class NextWordAction extends WordAction {
    /** Create this action with the appropriate identifier. */
    public NextWordAction(boolean shouldSelect) {
        super(shouldSelect ? DefaultEditorKit.selectionNextWordAction : DefaultEditorKit.nextWordAction, shouldSelect);
    }
    
    public int getNewOffset(ETextArea text, int offset) {
        // If we're at the end of the document, we're not going far.
        if (offset == text.getDocument().getLength()) {
            return 0;
        }
        
        // If we're in a word, go to the end of this word.
        if (isInWord(text, offset)) {
            return getWordEnd(text, offset);
        }
        
        // Otherwise go to the start of the next word.
        return getWordEnd(text, getNonWordEnd(text, getWordEnd(text, offset)));
    }
}
