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
        
        // Recognize block comments, and help out with the ASCII art.
        if (lineIndex > 0) {
            List<PLineSegment> previousLineSegments = textArea.getLineSegments(lineIndex - 1);
            if (previousLineSegments.size() > 0) {
                // Extract the previous line's comment text.
                String previousLineCommentText = "";
                for (PLineSegment segment : previousLineSegments) {
                    if (segment.getStyle() == PStyle.COMMENT) {
                        previousLineCommentText += segment.getCharSequence();
                    }
                }
                previousLineCommentText = previousLineCommentText.trim();
                
                // Extract this line's text. I think we can safely infer whether or not it's comment (see below).
                String currentLineText = textArea.getLineText(lineIndex).trim();
                
                // NewlineInserter treats /** and /* the same way, so we should too.
                if (previousLineCommentText.startsWith("/*") || previousLineCommentText.startsWith("*")) {
                    // We're either part-way through, or on the line after, a block comment.
                    if (previousLineCommentText.endsWith("*/")) {
                        // We're on the line after, so we must leave the current line's indentation as it is.
                    } else if (currentLineText.endsWith("*/")) {
                        // We're on the last line, so add a leading " " to line the "*/" on this line up with the "*" above.
                        indentation += " ";
                    } else {
                        // We're part-way through a JavaDoc-style  block comment, so add a leading " * ".
                        indentation += " * ";
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
