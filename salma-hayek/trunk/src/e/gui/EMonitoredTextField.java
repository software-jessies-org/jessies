package e.gui;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public abstract class EMonitoredTextField extends JTextField {
    private Timer timer;
    private boolean callerIsReady = false;
    
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
    
    /** Called by FormDialog when it's ready. */
    public void startMonitoring() {
        // All of our users want to be called soon after the dialog is displayed
        // to populate the match list from the initial field contents.
        timerExpired();
        callerIsReady = true;
    }
    
    /** Called by FormDialog when it's done. */
    public void stopMonitoring() {
        callerIsReady = false;
        timer.stop();
    }

    /** Override this to do something. */
    public abstract void timerExpired();

    /** Restarts the timer every time the text changes. */
    public void textChanged() {
        if (callerIsReady) {
            timer.restart();
        }
    }
}
