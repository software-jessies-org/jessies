package e.ptextarea;

import e.forms.*;
import e.gui.*;
import java.awt.*;
import java.awt.event.*;
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
        private FindField findField = new FindField();
        private JLabel findStatus = new JLabel(" ");
        private PTextArea textArea;
        
        public FindAction() {
            super("Find...", e.util.GuiUtilities.makeKeyStroke("F", false));
            PTextArea.initKeyBinding(findField, PActionFactory.makeFindNextAction());
            PTextArea.initKeyBinding(findField, PActionFactory.makeFindPreviousAction());
        }
        
        public void performOn(PTextArea textArea) {
            this.textArea = textArea;
            showFindDialog();
        }
        
        private void showFindDialog() {
            Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, textArea);
            
            FormPanel formPanel = new FormPanel();
            formPanel.addRow("Find:", findField);
            formPanel.setStatusBar(findStatus);
            FormDialog.showNonModal(frame, "Find", formPanel);
            
            findField.selectAll();
            findField.requestFocus();
            findStatus.setText(" ");
            
            findField.find();
        }
        
        private void updateFindResults() {
            int matchCount = textArea.findAllMatches(findField.getText());
            findStatus.setText("Matches: " + matchCount);
        }
        
        private class FindField extends EMonitoredTextField {
            public FindField() {
                super(40);
                addKeyListener(new KeyAdapter() {
                    public void keyTyped(KeyEvent e) {
                        if (e.getKeyChar() == '\n') {
                            find();
                            e.consume();
                        }
                    }
                    
                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                            // FIXME: cancelFind();
                        }
                    }
                });
            }
            
            public void timerExpired() {
                find();
            }
            
            public void find() {
                try {
                    setForeground(UIManager.getColor("TextField.foreground"));
                    updateFindResults();
                } catch (java.util.regex.PatternSyntaxException ex) {
                    setForeground(Color.RED);
                    findStatus.setText(ex.getDescription());
                }
            }
        }
    }
}
