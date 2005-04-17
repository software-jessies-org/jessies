package e.edit;

import java.awt.event.*;
//import e.util.*;

/**
 * The ETextArea action that removes characters.
 */
public class BackspaceAction extends ETextAction {
    public static final String ACTION_NAME = "back-space";

    public BackspaceAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextArea target = getTextArea();
        if (target.getSelectionStart() == target.getSelectionEnd()) {
            selectSomethingToDelete(target);
        }
        target.replaceSelection("");
    }
    
    public void selectSomethingToDelete(ETextArea target) {
        /*
        int charactersToDelete = 1;
        // FIXME - selection
        int position = target.getSelectionStart();
        String whitespace = target.getIndentationOfLineAtOffset(position);
        int lineOffset = position - target.getLineStartOffset(target.getLineOfOffset(position));
        CharSequence chars = target.charSequence();
        if (Parameters.getParameter("hungryDelete", false)) {
            int startPosition = position - 1;
            if (Character.isWhitespace(chars.charAt(startPosition))) {
                while (startPosition > 0 && Character.isWhitespace(chars.charAt(startPosition - 1))) {
                    startPosition--;
                    charactersToDelete++;
                }
            }
        } else if (lineOffset > 1 && lineOffset <= whitespace.length()) {
            String tab = target.getIndentationString();
            whitespace = whitespace.substring(0, lineOffset);
            while (whitespace.startsWith(tab)) {
                whitespace = whitespace.substring(tab.length());
            }
            charactersToDelete = whitespace.length();
            if (charactersToDelete == 0) {
                charactersToDelete = tab.length();
            }
        }
        target.select(position - charactersToDelete, position);
        */
    }
}
