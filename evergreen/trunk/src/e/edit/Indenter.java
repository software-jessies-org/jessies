package e.edit;

import e.util.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.text.*;

public class Indenter {
    public String getIndentString() {
        return Parameters.getParameter("indent.string", "\t");
    }
    
    public String increaseIndentation(String original) {
        return original + getIndentString();
    }
    
    public String decreaseIndentation(String original) {
        String delta = getIndentString();
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
    
    public String guessIndentationFromFile(String fileContents) {
        String previousIndent = "";
        Set indentations = new HashSet();
        String emergencyAlternative = "    ";
        String[] lines = fileContents.split("\n");
        for (int i = 0; i < lines.length; ++i) {
            String line = lines[i];
            Matcher matcher = INDENTATION_PATTERN_1.matcher(line);
            if (matcher.matches()) {
                String indent = matcher.group(1);
                if (indent.length() < emergencyAlternative.length()) {
                    emergencyAlternative = indent;
                }
            }
            matcher = INDENTATION_PATTERN_2.matcher(line);
            if (matcher.matches()) {
                String indent = matcher.group(1);
                if (indent.length() > previousIndent.length()) {
                    String difference = indent.substring(previousIndent.length());
                    indentations.add(difference);
                }
                previousIndent = indent;
            }
        }
        if (indentations.size() == 0) {
            return emergencyAlternative;
        } else {
            String[] array = (String[]) indentations.toArray(new String[indentations.size()]);
            String shortest = array[0];
            for (int i = 1; i < array.length; ++i) {
                String indentation = (String) array[i];
                if (indentation.length() < shortest.length()) {
                    shortest = indentation;
                }
            }
            return shortest;
        }
    }
    private static final Pattern INDENTATION_PATTERN_1 = Pattern.compile("^( +)[A-Za-z].*$");
    private static final Pattern INDENTATION_PATTERN_2 = Pattern.compile("^( +)[{}]$");
}
