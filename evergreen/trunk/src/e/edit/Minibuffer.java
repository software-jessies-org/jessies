package e.edit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import e.util.*;

public class Minibuffer extends JPanel implements FocusListener {
    private MinibufferUser minibufferUser;
    private Component previousFocusOwner;
    
    private Timer typingTimer;
    
    private JLabel prompt;
    private JTextField textField;
    
    private StringHistory history;
    private int historyIndex;
    private String itemAfterHistory;
    
    public Minibuffer() {
        super(new BorderLayout());
        
        // It's important that the prompt not be the empty string, because then the minibuffer would be zero-height.
        prompt = new JLabel(" ");
        textField = new JTextField("");
        
        typingTimer = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                notifyMinibufferUserOfTyping();
            }
        });
        typingTimer.setRepeats(false);
        
        addListeners();
        deactivate();
    }
    
    public void showComponents() {
        add(prompt, BorderLayout.WEST);
        add(textField, BorderLayout.CENTER);
        prompt.setEnabled(true);
        textField.setEnabled(true);
        textField.setEditable(true);
    }

    public void hideComponents() {
        prompt.setEnabled(false);
        textField.setEnabled(false);
        textField.setEditable(false);
        remove(prompt);
        remove(textField);
    }

    private void notifyMinibufferUserOfTyping() {
        if (minibufferUser != null) {
            String newValue = textField.getText();
            minibufferUser.valueChangedTo(newValue);
            Color color = minibufferUser.isValid(newValue) ? Color.BLACK : Color.RED;
            textField.setForeground(color);
        }
    }
    
    private void addListeners() {
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                /* Doesn't happen, because we're not a StyledDocument. */
            }
            public void insertUpdate(DocumentEvent e) {
                textChanged();
            }
            public void removeUpdate(DocumentEvent e) {
                textChanged();
            }
            public void textChanged() {
                typingTimer.restart();
            }
        });
        textField.addFocusListener(this);
        textField.addKeyListener(new KeyAdapter() {
            public void keyReleased(final KeyEvent e) {
                if (e.isConsumed()) {
                    return;
                }
                if (e.getModifiers() == 0) {
                    boolean shouldHide = handleEnterAndEsc(e);
                    if (shouldHide) {
                        deactivate();
                    }
                } else if ((e.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0) {
                    final KeyStroke thisKeyStroke = KeyStroke.getKeyStrokeForEvent(e);
                    KeyStroke currentMinibufferUserKeyStroke = (KeyStroke) ((AbstractAction) minibufferUser).getValue(AbstractAction.ACCELERATOR_KEY);
                    if (currentMinibufferUserKeyStroke.getKeyCode() == thisKeyStroke.getKeyCode() && currentMinibufferUserKeyStroke.getKeyChar() == thisKeyStroke.getKeyChar() && currentMinibufferUserKeyStroke.getModifiers() == thisKeyStroke.getModifiers()) {
                        // We're already showing, so there's nothing to do.
                        // This may look like a premature optimization, but we actually receive the initial KeyEvent that caused us to show.
                        // Not returning early would cause us to be canceled before we'd shown.
                        e.consume();
                        return;
                    }
                    
                    typingTimer.stop();
                    notifyMinibufferUserOfTyping();
                    
                    // FIXME: check first if the keystroke is actually bound to something and do nothing if it's not?
                    addToHistory();
                    deactivate();
                    
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            EvergreenMenuBar menuBar = (EvergreenMenuBar) Evergreen.getInstance().getFrame().getJMenuBar();
                            menuBar.performActionForKeyStroke(thisKeyStroke);
                        }
                    });
                }
            }
        });
    }
    
    private boolean handleEnterAndEsc(KeyEvent e) {
        boolean shouldHide = false;
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_ESCAPE) {
            e.consume();
            minibufferUser.wasCanceled();
            shouldHide = true;
        } else if (keyCode == KeyEvent.VK_ENTER) {
            e.consume();
            notifyMinibufferUserOfTyping();
            String text = textField.getText();
            shouldHide = (minibufferUser.isValid(text) && minibufferUser.wasAccepted(text));
            if (shouldHide) {
                addToHistory();
            }
        } else if (keyCode == KeyEvent.VK_UP) {
            e.consume();
            traverseHistory(-1);
        } else if (keyCode == KeyEvent.VK_DOWN) {
            e.consume();
            traverseHistory(1);
        }
        return shouldHide;
    }
    
    private void addToHistory() {
        String text = textField.getText();
        if (history != null) {
            history.add(text);
        }
    }

    private void traverseHistory(int direction) {
        if (history == null) {
            return;
        }
        int newHistoryIndex = historyIndex + direction;
        if (newHistoryIndex < 0 || newHistoryIndex > history.size()) {
            return;
        }
        historyIndex = newHistoryIndex;
        textField.setText((historyIndex == history.size()) ? itemAfterHistory : history.get(historyIndex));
    }
        
    public void activate(MinibufferUser newUser) {
        if (minibufferUser != null) {
            deactivate();
        }
        
        previousFocusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        
        showComponents();
        
        minibufferUser = newUser;
        prompt.setText(minibufferUser.getPrompt() + ": ");
        textField.setText(minibufferUser.getInitialValue());
        textField.selectAll();
        textField.requestFocusInWindow();
        
        history = minibufferUser.getHistory();
        if (history == null) {
            historyIndex = 0;
        } else {
            if (history.size() > 0 && history.get(history.getLatestHistoryIndex()).equals(textField.getText())) {
                itemAfterHistory = "";
                historyIndex = history.getLatestHistoryIndex();
            } else {
                itemAfterHistory = textField.getText();
                historyIndex = history.getLatestHistoryIndex() + 1;
            }
        }
        
        // Rather than force the MinibufferUser to have a special case,
        // we notify them that the value has 'changed' to the value they
        // suggested...
        notifyMinibufferUserOfTyping();
    }
    
    public void deactivate() {
        typingTimer.stop();
        minibufferUser = null;
        
        prompt.setText(" ");
        textField.setText(" ");
        
        hideComponents();
        
        if (previousFocusOwner != null) {
            previousFocusOwner.requestFocusInWindow();
            previousFocusOwner = null;
        }
    }
    
    //
    // FocusListener interface.
    //
    
    public void focusGained(FocusEvent e) {
    }
    
    /**
     * Deactivates the minibuffer if the focus is lost to another window. This prevents us from presenting a confusing
     * UI where it's not obvious whether the focus is in a text or in the minibuffer.
     */
    public void focusLost(FocusEvent e) {
        deactivate();
    }
}
