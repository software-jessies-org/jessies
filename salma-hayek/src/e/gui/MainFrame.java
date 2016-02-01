package e.gui;

import e.util.*;
import javax.swing.*;

/**
 * Handles the simple case of an application with a single main window.
 * It's surprisingly awkward to ensure that native resources are freed up in
 * all cases, and if they're not, the VM won't exit (which means that users
 * who try to quit will find your application hanging around on their system,
 * invisible but still running, which may cause future instances of your
 * application trouble if they run a server, say).
 * 
 * This seems to be the simplest implementation, even if it is rather ugly.
 * 
 * This class also does various things to make your application look more
 * native: ensuring it's sensibly sized and positioned, using the application's
 * icon, and so forth.
 */
public class MainFrame extends JFrame {
    public MainFrame() {
        this("");
    }
    
    public MainFrame(String title) {
        super(title);
        // EXIT_ON_CLOSE is simple and effective, but it's unfriendly, especially if you become embedded.
        // DISPOSE_ON_CLOSE is tempting, but doesn't cover setVisible(false).
        // HIDE_ON_CLOSE and then manually doing a dispose in setVisible covers everything.
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }
    
    @Override
    public void setVisible(boolean newVisibility) {
        if (newVisibility) {
            setLocationRelativeTo(null);
            JFrameUtilities.constrainToScreen(this);
            JFrameUtilities.setFrameIcon(this);
            super.setVisible(true);
            GuiUtilities.finishGnomeStartup();
        } else {
            super.setVisible(false);
            dispose();
        }
    }
}
