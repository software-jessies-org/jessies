package e.edit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

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
            public void keyReleased(KeyEvent e) {
                boolean shouldHide = false;
                if (e.getModifiers() == 0) {
                    shouldHide = handleEnterAndEsc(e);
                } else if ((e.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0) {
                    // This is horrid, but I don't think it's possible to do the right thing.
                    // If you look at the API Component and JComponent provide, you'll
                    // see that the likely candidates for injecting KeyEvents are protected,
                    // and the Swing utility class (KeyboardManager) for handling this
                    // kind of thing is likewise inaccessible. You can't just get the list of
                    // KeyListener objects, because that's not how key bindings are handled.
                    // You might think you could getActionForKeyStroke and fake up a
                    // suitable KeyStroke using KeyStroke.getKeyStrokeForEvent, and you'd
                    // be right, but you'd find that text actions work by getting the focused
                    // component and invoking a method on that, and we the minibuffer are
                    // focused, so that's not going to help forward the event, is it?
                    typingTimer.stop();
                    notifyMinibufferUserOfTyping();
                    shouldHide = minibufferUser.interpretSpecialKeystroke(e);
                }
                if (shouldHide) {
                    deactivate();
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
            shouldHide = minibufferUser.wasAccepted(text);
            if (shouldHide && history != null && text.length() != 0) {
                history.add(text);
            }
        } else if (keyCode == KeyEvent.VK_UP) {
            traverseHistory(-1);
        } else if (keyCode == KeyEvent.VK_DOWN) {
            traverseHistory(1);
        }
        return shouldHide;
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
            if (history.get(history.getLatestHistoryIndex()).equals(textField.getText())) {
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
            // FIXME: why doesn't this look like it's worked? If you hit a key or wiggle the mouse,
            // you can see that it *has* actually transferred the focus, but nothing seems to have
            // noticed the fact. The following line is Ed's workaround:
            ((Window) SwingUtilities.getAncestorOfClass(Window.class, previousFocusOwner)).repaint();
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
