package e.edit;

import e.util.*;
import javax.swing.text.*;

public class Indenter {
    public String getIndentString() {
        return Parameters.getParameter("indent.string", "\t");
    }
    
    public String increaseIndentation(String original) {
        return original + getIndentString();
    }
    
    public String decreaseIndentation(String original) {
        String delta = getIndentString();
        if (original.endsWith(delta)) {
            return original.substring(0, original.length() - delta.length());
        }
        return original;
    }
    
    /**
     * Returns true if c is an 'electric' character, which is emacs terminology
     * for a character that causes the indentation to be modified when you
     * type it. Typically, this signifies the end of a block.
     */
    public boolean isElectric(char c) {
        return (c == '}');
    }
    
    /** Returns the whitespace that should be used for the given line number. */
    public String getIndentation(ETextArea text, int lineNumber) throws BadLocationException {
        return text.getIndentationOfLine(text.getPreviousNonBlankLineNumber(lineNumber));
    }
}
