package e.ptextarea;

import java.util.*;

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
        if (PBracketUtilities.isCloseBracket(c)) {
            return true;
        }
        return false;
    }
    
    public boolean isBlockBegin(String activePartOfLine) {
        if (activePartOfLine.length() == 0) {
            return false;
        }
        char lastChar = activePartOfLine.charAt(activePartOfLine.length() - 1);
        return PBracketUtilities.isOpenBracket(lastChar);
    }
    
    public boolean isBlockEnd(String activePartOfLine) {
        if (activePartOfLine.length() == 0) {
            return false;
        }
        char firstChar = activePartOfLine.charAt(0);
        return PBracketUtilities.isCloseBracket(firstChar);
    }
    
    public boolean isSwitchLabel(String activePartOfLine) {
        return activePartOfLine.matches("(case\\b.*|default\\s*):");
    }
    
    /**
     * Lines that start with a closing brace or end with a opening brace or a colon tell us
     * definitively what the indentation for the next line should be.
     * Going back this far keeps us tidy in the face of various multi-line comment styles,
     * multi-line C++ output operator expressions and C++ preprocessor commands.
     * 
     * FIXME: more realistically, this tells us the minimum indentation. For example:
     *     { // This counts as "definitive"
     *         os << text
     *             // This line should start here, because it's a continuation of the previous line.
     *         // But this is where we'd assume it starts, because we have too much faith in "definitive" lines.
     */
    private boolean isDefinitive(String activePartOfLine) {
        return isBlockBegin(activePartOfLine) || isBlockEnd(activePartOfLine) || isLabel(activePartOfLine);
    }
    
    public int getPreviousDefinitiveLineNumber(int startLineNumber) {
        for (int lineIndex = startLineNumber - 1; lineIndex >= 0; --lineIndex) {
            String line = getActivePartOfLine(lineIndex);
            if (isDefinitive(line)) {
                return lineIndex;
            }
        }
        return -1;
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
        if (lineIndex > 0) {
            List<PLineSegment> segments = textArea.getLineSegments(lineIndex);
            if (segments.size() > 0) {
                PLineSegment lastSegment = segments.get(segments.size() - 1);
                if (lastSegment.getStyle() == PStyle.COMMENT) {
                    String commentText = lastSegment.getCharSequence().toString().trim();
                    if (commentText.startsWith("//") == false && commentText.endsWith("*/") == false) {
                        // We must be in a block comment. Assume it's JavaDoc style, and add a leading *.
                        indentation += " * ";
                    } else if (commentText.startsWith("*/")) {
                        // Add a space to line the * in */ up with the * in /*, like JavaDoc comments.
                        // FIXME: this is only correct if we're in a JavaDoc comment block. we should check for evidence.
                        indentation += " ";
                    }
                }
            }
        }
        return indentation;
    }
    
    protected abstract boolean isLabel(String activePartOfLine);
    protected abstract boolean shouldMoveHashToColumnZero();
    protected abstract boolean shouldMoveLabels();
}
