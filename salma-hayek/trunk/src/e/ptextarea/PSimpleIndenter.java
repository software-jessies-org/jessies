package e.ptextarea;

public abstract class PSimpleIndenter extends PIndenter {
    public PSimpleIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    protected static String stripDoubleSlashComment(String line) {
        // This doesn't work for comments in string literals.
        // That *is* a real problem in:
        // if (line.startsWith("//")) {
        // Another case where we could do with Styler input?
        return line.replaceFirst("//.*", "");
    }
    // Note the plural.  There /*can*/ /*be*/ several of these on one line.
    protected static String stripMultiLineComments(String line) {
        // We should strip these but this is rarely important.
        // Multi-line comments usually include the whole line,
        // we're only stripping comments to find the "active" part of the line
        // and a line which is entirely comment has no active part.
        // This is fortunate because we don't have enough context
        // to know that we're in the middle of a multi-line comment.
        // Another case where we could ideally do with some Styler input?
        return line;
    }
    protected static String stripHashComment(String line) {
        return line.replaceFirst("#.*", "");
    }
    
    /**
     * Returns that part of the given line number that isn't leading/trailing whitespace or comment.
     */
    public String getActivePartOfLine(int lineNumber) {
        String line = textArea.getLineText(lineNumber);
        return stripComments(line).trim();
    }
    
    @Override
    public boolean isElectric(char c) {
        if (c == '#' && shouldMoveHashToColumnZero()) {
            return true;
        }
        if (c == ':' && shouldMoveLabels()) {
            return true;
        }
        if (isBlockEnd(c)) {
            return true;
        }
        return false;
    }
    
    public boolean isBlockBegin(char lastChar) {
        return PBracketUtilities.isOpenBracket(lastChar);
    }
    public boolean isBlockEnd(char firstChar) {
        return PBracketUtilities.isCloseBracket(firstChar);
    }

    public boolean isBlockBegin(String activePartOfLine) {
        if (activePartOfLine.length() == 0) {
            return false;
        }
        char lastChar = activePartOfLine.charAt(activePartOfLine.length() - 1);
        return isBlockBegin(lastChar);
    }
    public boolean isBlockEnd(String activePartOfLine) {
        if (activePartOfLine.length() == 0) {
            return false;
        }
        char firstChar = activePartOfLine.charAt(0);
        return isBlockEnd(firstChar);
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
    
    @Override
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
            } else if (previousLine.matches("/\\*{1,2}") || previousLine.startsWith("* ") || previousLine.equals("*")) {
                // We're in a doc comment.
                if (activePartOfLine.startsWith("*/")) {
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
