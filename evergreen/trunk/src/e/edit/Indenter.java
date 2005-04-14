package e.edit;

import e.util.*;
import java.util.regex.*;
import javax.swing.text.*;

public class Indenter {
    public String increaseIndentation(ETextArea text, String original) {
        return original + text.getIndentationString();
    }
    
    public String decreaseIndentation(ETextArea text, String original) {
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
    public String getIndentation(ETextArea text, int lineNumber) throws BadLocationException {
        return text.getIndentationOfLine(text.getPreviousNonBlankLineNumber(lineNumber));
    }
    
    public static final String INDENTATION_PROPERTY = "IndentationStringProperty";
    
    public void setIndentationPropertyBasedOnContent(ETextArea text, String content) {
        String indentation = text.getIndenter().guessIndentationFromFile(content);
        //System.err.println(filename + ": '" + indentation + "'");
        text.getPTextBuffer().putProperty(Indenter.INDENTATION_PROPERTY, indentation);
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
    private static final Pattern INDENTATION_PATTERN_1 = Pattern.compile("^(\\s+)[A-Za-z].*$");
    private static final Pattern INDENTATION_PATTERN_2 = Pattern.compile("^(\\s*)[{}]$");
}
