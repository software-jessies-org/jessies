package e.gui;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class EMonitoredTextField extends JTextField {
    private Timer timer;
    
    public EMonitoredTextField() {
        this(0);
    }
    
    public EMonitoredTextField(int columns) {
        super(null, null, columns);
        getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { textChanged(); }
            public void insertUpdate(DocumentEvent e) { textChanged(); }
            public void removeUpdate(DocumentEvent e) { textChanged(); }
        });
        timer = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                timerExpired();
            }
        });
        timer.setRepeats(false);
    }

    /** Override this to do something. */
    public void timerExpired() {
    }

    /** Restarts the timer every time the text changes. */
    public void textChanged() {
        timer.restart();
    }
}
