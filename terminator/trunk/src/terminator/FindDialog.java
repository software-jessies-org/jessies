package terminator;

import e.forms.*;
import e.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import javax.swing.*;
import terminator.view.*;
import terminator.view.highlight.*;

public class FindDialog {
    private static final FindDialog INSTANCE = new FindDialog();
    
    private FindField findField = new FindField();
    private JLabel findStatus = new JLabel(" ");
    private JTextBuffer textToFindIn;
    
    private FindDialog() {
    }
    
    public static FindDialog getSharedInstance() {
        return INSTANCE;
    }
    
    public void showFindDialogFor(JTextBuffer text) {
        this.textToFindIn = text;
        
        JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, text);
        
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Find:", findField);
        formPanel.setStatusBar(findStatus);
        FormDialog.showNonModal(frame, "Find", formPanel);
        
        findField.selectAll();
        findField.requestFocus();
        findStatus.setText(" ");
        
        findField.find();
    }
    
    /**
     * Tests whether any of the Highlight objects in the array is a FindHighlighter.
     */
    private static boolean containsFindHighlight(Highlight[] highlights) {
        for (int i = 0; i < highlights.length; ++i) {
            if (highlights[i].getHighlighter() instanceof FindHighlighter) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Searches from startLine to endLine inclusive, incrementing the
     * current line by 'direction', looking for a line with a find highlight.
     * When one is found, the cursor is moved there.
     */
    private void findAgainIn(JTextBuffer text, int startLine, int endLine, int direction) {
        for (int i = startLine; i != endLine; i += direction) {
            Highlight[] highlights = text.getHighlightsForLine(i);
            if (containsFindHighlight(highlights)) {
                text.scrollToLine(i);
                return;
            }
        }
    }
    
    /**
     * Scrolls the display to the first find highlight not currently on the display.
     */
    public void findPreviousIn(JTextBuffer text) {
        findAgainIn(text, text.getFirstVisibleLine() - 1, -1, -1);
    }
    
    /**
     * Scrolls the display to the next find highlight not currently on the display.
     */
    public void findNextIn(JTextBuffer text) {
        findAgainIn(text, text.getLastVisibleLine() + 1, text.getModel().getLineCount() + 1, 1);
    }
    
    private FindHighlighter getFindHighlighter() {
        return (FindHighlighter) textToFindIn.getHighlighterOfClass(FindHighlighter.class);
    }
    
    private class FindField extends EMonitoredTextField {
        public FindField() {
            super(40);
            addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    if (textToFindIn != null && e.getKeyChar() == '\n') {
                        find();
                        e.consume();
                        textToFindIn.requestFocus();
                    }
                }
                
                public void keyPressed(KeyEvent e) {
                    if (textToFindIn != null && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        getFindHighlighter().forgetRegularExpression(textToFindIn);
                        textToFindIn.requestFocus();
                    }
                }
                
                public void keyReleased(KeyEvent e) {
                    if (TerminatorMenuBar.isKeyboardEquivalent(e)) {
                        if (e.getKeyCode() == KeyEvent.VK_D) {
                            findPreviousIn(textToFindIn);
                        } else if (e.getKeyCode() == KeyEvent.VK_G) {
                            findNextIn(textToFindIn);
                        }
                    }
                }
            });
        }
        
        public void timerExpired() {
            find();
        }
        
        public void find() {
            String regularExpression = getText();
            try {
                int matchCount = getFindHighlighter().setRegularExpression(textToFindIn, regularExpression);
                findStatus.setText("Matches: " + matchCount);
                setForeground(UIManager.getColor("TextField.foreground"));
            } catch (PatternSyntaxException ex) {
                setForeground(Color.RED);
                findStatus.setText(ex.getDescription());
            }
        }
    }
}
