package e.edit;

import java.awt.event.*;
import javax.swing.*;
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

        // If we're not in a word, go to the start of the next word.
        if (isInWhitespace(text, offset) || isInWord(text, offset) == false) {
            return getWordStart(text, getNonWordEnd(text, offset));
        }
        
        // Otherwise go to the start of the next word.
        return getWordStart(text, getNonWordEnd(text, getWordEnd(text, offset)));
    }
}
