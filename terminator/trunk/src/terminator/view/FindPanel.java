package terminator.view;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import javax.swing.*;
import terminator.*;
import terminator.view.highlight.*;

public class FindPanel extends JPanel implements FindStatusDisplay {
    private static JWindow statusWindow;
    
    private final JTerminalPane terminalPane;
    
    private final JTextField findField = new JTextField();
    private final ELabel findStatus = new ELabel();
    
    private final Timer typingTimer;
    
    private final TerminatorMenuBar.BindableAction findNextAction = new TerminatorMenuBar.FindNextAction();
    private final TerminatorMenuBar.BindableAction findPreviousAction = new TerminatorMenuBar.FindPreviousAction();
    
    public FindPanel(JTerminalPane terminalPane) {
        this.terminalPane = terminalPane;
        
        this.typingTimer = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateResults();
            }
        });
        typingTimer.setRepeats(false);
        findField.getDocument().addDocumentListener(new DocumentAdapter() {
            public void documentChanged() {
                typingTimer.restart();
            }
        });
        
        initKeyBindings();
        
        setLayout(new BorderLayout());
        add(findField, BorderLayout.CENTER);
        
        findStatus.setFont(UIManager.getFont("ToolTip.font"));
        findStatus.setBackground(UIManager.getColor("ToolTip.background"));
        findStatus.setForeground(UIManager.getColor("ToolTip.foreground"));
    }
    
    private void initKeyBindings() {
        findNextAction.bindTo(terminalPane);
        ComponentUtilities.initKeyBinding(findField, findNextAction);
        
        findPreviousAction.bindTo(terminalPane);
        ComponentUtilities.initKeyBinding(findField, findPreviousAction);
        
        ComponentUtilities.initKeyBinding(findField, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), new EnterAction());
        ComponentUtilities.initKeyBinding(findField, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), new EscapeAction());
    }
    
    private class EnterAction extends AbstractAction {
        public EnterAction() {
            super("EnterAction");
        }
        
        public void actionPerformed(ActionEvent e) {
            // Enter hides the find panel but leaves the search active.
            hideFindPanel();
        }
    }
    
    private class EscapeAction extends AbstractAction {
        public EscapeAction() {
            super("EscapeAction");
        }
        
        public void actionPerformed(ActionEvent e) {
            // Escape hides the find panel and cancels the search.
            getFindHighlighter().forgetPattern(terminalPane.getTerminalView());
            hideFindPanel();
        }
    }
    
    private void hideFindPanel() {
        hideStatusWindow();
        terminalPane.requestFocus();
        setVisible(false);
    }
    
    private void showStatusWindow() {
        if (statusWindow == null) {
            final JFrame owner = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this);
            statusWindow = new JWindow(owner);
            statusWindow.setBackground(UIManager.getColor("TextField.background"));
        }
        statusWindow.setContentPane(findStatus);
        statusWindow.pack();
        
        final int x = findField.getWidth() - statusWindow.getWidth() - findField.getInsets().right;
        final int y = (findField.getHeight() - statusWindow.getHeight()) / 2;
        final Point location = new Point(x, y);
        SwingUtilities.convertPointToScreen(location, findField);
        statusWindow.setLocation(location);
        statusWindow.setVisible(true);
    }
    
    private void hideStatusWindow() {
        if (statusWindow != null) {
            statusWindow.setVisible(false);
            statusWindow.dispose();
        }
    }
    
    private void updateResults() {
        getFindHighlighter().setPattern(terminalPane.getTerminalView(), findField.getText(), this);
    }
    
    private FindHighlighter getFindHighlighter() {
        return terminalPane.getTerminalView().getHighlighterOfClass(FindHighlighter.class);
    }
    
    public void showFindPanel() {
        final String selection = terminalPane.getSelectionHighlighter().getTabbedString();
        if (selection.length() > 0) {
            findField.setText("(?-i)" + StringUtilities.regularExpressionFromLiteral(selection));
        }
        updateResults();
        findField.selectAll();
        setVisible(true);
        findField.requestFocus();
        showStatusWindow();
    }
    
    public void setStatus(String text, boolean isError) {
        findField.setForeground(isError ? Color.RED : UIManager.getColor("TextField.foreground"));
        findStatus.setText(text);
        
        if (text.length() == 0) {
            hideStatusWindow();
        } else {
            showStatusWindow();
        }
    }
}
