package e.edit;

import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.event.*;
import java.util.regex.*;

public class FindAction extends ETextAction implements MinibufferUser {
    public static final FindAction INSTANCE = new FindAction();
    
    private ETextWindow currentTextWindow;
    private StringHistory regularExpressionHistory;
    
    private FindAction() {
        super("_Find...", GuiUtilities.makeKeyStroke("F", false));
        GnomeStockIcon.configureAction(this);
        regularExpressionHistory = new StringHistory(Evergreen.getPreferenceFilename("e.edit.FindAction-history"));
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow newCurrentTextWindow = getFocusedTextWindow();
        if (newCurrentTextWindow == null) {
            return;
        }
        
        // Only now can we set currentTextWindow; otherwise "Find" while the
        // find mini-buffer is already up fails, and causes "Find Next" and
        // "Find Previous" to also fail.
        currentTextWindow = newCurrentTextWindow;
        Evergreen.getInstance().showMinibuffer(this);
    }
    
    //
    // MinibufferUser interface.
    //
    
    /** Stores the initial start of the selection so we can restore it if the user cancels the search. */
    private int initialSelectionStart;
    
    /** Stores the initial end of the selection so we can restore it if the user cancels the search. */
    private int initialSelectionEnd;
    
    public StringHistory getHistory() {
        return regularExpressionHistory;
    }
    
    public String getInitialValue() {
        PTextArea textArea = currentTextWindow.getTextArea();
        this.initialSelectionStart = textArea.getSelectionStart();
        this.initialSelectionEnd = textArea.getSelectionEnd();
        String selectedText = textArea.getSelectedText();
        if (selectedText.length() == 0) {
            String currentRegularExpression = currentTextWindow.getCurrentRegularExpression();
            return (currentRegularExpression != null) ? currentRegularExpression : "";
        }
        return "(?-i)" + StringUtilities.regularExpressionFromLiteral(selectedText);
    }
    
    public String getPrompt() {
        return "Find";
    }
    
    /** Checks that we can compile the pattern okay. */
    public boolean isValid(String value) {
        try {
            Pattern.compile(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    
    public void valueChangedTo(String value) {
        findAllMatches(value);
    }
    
    public boolean wasAccepted(String value) {
        return true;
    }
    
    public void wasCanceled() {
        removeAllMatches();
        currentTextWindow.setCurrentRegularExpression(null);
        currentTextWindow.getTextArea().select(initialSelectionStart, initialSelectionEnd);
    }
    
    //
    // Programmatic interface to finding.
    //
    
    public void findInText(ETextWindow textWindow, String regularExpression) {
        currentTextWindow = textWindow;
        findAllMatches(regularExpression);
    }
    
    public void repeatLastFind(ETextWindow textWindow) {
        currentTextWindow = textWindow;
        findAllMatches(currentTextWindow.getCurrentRegularExpression());
    }
    
    //
    // Find stuff.
    //
    
    public void removeAllMatches() {
        currentTextWindow.getTextArea().removeHighlights(PFind.MatchHighlight.HIGHLIGHTER_NAME);
        currentTextWindow.getBirdView().clearMatchingLines();
    }
    
    private void findAllMatches(String regularExpression) {
        currentTextWindow.setCurrentRegularExpression(regularExpression);
        PTextArea textArea = currentTextWindow.getTextArea();
        try {
            textArea.findAllMatches(regularExpression, currentTextWindow.getBirdView());
            currentTextWindow.updateStatusLine();
        } catch (PatternSyntaxException patternSyntaxException) {
            Evergreen.getInstance().showStatus(patternSyntaxException.getDescription());
            return;
        }
    }
}
