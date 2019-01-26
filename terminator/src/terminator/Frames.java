package terminator;

import e.util.*;
import java.awt.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;

/**
 * Ensures that, on Mac OS, we always have our menu bar visible, even
 * when there are no terminal windows open. We use a dummy window with
 * a copy of the menu bar attached. When no other window has the focus,
 * but the application is focused, this hidden window gets the focus.
 *
 * Most of this class should be made obsolete by Desktop.setDefaultMenuBar
 * in Java 9...
 */
public class Frames implements Iterable<TerminatorFrame> {
    private ArrayList<TerminatorFrame> list = new ArrayList<>();
    private JFrame hiddenFrame; // Mac OS X only.
    
    private static final Action[] DOCK_MENU_ACTIONS = new Action[] {
        new TerminatorMenuBar.NewShellAction(),
        new TerminatorMenuBar.NewCommandAction(),
    };
    
    public Frames() {
    }
    
    private synchronized void initHiddenFrameAndDockMenu() {
        if (hiddenFrame == null) {
            // TODO: replace this with Desktop.setDefaultMenuBar when we require Java >= 9.
            String name = "Mac OS Hidden Frame";
            hiddenFrame = new JFrame(name);
            hiddenFrame.setName(name);
            hiddenFrame.setJMenuBar(new TerminatorMenuBar());
            hiddenFrame.setUndecorated(true);
            // Move the window off-screen so that when we're forced to setVisible(true) it doesn't actually disturb the user.
            hiddenFrame.setLocation(new java.awt.Point(-100, -100));
            
            PopupMenu dockMenu = new PopupMenu("Dock Menu");
            addActions(dockMenu, DOCK_MENU_ACTIONS);
            try {
                Class<?> taskbarClass = Class.forName("java.awt.Taskbar");
                Method getTaskbarMethod = taskbarClass.getDeclaredMethod("getTaskbar");
                Method setMenuMethod = taskbarClass.getDeclaredMethod("setMenu", PopupMenu.class);
                setMenuMethod.invoke(getTaskbarMethod.invoke(null), dockMenu);
            } catch (Throwable th) {
                Log.warn("failed to set dock menu", th);
            }
        }
    }
    
    private void addActions(PopupMenu menu, Action[] actions) {
        for (Action action : actions) {
            MenuItem item = new MenuItem(action.getValue(Action.NAME).toString());
            item.addActionListener(action);
            menu.add(item);
        }
    }
    
    private JFrame getHiddenFrame() {
        initHiddenFrameAndDockMenu();
        return hiddenFrame;
    }
    
    public void addFrame(TerminatorFrame frame) {
        list.add(frame);
        if (GuiUtilities.isMacOs()) {
            // Make the hidden frame invisible so that Mac OS won't give it the focus if the user hits C-` or C-~.
            frameStateChanged();
        }
    }
    
    public void removeFrame(TerminatorFrame frame) {
        list.remove(frame);
        if (GuiUtilities.isMacOs()) {
            frameStateChanged();
        }
    }
    
    public void frameStateChanged() {
        if (GuiUtilities.isMacOs()) {
            for (TerminatorFrame frame : list) {
                if (frame.isShowing() && (frame.getExtendedState() & Frame.ICONIFIED) == 0) {
                    getHiddenFrame().setVisible(false);
                    return;
                }
            }
            getHiddenFrame().setVisible(true);
        }
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
     * Implements java.lang.Iterable so we can be used with the new for loop.
     */
    public Iterator<TerminatorFrame> iterator() {
        return list.iterator();
    }
    
    /**
     * Allows convenient cloning of the underlying list.
     * You might think you could use our iterator() with Collections.list(), but you'd be wrong, because it uses Enumeration instead.
     */
    public ArrayList<TerminatorFrame> toArrayList() {
        ArrayList<TerminatorFrame> result = new ArrayList<>();
        result.addAll(list);
        return result;
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
        return getHiddenFrame();
    }
}
