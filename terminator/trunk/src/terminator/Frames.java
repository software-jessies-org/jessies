package terminator;

import e.util.*;
import java.util.*;
import javax.swing.*;

/**
 * Ensures that, on Mac OS, we always have our menu bar visible, even
 * when there are no terminal windows open. We use a dummy window with
 * a copy of the menu bar attached. When no other window has the focus,
 * but the application is focused, this hidden window gets the focus,
 * and its menu is used for the screen menu bar.
 */
public class Frames {
    private ArrayList<TerminatorFrame> list = new ArrayList<TerminatorFrame>();
    private JFrame hiddenFrame; // Mac OS X only.
    
    public Frames() {
        if (GuiUtilities.isMacOs()) {
            hiddenFrame = new JFrame("Mac OS implementation detail");
            hiddenFrame.setJMenuBar(new TerminatorMenuBar());
            hiddenFrame.setUndecorated(true);
        }
    }
    
    public void add(TerminatorFrame frame) {
        list.add(frame);
        if (GuiUtilities.isMacOs()) {
            // Make the hidden frame invisible so that Mac OS won't give it the
            // focus if the user hits C-` or C-~.
            hiddenFrame.setVisible(false);
        }
    }
    
    public void remove(TerminatorFrame frame) {
        if (GuiUtilities.isMacOs() && size() == 1) {
            hiddenFrame.setVisible(true);
        }
        list.remove(frame);
    }
    
    public TerminatorFrame get(int i) {
        return list.get(i);
    }
    
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    public int size() {
        return list.size();
    }
    
    /**
     * Returns a JFrame with the application menu bar, or null. Useful on
     * Mac OS for ensuring that a form gets a suitable parent frame so that
     * it can inherit the screen menu bar.
     */
    public JFrame getFrame() {
        if (list.isEmpty() == false) {
            return list.get(0);
        }
        return hiddenFrame;
    }
}
