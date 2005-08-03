package e.ptextarea;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class PFind {
    public static final String HIGHLIGHTER_NAME = "PFind";
    
    /**
     * Used to mark the matches in the text as if they'd been gone over with a highlighter pen. We use
     * full yellow with half-alpha so you can see the selection through, as a dirty smudge, just like a real
     * highlighter pen might do.
     */
    public static class MatchHighlight extends PColoredHighlight {
        private static final Color MATCH_COLOR = new Color(255, 255, 0, 128);
        
        public MatchHighlight(PTextArea textArea, int startIndex, int endIndex) {
            super(textArea, startIndex, endIndex, MATCH_COLOR);
        }
        
        public String getHighlighterName() {
            return HIGHLIGHTER_NAME;
        }
    }
    
    /**
     * Offers a default find dialog.
     */
    public static class FindAction extends PTextAction {
        private JTextField findField = new JTextField(40);
        private JLabel findStatus = new JLabel(" ");
        private PTextArea textArea;
        
        private List<PTextAction> actions = new ArrayList<PTextAction>();
        
        public FindAction() {
            super("Find...", e.util.GuiUtilities.makeKeyStroke("F", false));
            initAction(PActionFactory.makeFindNextAction());
            initAction(PActionFactory.makeFindPreviousAction());
        }
        
        private void initAction(PTextAction action) {
            actions.add(action);
            ComponentUtilities.initKeyBinding(findField, action);
        }
        
        private void rebindActions() {
            for (PTextAction action : actions) {
                action.bindTo(textArea);
            }
        }
        
        public void performOn(PTextArea textArea) {
            this.textArea = textArea;
            initFindField();
            rebindActions();
            showFindDialog();
        }
        
        private void initFindField() {
            String selection = textArea.getSelectedText();
            if (selection.length() > 0) {
                findField.setText(StringUtilities.regularExpressionFromLiteral(selection));
            }
        }
        
        private void showFindDialog() {
            Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, textArea);
            
            FormBuilder form = new FormBuilder(frame, "Find");
            FormPanel formPanel = form.getFormPanel();
            formPanel.addRow("Find:", findField);
            formPanel.setStatusBar(findStatus);
            formPanel.setTypingTimeoutActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        findField.setForeground(UIManager.getColor("TextField.foreground"));
                        updateFindResults();
                    } catch (java.util.regex.PatternSyntaxException ex) {
                        findField.setForeground(Color.RED);
                        findStatus.setText(ex.getDescription());
                    }
                }
            });
            form.getFormDialog().setCancelRunnable(new Runnable() {
                public void run() {
                    textArea.clearFindMatches();
                }
            });
            form.showNonModal();
            
            findField.selectAll();
            findField.requestFocus();
            findStatus.setText(" ");
        }
        
        private void updateFindResults() {
            int matchCount = textArea.findAllMatches(findField.getText());
            findStatus.setText("Matches: " + matchCount);
        }
    }
}
