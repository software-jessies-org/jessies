package e.ptextarea;

import e.util.*;
import java.util.regex.*;

abstract public class PIndenter {
    private static final Pattern INDENTATION_PATTERN_1 = Pattern.compile("^(\\s+)[A-Za-z].*$");
    private static final Pattern INDENTATION_PATTERN_2 = Pattern.compile("^(\\s*)[{}]$");
    
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
     * for a character that causes the indentation to be modified when you
     * type it. Typically, this signifies the end of a block.
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
        String indentation = textArea.getIndenter().guessIndentationFromFile(content);
        //System.err.println(filename + ": '" + indentation + "'");
        textArea.getTextBuffer().putProperty(PTextBuffer.INDENTATION_PROPERTY, indentation);
    }
    
    public String guessIndentationFromFile(String fileContents) {
        String previousIndent = "";
        Bag indentations = new Bag();
        String emergencyAlternative = Parameters.getParameter("indent.string", "    ");
        String[] lines = fileContents.split("\n");
        for (int i = 0; i < lines.length; ++i) {
            String line = lines[i];
            Matcher matcher = INDENTATION_PATTERN_1.matcher(line);
            if (matcher.matches()) {
                String indent = matcher.group(1);
                if (indent.length() < emergencyAlternative.length()) {
                    emergencyAlternative = indent;
                }
                previousIndent = indent;
            }
            matcher = INDENTATION_PATTERN_2.matcher(line);
            if (matcher.matches()) {
                String indent = matcher.group(1);
                if (indent.length() > previousIndent.length()) {
                    String difference = indent.substring(previousIndent.length());
                    indentations.add(difference);
                } else if (indent.length() < previousIndent.length()) {
                    String difference = previousIndent.substring(indent.length());
                    indentations.add(difference);
                }
                previousIndent = indent;
            }
        }
        System.err.println("indentations=" + indentations);
        if (indentations.isEmpty()) {
            System.err.println(" - no line just containing an indented brace?");
            return emergencyAlternative;
        } else {
            return (String) indentations.commonestItem();
        }
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
    public boolean correctIndentation(boolean shouldMoveCaret) {
        // FIXME - selection
        int position = textArea.getSelectionStart();
        int lineNumber = textArea.getLineOfOffset(position);
        
        int offsetIntoLine = position - textArea.getLineStartOffset(lineNumber) - getCurrentIndentationOfLine(lineNumber).length();
        
        String whitespace = getIndentation(lineNumber);
        int lineStart = textArea.getLineStartOffset(lineNumber);
        int lineLength = textArea.getLineEndOffsetBeforeTerminator(lineNumber) - lineStart;
        String originalLine = textArea.getLineText(lineNumber);
        String line = originalLine.trim();
        String replacement = whitespace + line;
        //Log.warn("line=@" + originalLine + "@; replacement=@" + replacement + "@");
        boolean lineChanged = (replacement.equals(originalLine) == false);
        if (lineChanged) {
            textArea.replaceRange(whitespace + line, lineStart, lineStart + lineLength);
            if (shouldMoveCaret == false) {
                final int offset = lineStart + whitespace.length() + offsetIntoLine;
                textArea.select(offset, offset);
            }
        }
        if (shouldMoveCaret) {
            // Move the caret ready to perform the same service to the next line.
            int newCaretPosition = lineStart + whitespace.length() + line.length() + 1;
            newCaretPosition = Math.min(newCaretPosition, textArea.getTextBuffer().length());
            textArea.select(newCaretPosition, newCaretPosition);
        }
        return lineChanged;
    }
    
    public Range getRangeToRemove() {
        Range range = new Range();
        
        if (textArea.hasSelection()) {
            // The user's already done our work for us.
            range.start = textArea.getSelectionStart();
            range.end = textArea.getSelectionEnd();
            return range;
        }
        
        int charactersToDelete = 1;
        int position = textArea.getSelectionStart();
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
        
        range.start = position - charactersToDelete;
        range.end = position;
        return range;
    }
}
