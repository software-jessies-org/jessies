package e.gui;

import java.awt.event.*;
import javax.swing.*;

/**
 * A simple status bar. Messages time-out after a certain delay.
 */
public class EStatusBar extends JLabel {
    /** Tells us when to clear the status bar. */
    private Timer statusBarClearingTimer;
    
    /** How many milliseconds to wait before clearing the status bar. */
    private static final int STATUS_BAR_CLEAR_DELAY = 20 * 1000;
    
    /**
     * Constructs a status bar with no initial message.
     */
    public EStatusBar() {
        super(" ");
        
        // Use the slimmer menu font on Linux.
        if (System.getProperty("os.name").contains("Linux")) {
            setFont(UIManager.getFont("Menu.font"));
        }
        
        initStatusBarClearingTimer();
    }
    
    /** Initializes the timer. */
    private void initStatusBarClearingTimer() {
        statusBarClearingTimer = new Timer(STATUS_BAR_CLEAR_DELAY, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearStatusBar();
            }
        });
        statusBarClearingTimer.setRepeats(false);
    }
    
    /**
     * Sets the text to display in the status bar.
     */
    public void setText(String status) {
        // Restarts the timer that will cause this message to be hidden after a reasonable delay.
        // We have to check that the timer's been created because our superconstructor invokes
        // setText. This is why some people argue that a class' implementation should never
        // make use of its overrideable public API.
        if (statusBarClearingTimer != null) {
            statusBarClearingTimer.restart();
        }
        
        // Works around a Swing misfeature whereby a JLabel showing the empty string is zero-height,
        // which is likely to mess up your layout. We substitute a single space for the empty string.
        if (status.length() == 0) {
            status = " ";
        }
        
        super.setText(status);
    }
    
    /** Clears the status bar. */
    public void clearStatusBar() {
        super.setText(" ");
    }
}
