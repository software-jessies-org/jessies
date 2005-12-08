package e.ptextarea;

import e.util.*;
import java.util.regex.*;

public abstract class PIndenter {
    protected PTextArea textArea;
    private Pattern indentationPattern = Pattern.compile("(^[ \\t]*(?:\\*(?: |$))?).*$");
    
    public PIndenter(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    protected String increaseIndentation(String original) {
        return original + textArea.getIndentationString();
    }
    
    protected String decreaseIndentation(String original) {
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
     * Returns a copy of just the leading part of the given line.
     * Usually that just contains whitespace but the asterisks at the start
     * of the body lines of a doc-comment are also considered as indentation
     * because they end in (a single character of) whitespace.
     */
    public String getCurrentIndentationOfLine(int lineNumber) {
        String line = textArea.getLineText(lineNumber);
        Matcher matcher = indentationPattern.matcher(line);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            throw new IllegalArgumentException("line number " + lineNumber + " \"" + line + "\" has impossible indentation");
        }
    }
    
    /** Returns the indentation which should be used for the given line number. */
    private String getIndentation(int lineNumber) {
        final int previousNonBlankLineNumber = getPreviousNonBlankLineNumber(lineNumber);
        return (previousNonBlankLineNumber == -1) ? "" : getCurrentIndentationOfLine(previousNonBlankLineNumber);
    }
    
    public void setIndentationPropertyBasedOnContent(CharSequence content) {
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
    
    /** Corrects the indentation of the lines with the selection. Returns true if the contents of the current line were changed. */
    public void fixIndentation() {
        fixIndentationBetween(textArea.getSelectionStart(), textArea.getSelectionEnd());        
    }
    
    private static int adjustOffsetAfterInsertion(int offsetToAdjust, int lineStartOffset, String originalIndentation, String replacementIndentation) {
        if (offsetToAdjust < lineStartOffset) {
            return offsetToAdjust;
        } else if (offsetToAdjust > lineStartOffset + originalIndentation.length()) {
            int charsInserted = replacementIndentation.length() - originalIndentation.length();
            return offsetToAdjust + charsInserted;
        } else {
            return lineStartOffset + replacementIndentation.length();
        }
    }
    
    private static int adjustOffsetAfterDeletion(int offsetToAdjust, int offsetOfDeletion, int charsDeleted) {
        if (offsetToAdjust < offsetOfDeletion) {
            return offsetToAdjust;
        } else if (offsetToAdjust > offsetOfDeletion + charsDeleted) {
            return offsetToAdjust - charsDeleted;
        } else {
            return offsetOfDeletion;
        }
    }
    
    public void fixIndentationBetween(final int start, final int end) {
        final int startLine = textArea.getLineOfOffset(start);
        // I've thought about (and experimented with) the +-1 issue here.
        final int finishLine = textArea.getLineOfOffset(end);
        for (int line = startLine; line <= finishLine; ++line) {
            fixIndentationAt(textArea.getLineStartOffset(line));
        }
    }

    public void fixIndentationAt(int position) {
        int lineIndex = textArea.getLineOfOffset(position);
        String originalIndentation = getCurrentIndentationOfLine(lineIndex);
        String replacementIndentation = getIndentation(lineIndex);
        String originalLine = textArea.getLineText(lineIndex);
        String replacementLine = replacementIndentation + StringUtilities.trimTrailingWhitespace(originalLine.substring(originalIndentation.length()));
        //Log.warn("originalIndentation=@" + originalIndentation + "@; replacementIndentation=@" + replacementIndentation + "@");
        if (replacementLine.equals(originalLine)) {
            return;
        }
        int lineStartOffset = textArea.getLineStartOffset(lineIndex);
        int charsInserted = replacementIndentation.length() - originalIndentation.length();
        int desiredStartOffset = adjustOffsetAfterInsertion(textArea.getSelectionStart(), lineStartOffset, originalIndentation, replacementIndentation);
        int desiredEndOffset = adjustOffsetAfterInsertion(textArea.getSelectionEnd(), lineStartOffset, originalIndentation, replacementIndentation);
        int trimOffset = lineStartOffset + replacementLine.length();
        int charsTrimmed = originalLine.length() - (replacementLine.length() - charsInserted);
        desiredStartOffset = adjustOffsetAfterDeletion(desiredStartOffset, trimOffset, charsTrimmed);
        desiredEndOffset = adjustOffsetAfterDeletion(desiredEndOffset, trimOffset, charsTrimmed);
        textArea.replaceRange(replacementLine, lineStartOffset, lineStartOffset + originalLine.length());
        textArea.select(desiredStartOffset, desiredEndOffset);
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
