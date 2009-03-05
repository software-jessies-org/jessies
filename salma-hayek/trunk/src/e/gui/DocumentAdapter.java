package e.gui;

import javax.swing.event.*;
import javax.swing.text.*;

/**
 * Forwards all DocumentListener methods to a single method, for convenience.
 */
public abstract class DocumentAdapter implements DocumentListener {
    /** From the DocumentListener interface. Override if you like, but see documentChanged. */
    public void changedUpdate(DocumentEvent e) {
        documentChanged();
    }
    
    /** From the DocumentListener interface. Override if you like, but see documentChanged. */
    public void insertUpdate(DocumentEvent e) {
        documentChanged();
    }
    
    /** From the DocumentListener interface. Override if you like, but see documentChanged. */
    public void removeUpdate(DocumentEvent e) {
        documentChanged();
    }
    
    /** Override this. */
    public abstract void documentChanged();
}
