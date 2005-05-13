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
    
    private JTextField findField = new JTextField(40);
    private JLabel findStatus = new JLabel(" ");
    private JTextBuffer textToFindIn;
    
    private FindDialog() {
        FormDialog.markAsMonitoredField(findField);
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
        formPanel.setTypingTimeoutActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
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
        });
        FormDialog.showNonModal(frame, "Find", formPanel);
        
        findField.selectAll();
        findField.requestFocus();
        findStatus.setText(" ");
    }
    
    private FindHighlighter getFindHighlighter() {
        return (FindHighlighter) textToFindIn.getHighlighterOfClass(FindHighlighter.class);
    }
    
    /*
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
                            textToFindIn.findPrevious(FindHighlighter.class);
                        } else if (e.getKeyCode() == KeyEvent.VK_G) {
                            textToFindIn.findNext(FindHighlighter.class);
                        }
                    }
                }
            });
    */
}
