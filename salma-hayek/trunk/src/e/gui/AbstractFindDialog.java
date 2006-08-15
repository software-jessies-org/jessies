package e.gui;

import e.forms.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import javax.swing.*;

/**
 * A basic find dialog suitable for use with PTextArea or JTextPane or whatever text component you happen to have.
 */
public abstract class AbstractFindDialog {
    public void showFindDialog(Component parent, final JTextField findField) {
        Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, parent);
        
        final JLabel findStatus = new JLabel(" ");
        
        FormBuilder form = new FormBuilder(frame, "Find");
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Find:", findField);
        formPanel.setStatusBar(findStatus);
        formPanel.setTypingTimeoutActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    findField.setForeground(UIManager.getColor("TextField.foreground"));
                    int matchCount = updateFindResults(findField.getText());
                    findStatus.setText("Matches: " + matchCount);
                } catch (java.util.regex.PatternSyntaxException ex) {
                    findField.setForeground(Color.RED);
                    findStatus.setText(ex.getDescription());
                }
            }
        });
        form.getFormDialog().setCancelRunnable(new Runnable() {
            public void run() {
                clearFindResults();
            }
        });
        form.showNonModal();
        
        findField.selectAll();
        findField.requestFocus();
        findStatus.setText(" ");
    }
    
    /**
     * Override this to highlight all the matches for the given regular expression.
     * Return the number of matches.
     * Throw PatternSyntaxException if you couldn't compile the regular expression, and we'll report the problem to the user.
     */
    public abstract int updateFindResults(String regularExpression) throws PatternSyntaxException;
    
    /**
     * Override this to remove all the highlights (and do any other necessary cleanup) when the user cancels the find.
     */
    public abstract void clearFindResults();
}
