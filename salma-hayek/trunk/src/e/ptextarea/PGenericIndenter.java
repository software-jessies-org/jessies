package e.ptextarea;

import java.util.regex.*;

/**
 * Indents text using just supplied regular expressions.
 * This is based on the documentation for the "varindent" variable-based indenter for the Kate editor.
 * See http://kate-editor.org/article/the_variable_based_indenter
 */
public class PGenericIndenter extends PSimpleIndenter {
    private Pattern indentAfterPattern;
    private Pattern indentPattern;
    private Pattern unindentPattern;
    
    private String electricCharacters;
    
    /**
     * Subclasses should call this constructor, and shouldn't need to override any methods.
     * 
     * @param indentAfterRegEx - if the first line with content above the current line matches, indentation is added.
     * @param indentRegEx - if the current line matches, indentation is added. This is only used if indentAfter matched.
     * @param unindentRegEx - if the current line matches, indentation is removed.
     * @param electricCharacters - a line's indentation is corrected whenever any of these characters is typed.
     */
    public PGenericIndenter(PTextArea textArea, String indentAfterRegEx, String indentRegEx, String unindentRegEx, String electricCharacters) {
        super(textArea);
        this.indentAfterPattern = Pattern.compile(indentAfterRegEx);
        if (indentRegEx != null) {
            this.indentPattern = Pattern.compile(indentRegEx);
        }
        this.unindentPattern = Pattern.compile(unindentRegEx);
        this.electricCharacters = electricCharacters;
    }
    
    public boolean isElectric(char c) {
        return electricCharacters.indexOf(c) != -1;
    }
    
    public String calculateNewIndentation(int lineNumber) {
        int previousNonBlank = getPreviousNonBlankLineNumber(lineNumber);
        if (previousNonBlank == -1) {
            return "";
        }
        
        String indentation = getCurrentIndentationOfLine(previousNonBlank);
        String thisLineText = getActivePartOfLine(lineNumber);
        
        if (indentAfterPattern.matcher(getActivePartOfLine(previousNonBlank)).find()) {
            indentation = increaseIndentation(indentation);
        } else if (indentPattern != null && indentPattern.matcher(thisLineText).find()) {
            indentation = increaseIndentation(indentation);
        }
        
        if (unindentPattern.matcher(thisLineText).find()) {
            indentation = decreaseIndentation(indentation);
        }
        
        return indentation;
    }
}
