package e.util;

import java.util.regex.*;

/**
 * Tries to guess the depth of indentation ("two spaces", "four spaces",
 * "single tab", et cetera) in use.
 */
public class IndentationGuesser {
    private static final Pattern INDENTATION_PATTERN_1 = Pattern.compile("^(\\s+)[A-Za-z].*$");
    private static final Pattern INDENTATION_PATTERN_2 = Pattern.compile("^(\\s*)[{}]$");
    
    /**
     * Returns the best guess at the indentation in use in the given content.
     */
    public static String guessIndentationFromFile(CharSequence chars) {
        // FIXME: we shouldn't force our callers to pay for this!
        String fileContents = chars.toString();

        String previousIndent = "";
        Bag<String> indentations = new Bag<String>();
        String emergencyAlternative = Parameters.getParameter("indent.string", "    ");
        String[] lines = fileContents.split("\n");
        for (String line : lines) {
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
        //System.out.println("indentations=" + indentations);
        if (indentations.isEmpty()) {
            //System.out.println(" - no line just containing an indented brace?");
            return emergencyAlternative;
        } else {
            return indentations.commonestItem();
        }
    }
    
    private IndentationGuesser() {
    }
}
