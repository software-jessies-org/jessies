package e.ptextarea;

import e.forms.*;
import e.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class PFind {
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
    }
    
    /**
     * Used to implement find next/previous (by searching for the find match
     * highlights).
     */
    public static class MatchHighlightMatcher implements PHighlightMatcher {
        private boolean forwards;
        private int selectionStart;
        private int selectionEnd; 
        
        public MatchHighlightMatcher(boolean forwards, PTextArea textArea) {
            this.forwards = forwards;
            this.selectionStart = textArea.getSelectionStart();
            this.selectionEnd = textArea.getSelectionEnd();
        }
        
        public boolean matches(PHighlight highlight) {
            if (highlight instanceof MatchHighlight == false) {
                return false;
            }
            final int minOffset = Math.min(highlight.getStartIndex(), highlight.getEndIndex());
            final int maxOffset = Math.max(highlight.getStartIndex(), highlight.getEndIndex());
            if (forwards) {
                return minOffset > selectionEnd;
            } else {
                return maxOffset < selectionStart;
            }
        }
    }
    
    /**
     * Offers a default find dialog.
     */
    public static class FindAction extends PActionFactory.PTextAction {
        private JTextField findField = new JTextField(40);
        private JLabel findStatus = new JLabel(" ");
        private PTextArea textArea;
        
        private List<PActionFactory.PTextAction> actions = new ArrayList<PActionFactory.PTextAction>();
        
        public FindAction() {
            super("Find...", e.util.GuiUtilities.makeKeyStroke("F", false));
            initAction(PActionFactory.makeFindNextAction());
            initAction(PActionFactory.makeFindPreviousAction());
        }
        
        private void initAction(PActionFactory.PTextAction action) {
            actions.add(action);
            PTextArea.initKeyBinding(findField, action);
        }
        
        private void rebindActions() {
            for (PActionFactory.PTextAction action : actions) {
                action.bindTo(textArea);
            }
        }
        
        public void performOn(PTextArea textArea) {
            this.textArea = textArea;
            rebindActions();
            showFindDialog();
        }
        
        private void showFindDialog() {
            Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, textArea);
            
            FormPanel formPanel = new FormPanel();
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
            FormDialog.showNonModal(frame, "Find", formPanel);
            
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
