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
 * Code calling this should probably be guarded by a GuiUtilities.isMacOs() test.
 * Linux and Windows offer their own system-wide "minimize" and "zoom" equivalents.
 * Neither has an equivalent of "bring all to front", and users don't expect it.
 * 
 * FIXME: should use all the icons mentioned in the UI guidelines.
 */
public class WindowMenu {
    private static final WindowMenu INSTANCE = new WindowMenu();
    
    private final WindowEventListener windowEventListener = new WindowEventListener();
    private final WindowTitleListener windowTitleListener = new WindowTitleListener();
    
    private final ArrayList<Frame> windows = new ArrayList<Frame>();
    
    private WindowMenu() {
    }
    
    /**
     * Returns this application's WindowMenu.
     */
    public static WindowMenu getSharedInstance() {
        return INSTANCE;
    }
    
    /**
     * Returns a new JMenu to be added to a window's JMenuBar. This menu will
     * automatically be updated whenever a window is created or destroyed, or
     * if a window's title changes.
     * 
     * You can supply custom items for the menu by providing a non-null, non-empty
     * array of Action instances as customItems.
     */
    public JMenu makeJMenu(Action[] customItems) {
        UpdatableJMenu menu = new UpdatableJMenu(customItems);
        menu.updateMenu(getFrames());
        return menu;
    }
    
    /**
     * Makes us aware of the creation of a new window. We attach listeners to
     * be notified directly of events other than creation.
     */
    public void addWindow(Frame f) {
        f.addPropertyChangeListener("title", windowTitleListener);
        f.addWindowListener(windowEventListener);
        f.addWindowFocusListener(windowEventListener);
        windows.add(f);
        updateMenus();
    }
    
    private class WindowEventListener extends WindowAdapter {
        public void windowClosed(WindowEvent e) {
            removeWindow((Frame) e.getWindow());
        }
        
        public void windowGainedFocus(WindowEvent e) {
            updateMenus();
        }
    }
    
    private class WindowTitleListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            updateMenus();
        }
    }
    
    private void removeWindow(Frame f) {
        f.removePropertyChangeListener("title", windowTitleListener);
        f.removeWindowListener(windowEventListener);
        f.removeWindowFocusListener(windowEventListener);
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
    
    private class UpdatableJMenu extends JMenu {
        private Action[] customItems;
        
        public UpdatableJMenu(Action[] customItems) {
            super("Window");
            this.customItems = customItems;
            InstanceTracker.addInstance(this);
        }
        
        public void updateMenu(Frame[] windows) {
            removeAll();
            addStandardItems();
            if (windows.length == 0) {
                disableAll();
            } else {
                addWindowItems(windows);
            }
        }
        
        private void addStandardItems() {
            add(new MinimizeAction());
            add(new ZoomAction());
            
            addCustomItems();
            
            add(new JSeparator());
            add(new BringAllToFrontAction());
        }
        
        private void addCustomItems() {
            if (customItems == null || customItems.length == 0) {
                return;
            }
            
            add(new JSeparator());
            for (int i = 0; i < customItems.length; ++i) {
                add(customItems[i]);
            }
        }
        
        private void addWindowItems(Frame[] windows) {
            add(new JSeparator());
            for (int i = 0; i < windows.length; ++i) {
                addWindowItem(windows[i]);
            }
        }
        
        private void addWindowItem(final Frame f) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(new ShowSpecificWindowAction(f));
            item.setSelected(f.isFocused());
            add(item);
        }
        
        private void disableAll() {
            for (int i = 0; i < getMenuComponentCount(); ++i) {
                JMenuItem item = getItem(i);
                if (item != null) {
                    item.setEnabled(false);
                }
            }
        }
    }
    
    private void updateMenus() {
        Frame[] frames = null;
        for (UpdatableJMenu menu : InstanceTracker.getInstancesOfClass(UpdatableJMenu.class)) {
            if (frames == null) {
                frames = getFrames();
            }
            menu.updateMenu(frames);
        }
    }
    
    private Frame[] getFrames() {
        return windows.toArray(new Frame[windows.size()]);
    }
    
    private void bringAllToFront() {
        Frame[] frames = getFrames();
        for (int i = 0; i < frames.length; ++i) {
            frames[i].toFront();
        }
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
        
        @Override
        public boolean isEnabled() {
            return (getFocusedFrame() != null);
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
        
        @Override
        public boolean isEnabled() {
            return (getFocusedFrame() != null);
        }
    }
    
    private class BringAllToFrontAction extends AbstractAction {
        public BringAllToFrontAction() {
            super("Bring All To Front");
        }
        
        public void actionPerformed(ActionEvent e) {
            bringAllToFront();
        }
        
        @Override
        public boolean isEnabled() {
            return (windows.isEmpty() == false);
        }
    }
}
