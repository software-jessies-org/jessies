package e.edit;

public class JavaIndenter extends Indenter {
    /**
     * Returns that part of the given line number that isn't leading/trailing whitespace or comment.
     */
    public String getActivePartOfLine(ETextArea text, int lineNumber) throws javax.swing.text.BadLocationException {
        String trimmedLine = text.getLineText(lineNumber).trim();
        return trimmedLine.replaceFirst("//.*", "");
    }
    
    public boolean isElectric(char c) {
        return (c == '}' || c == ':' || c == '#');
    }
    
    public boolean isBlockBegin(String activePartOfLine) {
        return activePartOfLine.endsWith("{");
    }
    public boolean isBlockEnd(String activePartOfLine) {
        return activePartOfLine.startsWith("}");
    }
    public boolean isLabel(String activePartOfLine) {
        return activePartOfLine.matches("(private|public|protected|case|default)\\b.*:");
    }
    
    /**
     * Lines that start with a closing brace or end with a opening brace or a colon tell us
     * definitively what the indentation for the next line should be.
     * Going back this far keeps us tidy in the face of various multi-line comment styles,
     * multi-line C++ output operator expressions and C++ preprocessor commands.
     */
    public boolean isDefinitive(String rawLine) {
        String trimmedLine = rawLine.trim();
        return isBlockBegin(trimmedLine) || isBlockEnd(trimmedLine) || isLabel(trimmedLine);
    }
    
    public int getPreviousDefinitiveLineNumber(ETextArea text, int startLineNumber) {
        try {
            for (int lineNumber = startLineNumber - 1; lineNumber > 0; lineNumber--) {
                if (isDefinitive(text.getLineText(lineNumber))) {
                    return lineNumber;
                }
            }
        } catch (javax.swing.text.BadLocationException ex) {
            ex.printStackTrace();
        }
        return 0;
    }
    
    public String getIndentation(ETextArea text, int lineNumber) throws javax.swing.text.BadLocationException {
        int previousDefinitive = getPreviousDefinitiveLineNumber(text, lineNumber);
        if (previousDefinitive == 0) {
            return "";
        }
        
        String indentation = text.getIndentationOfLine(previousDefinitive);
        
        String activePartOfPrevious = getActivePartOfLine(text, previousDefinitive);
        if (isBlockBegin(activePartOfPrevious) || isLabel(activePartOfPrevious)) {
            indentation = increaseIndentation(text, indentation);
        }
        
        String activePartOfLine = getActivePartOfLine(text, lineNumber);
        if (isBlockEnd(activePartOfLine) || isLabel(activePartOfLine)) {
            indentation = decreaseIndentation(text, indentation);
        }
        if (activePartOfLine.startsWith("#")) {
            indentation = "";
        }
        
        // Recognize doc comments, and help out with the ASCII art.
        if (lineNumber > 0) {
            String previousLine = text.getLineText(lineNumber - 1).trim();
            if (previousLine.endsWith("*/")) {
                // Whatever the previous line looks like, if it ends with
                // a close of comment, we're not in a comment, and should
                // do nothing.
            } else if (previousLine.startsWith("/**") || previousLine.startsWith("* ")) {
                indentation += " *";
            }
        }
        
        return indentation;
    }
}
