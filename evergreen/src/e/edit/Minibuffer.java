package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Minibuffer extends JPanel implements FocusListener {
    private final ELabel prompt = new ELabel();
    private final JTextField textField = new JTextField("");
    private final Timer typingTimer;
    
    private MinibufferUser minibufferUser;
    private Component previousFocusOwner;
    
    private StringHistory history;
    private int historyIndex;
    private String itemAfterHistory;
    
    public Minibuffer() {
        super(new BorderLayout());
        
        typingTimer = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                notifyMinibufferUserOfTyping();
            }
        });
        typingTimer.setRepeats(false);
        
        addListeners();
        deactivate(false);
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
    
    @SuppressWarnings("deprecation") // https://bugs.openjdk.java.net/browse/JDK-8183518
    private void addListeners() {
        textField.getDocument().addDocumentListener(new DocumentAdapter() {
            public void documentChanged() {
                typingTimer.restart();
            }
        });
        textField.addFocusListener(this);
        textField.addKeyListener(new KeyAdapter() {
            public void keyPressed(final KeyEvent e) {
                if (e.isConsumed()) {
                    return;
                }
                if (e.getModifiers() == 0) {
                    boolean shouldHide = handleEnterAndEsc(e);
                    if (shouldHide) {
                        deactivate(true);
                    }
                } else if ((e.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0) {
                    // Ensure everything's up to date.
                    typingTimer.stop();
                    notifyMinibufferUserOfTyping();
                    
                    final KeyStroke thisKeyStroke = KeyStroke.getKeyStrokeForEvent(e);
                    
                    // Check whether the text field would like the keystroke.
                    Object mapKey = textField.getInputMap().get(thisKeyStroke);
                    if (mapKey != null) {
                        Action action = textField.getActionMap().get(mapKey);
                        if (action != null) {
                            // Let the text field have it.
                            return;
                        }
                    }
                    
                    // Ignore the initial press of alt/control/command.
                    if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
                        return;
                    }
                    
                    // Cancel the minibuffer and see if the menu bar has an action for us.
                    addToHistory();
                    deactivate(true);
                    GuiUtilities.invokeLater(() -> {
                        EvergreenMenuBar menuBar = (EvergreenMenuBar) Evergreen.getInstance().getFrame().getJMenuBar();
                        // FIXME: PTextAction.isEnabled often returns false, disabling Find Next and Find Previous, because the focus transition initiated by deactivate, above, hasn't completed yet.
                        menuBar.performActionForKeyStroke(thisKeyStroke, e);
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
            deactivate(false);
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
    
    public void deactivate(boolean restorePreviousFocus) {
        typingTimer.stop();
        minibufferUser = null;
        
        prompt.setText("");
        textField.setText("");
        
        hideComponents();
        
        // We only restore the previous focus owner when we're closing ourselves (due to the user hitting
        // Escape, for example). If we lose focus (eg because the user clicked somewhere to give it focus),
        // we must not restore focus, as the user just deliberately gave focus to someone else.
        // However, it's important to restore the previous focus owner in other cases, as otherwise when
        // the minibuffer disappears focus is simply given to the first component in the parent window
        // (which will be the topmost ETextArea), which is weird and confusing.
        if (previousFocusOwner != null && restorePreviousFocus) {
            previousFocusOwner.requestFocusInWindow();
        }
        previousFocusOwner = null;
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
        // Deactivate _without_ restoring the previous focus owner, in the case where we
        // lose focus due to, for example, the user clicking in another PTextArea. Otherwise
        // we'll be effectively overriding what the user wanted to do.
        deactivate(false);
    }
}
