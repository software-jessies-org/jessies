package e.edit;

import javax.swing.text.*;

public class PreviousWordAction extends WordAction {
    /** Create this action with the appropriate identifier. */
    public PreviousWordAction(boolean shouldSelect) {
        super(shouldSelect ? DefaultEditorKit.selectionPreviousWordAction : DefaultEditorKit.previousWordAction, shouldSelect);
    }
    
    public int getNewOffset(ETextArea text, int offset) {
        // If we're at the start of the document, we're not going far.
        if (offset == 0) {
            return 0;
        }
        
        // If we're selecting and at the start of whitespace, go the start of the whitespace.
        // FIXME: this heuristic isn't right. What should the behavior be? For now, behave like Project Builder.
        //if (select && isInWhitespace(text, offset - 1)) {
            //return getWhitespaceStart(text, offset);
        //}
        
        // If we're at the start of a word, go to the start of the word before.
        if (isInWord(text, offset - 1) == false) {
            return getWordStart(text, getNonWordStart(text, offset - 1));
        }
        
        // Otherwise go to the start of the current word.
        return getWordStart(text, offset);
    }
}
