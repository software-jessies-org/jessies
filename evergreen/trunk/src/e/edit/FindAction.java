package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import javax.swing.text.*;
import e.util.*;

/**
The ETextArea action to open a 'find' dialog.

TODO:
    [ ] We ought to remember previous searches, but I'm not sure whether to do that here, or in the Minibuffer, for all its users.
*/
public class FindAction extends ETextAction implements MinibufferUser {
    public static final String ACTION_NAME = "Find...";
    
    public static final FindAction INSTANCE = new FindAction();
    
    /**
     * Used to mark the matches in the text as if they'd been gone over with a highlighter pen. We use
     * full yellow with half-alpha so you can see the selection through, as a dirty smudge, just like a real
     * highlighter pen might do.
     */
    public static final Highlighter.HighlightPainter PAINTER = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 255, 0, 128));
    
    public ETextWindow currentTextWindow;
    
    public String currentRegularExpression;
    
    private FindAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        currentTextWindow = getFocusedTextWindow();
        if (currentTextWindow == null) {
            return;
        }
        
        Edit.showMinibuffer(this);
    }
    
    //
    // MinibufferUser interface.
    //
    
    /** Stores the initial start of the selection so we can restore it if the user cancels the search. */
    private int initialSelectionStart;
    
    /** Stores the initial end of the selection so we can restore it if the user cancels the search. */
    private int initialSelectionEnd;
    
    public String getInitialValue() {
        ETextArea textArea = currentTextWindow.getText();
        initialSelectionStart = textArea.getSelectionStart();
        initialSelectionEnd = textArea.getSelectionEnd();
        String selectedText = textArea.getSelectedText();
        if (selectedText.length() == 0) {
            return currentRegularExpression;
        }
        return StringUtilities.regularExpressionFromLiteral(selectedText);
    }
    
    public String getPrompt() {
        return "Find";
    }
    
    public boolean isValid(String value) {
        return true;
    }
    
    public void valueChangedTo(String value) {
        findAllMatches(value);
    }
    
    /**
     * Interprets C-D and C-G as requests to jump to the previous and next highlighted
     * matches, respectively.
     */
    public boolean interpretSpecialKeystroke(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_D) {
            currentTextWindow.findPrevious();
            return true;
        }
        if (e.getKeyCode() == KeyEvent.VK_G) {
            currentTextWindow.findNext();
            return true;
        }
        return false;
    }
    
    public boolean wasAccepted(String value) {
        return true;
    }
    
    public void wasCanceled() {
        removeAllMatches();
        currentTextWindow.goToSelection(initialSelectionStart, initialSelectionEnd);
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
        findAllMatches(currentRegularExpression);
    }
    
    //
    // Find stuff.
    //
    
    public void removeAllMatches() {
        Highlighter highlighter = currentTextWindow.getText().getHighlighter();
        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        for (int i = 0; i < highlights.length; i++) {
            if (highlights[i].getPainter() == PAINTER) {
                highlighter.removeHighlight(highlights[i]);
            }
        }
        currentTextWindow.getBirdView().clearMatchingLines();
    }
    
    public void findAllMatches(String regularExpression) {
        removeAllMatches();
        
        // Do we have something to search for?
        if (regularExpression == null || regularExpression.length() == 0) {
            return;
        }
        
        // Do we have something to search in?
        ETextArea textArea = currentTextWindow.getText();
        String content = textArea.getText();
        if (content == null) {
            return;
        }
        
        currentRegularExpression = regularExpression;
        
        // Compile the regular expression.
        Pattern pattern;
        try {
            pattern = Pattern.compile(regularExpression);
        } catch (PatternSyntaxException patternSyntaxException) {
            Edit.showStatus(patternSyntaxException.getDescription());
            patternSyntaxException.printStackTrace();
            return;
        }
        
        // Find all the matches.
        int matchCount = 0;
        Matcher matcher = pattern.matcher(content);
        Highlighter highlighter = textArea.getHighlighter();
        while (matcher.find()) {
            try {
                currentTextWindow.getBirdView().addMatchingLine(textArea.getLineOfOffset(matcher.end()));
                highlighter.addHighlight(matcher.start(), matcher.end(), PAINTER);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
            matchCount++;
        }
        Edit.showStatus("Found " + matchCount + " " + (matchCount != 1 ? "matches" : "match"));
    }
}
