package e.gui;

import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;

/**
 * A "Window" menu for a Mac OS application. All your application has to do
 * is tell this class whenever it opens a new window, and add a JMenu returned
 * by "makeJMenu" to each window's JMenuBar. We do the rest.
 * 
 * http://developer.apple.com/technotes/tn/tn2042.html#Section2_2
 * 
 * FIXME: should use all the icons mentioned in the UI guidelines.
 */
public class WindowMenu {
    private static final WindowMenu INSTANCE = new WindowMenu();
    
    private final WindowCloseListener windowCloseListener = new WindowCloseListener();
    private final WindowTitleListener windowTitleListener = new WindowTitleListener();
    
    private Vector windows;
    
    private WindowMenu() {
        windows = new Vector();
    }
    
    /**
     * Returns this application's handle on the window menu system.
     */
    public static WindowMenu getSharedInstance() {
        return INSTANCE;
    }
    
    /**
     * Returns a new JMenu to be added to a window's JMenuBar. This menu will
     * automatically be updated whenever a window is created or destroyed, or
     * if a window's title changes.
     */
    public JMenu makeJMenu() {
        UpdatableJMenu menu = new UpdatableJMenu();
        updateMenu(menu);
        return menu;
    }
    
    /**
     * Makes us aware of the creation of a new window. We attach listeners to
     * be notified directly of events other than creation.
     */
    public void addWindow(Frame f) {
        f.addPropertyChangeListener("title", windowTitleListener);
        f.addWindowListener(windowCloseListener);
        windows.add(f);
        updateMenus();
    }
    
    private class WindowCloseListener extends WindowAdapter {
        public void windowClosed(WindowEvent e) {
            removeWindow((Frame) e.getWindow());
        }
    }
    
    private class WindowTitleListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            updateMenus();
        }
    }
    
    private void removeWindow(Frame f) {
        windows.remove(f);
        updateMenus();
    }
    
    private static Frame getFocusedFrame() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return (Frame) SwingUtilities.getAncestorOfClass(Frame.class, focusOwner);
    }
    
    private static class ShowSpecificWindowAction extends AbstractAction {
        private Frame frame;
        
        public ShowSpecificWindowAction(Frame frame) {
            super(frame.getTitle());
            this.frame = frame;
        }
        
        public void actionPerformed(ActionEvent e) {
            frame.toFront();
        }
    }
    
    private static class UpdatableJMenu extends JMenu {
        public UpdatableJMenu() {
            super("Window");
            InstanceTracker.addInstance(this);
        }
        
        public void addWindowItem(final Frame f) {
            add(new ShowSpecificWindowAction(f));
        }
        
        public void disableAll() {
            for (int i = 0; i < getMenuComponentCount(); ++i) {
                JMenuItem item = getItem(i);
                if (item != null) {
                    item.setEnabled(false);
                }
            }
        }
    }
    
    private void updateMenus() {
        Object[] menus = InstanceTracker.getInstancesOfClass(UpdatableJMenu.class);
        for (int i = 0; i < menus.length; ++i) {
            updateMenu((UpdatableJMenu) menus[i]);
        }
    }
    
    private void updateMenu(UpdatableJMenu menu) {
        menu.removeAll();
        addStandardItemsTo(menu);
        
        if (windows.size() == 0) {
            menu.disableAll();
            return;
        }
        
        addWindowItemsTo(menu);
    }
    
    private void addWindowItemsTo(UpdatableJMenu menu) {
        menu.add(new JSeparator());
        for (int i = 0; i < windows.size(); ++i) {
            Frame f = (Frame) windows.get(i);
            menu.addWindowItem(f);
        }
    }
    
    private void addStandardItemsTo(JMenu menu) {
        menu.add(new JMenuItem(new MinimizeAction()));
        menu.add(new JMenuItem(new ZoomAction()));
        
        // FIXME: need some way for Terminator to add this:
        //menu.add(new JSeparator());
        //menu.add(new JMenuItem(new ReturnToDefaultSizeAction()));
        
        menu.add(new JSeparator());
        menu.add(new JMenuItem(new BringAllToFrontAction()));
    }
    
    private static class MinimizeAction extends AbstractAction {
        public MinimizeAction() {
            super("Minimize");
            putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke("M", false));
        }
        
        public void actionPerformed(ActionEvent e) {
            Frame window = getFocusedFrame();
            if (window != null) {
                window.setExtendedState(window.getExtendedState() | Frame.ICONIFIED);
            }
        }
    }
    
    private static class ZoomAction extends AbstractAction {
        public ZoomAction() {
            super("Zoom");
        }
        
        public void actionPerformed(ActionEvent e) {
            Frame window = getFocusedFrame();
            if (window != null) {
                window.setExtendedState(window.getExtendedState() | Frame.MAXIMIZED_BOTH);
            }
        }
    }
    
    private class BringAllToFrontAction extends AbstractAction {
        public BringAllToFrontAction() {
            super("Bring All To Front");
        }
        
        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < windows.size(); ++i) {
                Frame f = (Frame) windows.get(i);
                f.toFront();
            }
        }
    }
}
