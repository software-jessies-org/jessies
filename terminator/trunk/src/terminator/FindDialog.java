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
    
    private FormDialog formDialog;
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
            findField.setText("(?-i)" + StringUtilities.regularExpressionFromLiteral(selection));
        }
    }
    
    public synchronized void showFindDialogFor(final JTerminalPane terminalPane) {
        if (formDialog != null) {
            formDialog.acceptDialog();
            formDialog = null;
        }
        
        this.textToFindIn = terminalPane.getTextPane();
        initFindField(terminalPane);
        
        JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, terminalPane);
        
        FormBuilder form = new FormBuilder(frame, "Find");
        form.setStatusBar(findStatus);
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Find:", PatternUtilities.addRegularExpressionHelpToComponent(findField));
        formPanel.setTypingTimeoutActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                find();
            }
        });
        
        formDialog = form.getFormDialog();
        formDialog.setAcceptCallable(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() {
                // If the user brought up the dialog, typed a regular
                // expression, and hit return before the typing timeout went
                // off, we need to find. Unfortunately, we don't know if we
                // already did, so the slow typist pays a little extra.
                // FXIME: Should accepting a FormDialog always force the typing
                // timeout? A quick search of the places we're currently using
                // it suggests it would mostly be unwanted. But maybe there's
                // some better pattern we just haven't found yet.
                find();
                formDialog = null;
                return Boolean.TRUE;
            }
        });
        formDialog.setCancelRunnable(new Runnable() {
            public void run() {
                getFindHighlighter().forgetPattern(textToFindIn);
                formDialog = null;
            }
        });
        formDialog.setRememberBounds(false);
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
        
        getFindHighlighter().setPattern(textToFindIn, pattern, findStatus);
    }
    
    private void setStatus(String text, boolean isError) {
        findField.setForeground(isError ? Color.RED : UIManager.getColor("TextField.foreground"));
        findStatus.setText(text);
    }
    
    private void clearStatus() {
        setStatus(" ", false);
    }
}
