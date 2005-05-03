package e.ptextarea;

import e.util.*;
import java.util.regex.*;

public abstract class PIndenter {
    protected PTextArea textArea;
    
    public PIndenter(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    public String increaseIndentation(String original) {
        return original + textArea.getIndentationString();
    }
    
    public String decreaseIndentation(String original) {
        String delta = textArea.getIndentationString();
        if (original.endsWith(delta)) {
            return original.substring(0, original.length() - delta.length());
        }
        return original;
    }
    
    /**
     * Returns true if c is an 'electric' character, which is emacs terminology
     * for a character that may cause the indentation to be modified when you
     * type it. Typically, this signifies the end of a block.
     * Note the "may": this saves us correcting the indentation on every
     * character insertion.  That's an optimisation but also prevents us being
     * unusable when the indenter doesn't support the indentation style
     * in use.
     */
    public abstract boolean isElectric(char c);
    
    /**
     * Returns a copy of just the leading whitespace part of the given line.
     */
    public String getCurrentIndentationOfLine(int lineNumber) {
        int start = textArea.getLineStartOffset(lineNumber);
        int max = textArea.getLineEndOffsetBeforeTerminator(lineNumber);
        int end;
        CharSequence chars = textArea.getTextBuffer();
        for (end = start; end < max; ++end) {
            char nextChar = chars.charAt(end);
            if (nextChar != ' ' && nextChar != '\t') {
                break;
            }
        }
        return chars.subSequence(start, end).toString();
    }
    
    /** Returns the whitespace that should be used for the given line number. */
    public String getIndentation(int lineNumber) {
        final int previousNonBlankLineNumber = getPreviousNonBlankLineNumber(lineNumber);
        return (previousNonBlankLineNumber == -1) ? "" : getCurrentIndentationOfLine(previousNonBlankLineNumber);
    }
    
    public void setIndentationPropertyBasedOnContent(String content) {
        String indentation = IndentationGuesser.guessIndentationFromFile(content);
        //System.err.println(filename + ": '" + indentation + "'");
        textArea.getTextBuffer().putProperty(PTextBuffer.INDENTATION_PROPERTY, indentation);
    }
    
    public boolean isInNeedOfClosingSemicolon(String line) {
        return false;
    }
    
    protected int getPreviousNonBlankLineNumber(int startLineNumber) {
        for (int lineNumber = startLineNumber - 1; lineNumber >= 0; lineNumber--) {
            if (textArea.getLineText(lineNumber).trim().length() != 0) {
                return lineNumber;
            }
        }
        return -1;
    }
    
    /** Corrects the indentation of the line with the caret, optionally moving the caret. Returns true if the contents of the current line were changed. */
    public boolean fixIndentation() {
        // FIXME - selection
        return fixIndentationAt(textArea.getSelectionStart());        
    }
    
    // charsInserted is allowed to be negative.
    private static int adjustOffsetAfterInsertion(int offsetToAdjust, int offsetOfInsertion, int charsInserted) {
        if (offsetToAdjust < offsetOfInsertion) {
            return offsetToAdjust;
        } else {
            return offsetToAdjust + charsInserted;
        }
    }
    
    public boolean fixIndentationAt(int position) {
        int lineIndex = textArea.getLineOfOffset(position);
        String originalIndentation = getCurrentIndentationOfLine(lineIndex);
        String replacementIndentation = getIndentation(lineIndex);
        //Log.warn("originalIndentation=@" + originalIndentation + "@; replacementIndentation=@" + replacementIndentation + "@");
        boolean lineChanged = (replacementIndentation.equals(originalIndentation) == false);
        if (lineChanged) {
            int lineStartOffset = textArea.getLineStartOffset(lineIndex);
            int charsInserted = replacementIndentation.length() - originalIndentation.length();
            int desiredStartOffset = adjustOffsetAfterInsertion(textArea.getSelectionStart(), lineStartOffset, charsInserted);
            int desiredEndOffset = adjustOffsetAfterInsertion(textArea.getSelectionEnd(), lineStartOffset, charsInserted);
            textArea.replaceRange(replacementIndentation, lineStartOffset, lineStartOffset + originalIndentation.length());
            textArea.select(desiredStartOffset, desiredEndOffset);
        }
        return lineChanged;
    }
    
    public Range getRangeToRemove() {
        if (textArea.hasSelection()) {
            // The user's already done our work for us.
            return new Range(textArea.getSelectionStart(), textArea.getSelectionEnd());
        }
        
        int position = textArea.getSelectionStart();
        if (position == 0) {
            // We can't remove anything before the beginning.
            return Range.NULL_RANGE;
        }
        
        int charactersToDelete = 1;
        final int lineNumber = textArea.getLineOfOffset(position);
        String whitespace = getCurrentIndentationOfLine(lineNumber);
        int lineOffset = position - textArea.getLineStartOffset(lineNumber);
        CharSequence chars = textArea.getTextBuffer();
        if (Parameters.getParameter("hungryDelete", false)) {
            int startPosition = position - 1;
            if (Character.isWhitespace(chars.charAt(startPosition))) {
                while (startPosition > 0 && Character.isWhitespace(chars.charAt(startPosition - 1))) {
                    startPosition--;
                    charactersToDelete++;
                }
            }
        } else if (lineOffset > 1 && lineOffset <= whitespace.length()) {
            String tab = textArea.getIndentationString();
            whitespace = whitespace.substring(0, lineOffset);
            while (whitespace.startsWith(tab)) {
                whitespace = whitespace.substring(tab.length());
            }
            charactersToDelete = whitespace.length();
            if (charactersToDelete == 0) {
                charactersToDelete = tab.length();
            }
        }
        
        return new Range(position - charactersToDelete, position);
    }
}
