package e.edit;

import java.awt.event.*;
import javax.swing.text.*;
import e.util.*;

/**
The ETextArea action that removes characters.
*/
public class BackspaceAction extends TextAction {
    public static final String ACTION_NAME = "back-space";

    public BackspaceAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextArea target = (ETextArea) getFocusedComponent();
        try {
            if (target.getSelectionStart() == target.getSelectionEnd()) {
                selectSomethingToDelete(target);
            }
            target.replaceSelection("");
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }
    
    public void selectSomethingToDelete(ETextArea target) throws BadLocationException {
        int charactersToDelete = 1;
        int position = target.getCaretPosition();
        String whitespace = target.getIndentationOfLineAtOffset(position);
        int lineOffset = position - target.getLineStartOffset(target.getLineOfOffset(position));
        if (Parameters.getParameter("hungryDelete", false)) {
            int startPosition = position - 1;
            if (Character.isWhitespace(target.getCharAt(startPosition))) {
                while (startPosition > 0 && Character.isWhitespace(target.getCharAt(startPosition - 1))) {
                    startPosition--;
                    charactersToDelete++;
                }
            }
        } else if (lineOffset > 1 && lineOffset <= whitespace.length()) {
            String tab = Parameters.getParameter("indent.string", "\t");
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
    }
}
