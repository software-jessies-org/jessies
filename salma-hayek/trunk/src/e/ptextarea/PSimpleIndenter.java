package e.ptextarea;

public abstract class PSimpleIndenter extends PIndenter {
    public PSimpleIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    public String stripCppComments(String line) {
        // We should strip C comments but this is rarely important.
        // C comments usually include the whole line,
        // we're only stripping comments to find the "active" part of the line
        // and a line which is entirely comment has no active part.
        return line.replaceFirst("//.*", "");
    }
    
    /**
     * Returns that part of the given line number that isn't leading/trailing whitespace or comment.
     */
    public String getActivePartOfLine(int lineNumber) {
        String line = textArea.getLineText(lineNumber);
        return stripComments(line).trim();
    }
    
    public boolean isElectric(char c) {
        if (c == '#' && shouldMoveHashToColumnZero()) {
            return true;
        }
        if (c == ':' && shouldMoveLabels()) {
            return true;
        }
        if (c == '}') {
            return true;
        }
        return false;
    }
    
    public boolean isBlockBegin(String activePartOfLine) {
        return activePartOfLine.endsWith("{");
    }
    public boolean isBlockEnd(String activePartOfLine) {
        return activePartOfLine.startsWith("}");
    }
    public boolean isSwitchLabel(String activePartOfLine) {
        return activePartOfLine.matches("(case\\b.*|default\\s*):");
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
    
    public int getPreviousDefinitiveLineNumber(int startLineNumber) {
        for (int lineNumber = startLineNumber - 1; lineNumber >= 0; --lineNumber) {
            String line = textArea.getLineText(lineNumber);
            if (isDefinitive(line)) {
                return lineNumber;
            }
        }
        return -1;
    }
    
    public String getIndentation(int lineNumber) {
        String activePartOfLine = getActivePartOfLine(lineNumber);
        if (shouldMoveHashToColumnZero() && activePartOfLine.startsWith("#")) {
            return "";
        }

        String indentation = "";
        int previousDefinitive = getPreviousDefinitiveLineNumber(lineNumber);
        if (previousDefinitive != -1) {
            indentation = getCurrentIndentationOfLine(previousDefinitive);
            
            String activePartOfPrevious = getActivePartOfLine(previousDefinitive);
            if (isBlockBegin(activePartOfPrevious) || isLabel(activePartOfPrevious)) {
                indentation = increaseIndentation(indentation);
            }
        }
        
        if (isBlockEnd(activePartOfLine) || isLabel(activePartOfLine)) {
            indentation = decreaseIndentation(indentation);
        }
        
        // Recognize doc comments, and help out with the ASCII art.
        if (lineNumber > 0 && shouldContinueDocComments()) {
            String previousLine = textArea.getLineText(lineNumber - 1).trim();
            if (previousLine.endsWith("*/")) {
                // Whatever the previous line looks like, if it ends with
                // a close of comment, we're not in a comment, and should
                // do nothing.
            } else if (previousLine.matches("/\\*{1,2}") || previousLine.startsWith("* ")) {
                // We're in a doc comment.
                if (activePartOfLine.startsWith("* ") || activePartOfLine.startsWith("*/")) {
                    // We already have the JavaDoc ASCII art, and just need to
                    // indent it one space.
                    indentation += " ";
                } else {
                    indentation += " * ";
                }
            }
        }
        
        return indentation;
    }

    protected abstract String stripComments(String line);
    protected abstract boolean isLabel(String activePartOfLine);
    protected abstract boolean shouldMoveHashToColumnZero();
    protected abstract boolean shouldMoveLabels();
    protected abstract boolean shouldContinueDocComments();
}
