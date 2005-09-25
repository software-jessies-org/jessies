package terminator;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import javax.swing.*;
import terminator.view.*;
import terminator.view.highlight.*;

public class FindDialog {
    private static final FindDialog INSTANCE = new FindDialog();
    
    private JTextField findField;
    private JLabel findStatus = new JLabel(" ");
    private JTextBuffer textToFindIn;
    private TerminatorMenuBar.BindableAction findNextAction;
    private TerminatorMenuBar.BindableAction findPreviousAction;
    
    private FindDialog() {
        initFindField();
    }
    
    private void initFindField() {
        findField = new JTextField(40);
        findNextAction = new TerminatorMenuBar.FindNextAction();
        ComponentUtilities.initKeyBinding(findField, findNextAction);
        findPreviousAction = new TerminatorMenuBar.FindPreviousAction();
        ComponentUtilities.initKeyBinding(findField, findPreviousAction);
    }
    
    public static FindDialog getSharedInstance() {
        return INSTANCE;
    }
    
    private void initFindField(final JTerminalPane terminalPane) {
        findNextAction.bindTo(terminalPane);
        findPreviousAction.bindTo(terminalPane);
        String selection = terminalPane.getSelectionHighlighter().getTabbedString();
        if (selection.length() > 0) {
            findField.setText(StringUtilities.regularExpressionFromLiteral(selection));
        }
    }
    
    public void showFindDialogFor(final JTerminalPane terminalPane) {
        this.textToFindIn = terminalPane.getTextPane();
        initFindField(terminalPane);
        
        JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, terminalPane);
        
        FormBuilder form = new FormBuilder(frame, "Find");
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Find:", findField);
        formPanel.setStatusBar(findStatus);
        formPanel.setTypingTimeoutActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                find();
            }
        });
        
        FormDialog dialog = form.getFormDialog();
        dialog.setAcceptRunnable(new Runnable() {
            public void run() {
                // If the user brought up the dialog, typed a regular
                // expression, and hit return before the typing timeout went
                // off, we need to find. Unfortunately, we don't know if we
                // already did, so the slow typist pays a little extra.
                // FXIME: Should accepting a FormDialog always force the typing
                // timeout? A quick search of the places we're currently using
                // it suggests it would mostly be unwanted. But maybe there's
                // some better pattern we just haven't found yet.
                find();
            }
        });
        dialog.setCancelRunnable(new Runnable() {
            public void run() {
                getFindHighlighter().forgetRegularExpression(textToFindIn);
            }
        });
        dialog.setRememberBounds(false);
        form.showNonModal();
        
        findField.selectAll();
        findField.requestFocus();
        findStatus.setText(" ");
    }
    
    private FindHighlighter getFindHighlighter() {
        return (FindHighlighter) textToFindIn.getHighlighterOfClass(FindHighlighter.class);
    }
    
    private void find() {
        String regularExpression = findField.getText();
        Pattern pattern = null;
        if (regularExpression.length() > 0) {
            try {
                pattern = PatternUtilities.smartCaseCompile(regularExpression);
                clearStatus();
            } catch (PatternSyntaxException ex) {
                setStatus(ex.getDescription(), true);
                return;
            }
        }
        
        int matchCount = getFindHighlighter().setRegularExpression(textToFindIn, pattern);
        setStatus("Matches: " + matchCount, false);
    }
    
    private void setStatus(String text, boolean isError) {
        findField.setForeground(isError ? Color.RED : UIManager.getColor("TextField.foreground"));
        findStatus.setText(text);
    }
    
    private void clearStatus() {
        setStatus(" ", false);
    }
}
