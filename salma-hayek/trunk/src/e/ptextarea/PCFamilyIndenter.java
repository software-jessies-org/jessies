package e.ptextarea;

/**
 * Implements indentation for members of the C family, parameterized to cater for their differences.
 */
public abstract class PCFamilyIndenter extends PSimpleIndenter {
    public PCFamilyIndenter(PTextArea textArea) {
        super(textArea);
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
     * */
    public boolean isDefinitive(String rawLine) {
        String trimmedLine = rawLine.trim();
        return isBlockBegin(trimmedLine) || isBlockEnd(trimmedLine) || isLabel(trimmedLine);
    }
    
    public int getPreviousDefinitiveLineNumber(int startLineNumber) {
        for (int lineIndex = startLineNumber - 1; lineIndex >= 0; --lineIndex) {
            String line = textArea.getLineText(lineIndex);
            if (isDefinitive(line)) {
                return lineIndex;
            }
        }
        return -1;
    }
    
    private String getActivePartOfLine(int lineIndex) {
        StringBuilder activePartOfLine = new StringBuilder();
        for (PLineSegment segment : textArea.getLineSegments(lineIndex)) {
            PStyle style = segment.getStyle();
            if (style == PStyle.NORMAL || style == PStyle.KEYWORD || style == PStyle.PREPROCESSOR) {
                activePartOfLine.append(segment.getCharSequence());
            }
        }
        return activePartOfLine.toString().trim();
    }
    
    @Override
    public String calculateNewIndentation(int lineIndex) {
        String activePartOfLine = getActivePartOfLine(lineIndex);
        
        if (shouldMoveHashToColumnZero() && activePartOfLine.startsWith("#")) {
            return "";
        }
        
        String indentation = "";
        int previousDefinitive = getPreviousDefinitiveLineNumber(lineIndex);
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
        if (lineIndex > 0 && shouldContinueDocComments()) {
            String previousLine = textArea.getLineText(lineIndex - 1).trim();
            if (previousLine.endsWith("*/")) {
                // Whatever the previous line looks like, if it ends with
                // a close of comment, we're not in a comment, and should
                // do nothing.
            } else if (previousLine.matches("/\\*{1,2}") || previousLine.startsWith("* ") || previousLine.equals("*")) {
                // We're in a doc comment.
                // FIXME: this is broken now activePartOfLine doesn't include comments.
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
    
    protected abstract boolean isLabel(String activePartOfLine);
    protected abstract boolean shouldMoveHashToColumnZero();
    protected abstract boolean shouldMoveLabels();
    protected abstract boolean shouldContinueDocComments();
}
