package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class CorrectSpellingAction extends AbstractAction {
    private ETextWindow textWindow;
    private String replacement;
    private int startIndex;
    private int endIndex;
    
    public CorrectSpellingAction(ETextWindow textWindow, String replacement, int startIndex, int endIndex) {
        super("Correct to '" + replacement + "'");
        this.textWindow = textWindow;
        this.replacement = replacement;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }
    
    public void actionPerformed(ActionEvent e) {
        textWindow.getText().replaceRange(replacement, startIndex, endIndex);
    }
}
