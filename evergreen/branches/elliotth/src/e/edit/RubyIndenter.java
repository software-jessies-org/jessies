package e.edit;

import java.util.regex.*;

public class RubyIndenter extends Indenter {
    private Pattern increasersPattern = Pattern.compile("^begin\\b|^case\\b|^catch\\b|^class\\b|^def\\b|^else\\b|^elsif\\b|^ensure\\b|^for\\b|^if\\b|^module\\b|^rescue\\b|^when\\b|^while\\b|^unless\\b|^until\\b|\\{$");
    
    public boolean isElectric(char c) {
        // These are the final characters in end, else, elsif, when, ensure, rescue and }.
        return "defn}".indexOf(c) != -1;
    }
    
    public String getIndentation(ETextArea text, int lineNumber) throws javax.swing.text.BadLocationException {
        int previousNonBlank = text.getPreviousNonBlankLineNumber(lineNumber);
        if (previousNonBlank == 0) {
            return "";
        }
        
        String indentation = text.getIndentationOfLine(previousNonBlank);
        String trimmedPrevious = text.getLineText(previousNonBlank).trim();
        
        // Add a 'shiftwidth' after lines beginning with keywords that imply an increase in indentation.
        boolean haveIncreasedIndentation = false;
        Matcher increasersMatcher = increasersPattern.matcher(trimmedPrevious);
//        System.err.println("testing increasers");
        if (increasersMatcher.find()) {
//            System.err.println("found an increaser");
            // || trimmedPrevious =~ '\({\|\<do\>\).*|.*|\s*$' || trimmedPrevious =~ '\<do\>\(\s*#.*\)\=$') {
            indentation = increaseIndentation(indentation);
            haveIncreasedIndentation = true;
        }
        
        // Subtract a 'shiftwidth' after lines ending with "end" (and an optional comment) when
        // they begin with something that would otherwise cause us to increase the indentation.
        if (haveIncreasedIndentation && trimmedPrevious.matches(".*\\<end\\>\\s*(#.*)?$")) {
//            System.err.println("found a nullifier");
            indentation = decreaseIndentation(indentation);
        }
        
        // Subtract a 'shiftwidth' on end, else and, elsif, when and '}'.
        String trimmedLine = text.getLineText(lineNumber).trim();
        if (trimmedLine.matches("end\\b|else\\b|elsif\\b.*|when\\b.*|ensure\\b|rescue\\b.*|}")) {
//            System.err.println("found a decreaser");
            indentation = decreaseIndentation(indentation);
        }
        
        return indentation;
    }
}
