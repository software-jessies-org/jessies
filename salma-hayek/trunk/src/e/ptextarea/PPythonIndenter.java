package e.ptextarea;

import java.util.regex.*;

/**
 * Basic Python auto-indenter.
 * 
 * There are several ways in which this could be cleverer:
 * 1. we know that "else" and "elsif" belong with an "if", and "except" and "finally" with a "try", and not just any old block.
 * 2. we could cope with split lines, where the ":" isn't on the the same line as the "class" or "def" or whatever.
 */
public class PPythonIndenter extends  PSimpleIndenter {
    private Pattern END_OF_BLOCK = Pattern.compile("^\\s*(break|continue|raise|return|pass)\\b.*");
    private Pattern SAME_AS_BLOCK = Pattern.compile("^\\s*(else|elif|except|finally)\\b.*");
    private Pattern START_OF_NEW_BLOCK = Pattern.compile("^\\s*(class|def|elif|else|except|if|for|while|try)\\b.*");
    
    public PPythonIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    public boolean isElectric(char c) {
        return "abcdefghijklmnopqrstuvwxyz:".indexOf(c) != -1;
    }
    
    @Override
    protected String calculateNewIndentation(int lineNumber) {
        final int previousNonBlankLineNumber = getPreviousNonBlankLineNumber(lineNumber);
        if (previousNonBlankLineNumber == -1) {
            return "";
        } else {
            // A simplified variant of this code:
            // http://developer.kde.org/documentation/library/3.4-api/kate/html/kateautoindent_8cpp-source.html
            // (Extended to cope with Python classes and exceptions.)
            int pos = textArea.getLineStartOffset(lineNumber);
            int nestLevel = 0;
            boolean levelFound = false;
            int i = previousNonBlankLineNumber;
            for (; i >= 0; --i) {
                String line = textArea.getLineText(i);
                if (START_OF_NEW_BLOCK.matcher(line).matches()) {
                    if ((levelFound == false && nestLevel == 0) || (levelFound && nestLevel - 1 <= 0)) {
                        pos = textArea.getLineStartOffset(i);
                        break;
                    }
                    --nestLevel;
                } else if (END_OF_BLOCK.matcher(line).matches()) {
                    ++nestLevel;
                    levelFound = true;
                }
            }
            if (i == -1) {
                return "";
            }
            String indentation = getCurrentIndentationOfLine(previousNonBlankLineNumber);
            String previousLine = textArea.getLineText(previousNonBlankLineNumber);
            /*
            System.err.println("previous non-blank line #" + previousNonBlankLineNumber + ":");
            System.err.println(previousLine);
            System.err.println("indentation:");
            System.err.println(indentation + "|<");
            System.err.println("previous block line #" + i + ":");
            System.err.println(textArea.getLineText(i));
            System.err.println("indentation:");
            System.err.println(getCurrentIndentationOfLine(i) + "|<");
            */
            
            String currentLine = textArea.getLineText(lineNumber);
            if (SAME_AS_BLOCK.matcher(currentLine).matches()) {
                indentation = decreaseIndentation(indentation);
            } else if (SAME_AS_BLOCK.matcher(previousLine).matches()) {
                indentation = increaseIndentation(indentation);
            } else if (START_OF_NEW_BLOCK.matcher(previousLine).matches()) {
                indentation = increaseIndentation(indentation);
            } else if (END_OF_BLOCK.matcher(previousLine).matches()) {
                indentation = decreaseIndentation(indentation);
            }
            return indentation;
        }
    }
}
