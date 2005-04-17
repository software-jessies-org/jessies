package e.ptextarea;

import e.util.*;
import java.util.regex.*;

public class PIndenter {
    public String increaseIndentation(PTextArea text, String original) {
        return original + text.getIndentationString();
    }
    
    public String decreaseIndentation(PTextArea text, String original) {
        String delta = text.getIndentationString();
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
    public boolean isElectric(char c) {
        return (c == '}');
    }
    
    /** Returns the whitespace that should be used for the given line number. */
    public String getIndentation(PTextArea text, int lineNumber) {
        final int previousNonBlankLineNumber = getPreviousNonBlankLineNumber(text, lineNumber);
        return (previousNonBlankLineNumber == -1) ? "" : text.getIndentationOfLine(previousNonBlankLineNumber);
    }
    
    public void setIndentationPropertyBasedOnContent(PTextArea text, String content) {
        String indentation = text.getIndenter().guessIndentationFromFile(content);
        //System.err.println(filename + ": '" + indentation + "'");
        text.getPTextBuffer().putProperty(PTextBuffer.INDENTATION_PROPERTY, indentation);
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
        // FIXME: for C++ return line.matches(".*\\b(class|enum|struct|union)\\b.*")
        return false;
    }
    
    private static final Pattern INDENTATION_PATTERN_1 = Pattern.compile("^(\\s+)[A-Za-z].*$");
    private static final Pattern INDENTATION_PATTERN_2 = Pattern.compile("^(\\s*)[{}]$");
    
    protected static int getPreviousNonBlankLineNumber(PTextArea text, int startLineNumber) {
        for (int lineNumber = startLineNumber - 1; lineNumber >= 0; lineNumber--) {
            if (text.getLineText(lineNumber).trim().length() != 0) {
                return lineNumber;
            }
        }
        return -1;
    }
    
    /** Corrects the indentation of the line with the caret, optionally moving the caret. Returns true if the contents of the current line were changed. */
    public boolean correctIndentation(PTextArea textArea, boolean shouldMoveCaret) {
        // FIXME - selection
        int position = textArea.getSelectionStart();
        int lineNumber = textArea.getLineOfOffset(position);
        
        int offsetIntoLine = position - textArea.getLineStartOffset(lineNumber) - textArea.getIndentationOfLine(lineNumber).length();
        
        String whitespace = getIndentation(textArea, lineNumber);
        int lineStart = textArea.getLineStartOffset(lineNumber);
        int lineLength = textArea.getLineEndOffset(lineNumber) - lineStart;
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
            newCaretPosition = Math.min(newCaretPosition, textArea.getPTextBuffer().length());
            textArea.select(newCaretPosition, newCaretPosition);
        }
        return lineChanged;
    }
}
