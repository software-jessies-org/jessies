package e.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class ETextField extends JTextField implements DocumentListener {
    public ETextField() {
        this(null, null, 0);
    }

    public ETextField(String text) {
        this(null, text, 0);
    }

    public ETextField(int columns) {
        this(null, null, columns);
    }

    public ETextField(String text, int columns) {
        this(null, text, columns);
    }
    
    public ETextField(Document doc, String text, int columns) {
        super(doc, text, columns);
        getCaret().setBlinkRate(0);
        addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                JDialog dialog = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, ETextField.this);
                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_ESCAPE) {
                    dialog.setVisible(false);
                    e.consume();
                }
            }
        });
        getDocument().addDocumentListener(this);
        setCaretColor(Color.RED);
    }
    
    /** From the DocumentListener interface. Override if you like, but see textChanged. */
    public void changedUpdate(DocumentEvent e) { /* Doesn't happen. */ }
    
    /** From the DocumentListener interface. Override if you like, but see textChanged. */
    public void insertUpdate(DocumentEvent e) { textChanged(); }
    
    /** From the DocumentListener interface. Override if you like, but see textChanged. */
    public void removeUpdate(DocumentEvent e) { textChanged(); }
    
    /**
     * Invoked whenever the text changes. By default, any text changes are routed to
     * this method. If you need to distinguish between inserts and removals, for
     * example, override the DocumentListener methods instead.
     */
    public void textChanged() {
        /* Do nothing. */
    }
}
