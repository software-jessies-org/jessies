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
    
    public void showFindDialogFor(final JTerminalPane terminalPane) {
        this.textToFindIn = terminalPane.getTextPane();
        findNextAction.bindTo(terminalPane);
        findPreviousAction.bindTo(terminalPane);
        
        JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, terminalPane);
        
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Find:", findField);
        formPanel.setStatusBar(findStatus);
        formPanel.setTypingTimeoutActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                find();
            }
        });
        
        FormDialog dialog = FormDialog.showNonModal(frame, "Find", formPanel);
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
        
        findField.selectAll();
        findField.requestFocus();
        findStatus.setText(" ");
    }
    
    private FindHighlighter getFindHighlighter() {
        return (FindHighlighter) textToFindIn.getHighlighterOfClass(FindHighlighter.class);
    }
    
    private void find() {
        String regularExpression = findField.getText();
        try {
            int matchCount = getFindHighlighter().setRegularExpression(textToFindIn, regularExpression);
            findStatus.setText("Matches: " + matchCount);
            findField.setForeground(UIManager.getColor("TextField.foreground"));
        } catch (PatternSyntaxException ex) {
            findField.setForeground(Color.RED);
            findStatus.setText(ex.getDescription());
        }
    }
}
