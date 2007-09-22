package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * A column containing views.
 * 
 * Note that you must call addComponent and not add, because our second parameter isn't an index or an abstract location, it's a y-coordinate.
 */
public class EColumn extends JPanel {
    private static final int MIN_HEIGHT = ETitleBar.TITLE_HEIGHT + 5;
    
    /**
     * Creates a new empty column.
     */
    public EColumn() {
        // We use a null LayoutManager, because we're going to do the layout ourselves.
        setLayout(null);
        addListeners();
        initPopUpMenu();
    }
    
    private void initPopUpMenu() {
        EPopupMenu backgroundMenu = new EPopupMenu(this);
        backgroundMenu.addMenuItemProvider(new MenuItemProvider() {
            public void provideMenuItems(MouseEvent e, Collection<Action> actions) {
                actions.add(new OpenQuicklyAction());
                actions.add(new FindFilesContainingSelectionAction());
                actions.add(null);
                actions.add(new CheckInChangesAction());
            }
        });
    }
    
    public void setSelectedWindow(EWindow window) {
        window.requestFocus();
    }
    
    private Workspace getWorkspace() {
        return (Workspace) SwingUtilities.getAncestorOfClass(Workspace.class, this);
    }
    
    public ETextWindow[] getTextWindows() {
        ArrayList<ETextWindow> result = new ArrayList<ETextWindow>();
        for (Component c : getComponents()) {
            if (c instanceof ETextWindow) {
                result.add(ETextWindow.class.cast(c));
            }
        }
        return result.toArray(new ETextWindow[result.size()]);
    }
    
    public EWindow findWindowByName(String name) {
        name = name.toLowerCase();
        for (ETextWindow text : getTextWindows()) {
            if (text.getTitle().toLowerCase().endsWith(name)) {
                return text;
            }
        }
        return null;
    }
    
    private void addListeners() {
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                relayoutAfterResize();
            }
        });
    }
    
    @Override
    public void paint(Graphics g) {
        if (getComponentCount() == 0) {
            GraphicsUtilities.paintPaper(g, 240);
        } else {
            super.paint(g);
        }
    }
    
    public void removeComponent(EWindow c, boolean mustReassignFocus) {
        int which = getComponentIndex(c);
        remove(which);
        removeListenersFrom(c);
        Log.warn("which="+which+";getComponentCount()="+getComponentCount());
        if (which < getComponentCount()) {
            Log.warn("free space to component below ("+which+")");
            /* Give free space to component below the one removed. */
            Component luckyBoy = getComponent(which);
            luckyBoy.requestFocus();
            luckyBoy.setSize(luckyBoy.getWidth(), luckyBoy.getHeight() + c.getHeight());
            luckyBoy.setLocation(luckyBoy.getX(), c.getY());
            luckyBoy.invalidate();
            luckyBoy.validate();
        } else if (which > 0) {
            Log.warn("free space to component above ("+(which-1)+")");
            /* Give free space to component above the one removed. */
            Component luckyBoy = getComponent(which - 1);
            luckyBoy.requestFocus();
            luckyBoy.setSize(luckyBoy.getWidth(), c.getY() + c.getHeight() - luckyBoy.getY());
            luckyBoy.invalidate();
            luckyBoy.validate();
        } else if (getComponentCount() > 0) {
            Log.warn("free space to topmost component");
            /* Give free space to the topmost component. */
            Component luckyBoy = getComponent(0);
            luckyBoy.requestFocus();
            luckyBoy.setLocation(0, 0);
            luckyBoy.setSize(luckyBoy.getWidth(), luckyBoy.getHeight() + c.getHeight());
            luckyBoy.invalidate();
            luckyBoy.validate();
        } else {
            Log.warn("empty column");
            /* Redraw the now empty column. */
            getParent().repaint();
        }
        getParent().repaint();
        getWorkspace().updateTabForWorkspace();
    }
    
    private class TitleBarMouseInputListener extends MouseInputAdapter {
        private int pointerOffset;
        
        /**
         * Keeps track of how far down the title bar the mouse button is depressed. This allows
         * us to position the title bar correctly when we move it in the mouseDragged method.
         */
        public void mousePressed(MouseEvent e) {
            pointerOffset = e.getY();
        }
        
        /** Causes the title bar to track the mouse's movements. */
        public void mouseDragged(MouseEvent e) {
            Component titleBar = (Component) e.getSource();
            moveTo(titleBar.getParent(), SwingUtilities.convertMouseEvent(titleBar, e, EColumn.this).getY() - pointerOffset);
        }
    }
    
    private final TitleBarMouseInputListener titleBarMouseInputListener = new TitleBarMouseInputListener();
    
    public void addComponent(final EWindow c, final int y) {
        try {
            if (y == -1) {
                addComponentHeuristically(c);
            } else {
                add(c);
                c.setBounds(0, y, getWidth(), getHeight() - y);
                moveTo(c, y);
            }
        } catch (StackOverflowError ex) {
            Log.warn("the total height of the title bars is too large for the parent window"); // Don't show the stack trace because it's *very* long, and uninformative.
        }
        addListenersTo(c);
        getWorkspace().updateTabForWorkspace();
    }
    
    private void addComponentHeuristically(final EWindow c) {
        int index = getIndexOfLargestComponent();
        if (index == -1) {
            add(c);
            placeComponent(0, c);
        } else {
            Component largest = getComponent(index);
            int y = largest.getY() + largest.getHeight() / 2;
            add(c, index + 1);
            placeComponent(index + 1, c);
            moveTo(c, y);
        }
    }
    
    private void addListenersTo(EWindow window) {
        window.getTitleBar().addMouseListener(titleBarMouseInputListener);
        window.getTitleBar().addMouseMotionListener(titleBarMouseInputListener);
    }
    
    private void removeListenersFrom(EWindow window) {
        window.getTitleBar().removeMouseListener(titleBarMouseInputListener);
        window.getTitleBar().removeMouseMotionListener(titleBarMouseInputListener);
    }
    
    private void placeComponent(int index, Component c) {
        if (index == 0) {
            c.setBounds(0, 0, getWidth(), getHeight());
            c.invalidate();
            c.validate();
        } else {
            Component last = getComponent(index - 1);
            int y = last.getY() + last.getHeight()/2;
            last.setBounds(0, last.getY(), getWidth(), last.getHeight()/2);
            c.setBounds(0, y, getWidth(), getHeight() - y);
            last.invalidate();
            c.invalidate();
            c.validate();
            last.validate();
        }
    }
    
    private int getIndexOfLargestComponent() {
        int largest = -1;
        int currentLargestHeight = -1;
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c.getHeight() > currentLargestHeight) {
                currentLargestHeight = c.getHeight();
                largest = i;
            }
        }
        return largest;
    }
    
    private int getComponentIndex(Component c) {
        for (int i = 0; i < getComponentCount(); i++) {
            if (getComponent(i) == c) {
                return i;
            }
        }
        return -1;
    }
    
    public void cycleWindow(EWindow c, int indexDelta) {
        int index = getComponentIndex(c);
        if (index == -1) {
            return;
        }
        int newIndex = (index + indexDelta) % getComponentCount();
        if (newIndex == -1) {
            newIndex = getComponentCount() - 1;
        }
        getComponent(newIndex).requestFocus();
    }
    
    private boolean isThereAnySpaceAboveComponent(int which) {
        for (int i = 0; i < which; i++) {
            if (getComponent(i).getHeight() > MIN_HEIGHT) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isValidComponentIndex(int which) {
        return which < getComponentCount();
    }
    
    public void expandComponent(Component luckyBoy) {
        int freeSpace = getHeight() - (MIN_HEIGHT * (getComponentCount() - 1));
        int y = 0;
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            int height = (c == luckyBoy) ? freeSpace : MIN_HEIGHT;
            c.setBounds(0, y, c.getWidth(), height);
            y += height;
            c.invalidate();
            c.validate();
        }
    }
    
    /** Moves the given component to the given absolute Y position in the column. */
    public void moveTo(Component c, int y) {
        int which = getComponentIndex(c);
        if (which < 1) {
            return;
        }
        
        /* Ensure a window can't be moved off the top or bottom. */
        int newY = Math.min(Math.max(MIN_HEIGHT, y), getHeight() - MIN_HEIGHT);
        
        /* Dramatis personae. */
        Component previous = getComponent(which - 1);
        Component current = getComponent(which);
        Component next = isValidComponentIndex(which + 1) ? getComponent(which + 1) : null;
        
        /* What happens to the window above us? */
        int bottomOfPrevious = previous.getY() + previous.getHeight();
        int newPreviousHeight = newY - previous.getY();
        
        /* If it would get squished... */
        if (newPreviousHeight < MIN_HEIGHT) {
            /* FIXME: wrong test. we're really interested in knowing whether there's room. */
            if (isThereAnySpaceAboveComponent(which)) {
                /* ... budge it up a bit ... */
                moveTo(previous, newY - MIN_HEIGHT);
                newPreviousHeight = newY - previous.getY();
            } else {
                /* ... or refuse to squish it. */
                newY = previous.getY() + MIN_HEIGHT;
                newPreviousHeight = MIN_HEIGHT;
            }
        }
        
        /* What happens to us? */
        int newHeight = (next != null) ? next.getY() - newY : getHeight() - newY;
        
        /* If we would get squished... */
        if (newHeight < MIN_HEIGHT) {
            if (next != null) {
                /* ... budge the window below us down a bit ... */
                moveTo(next, newY + MIN_HEIGHT);
                newHeight = next.getY() - newY;
            } else {
                /** ... or refuse to be squished. */
                //FIXME: implement this!
            }
        }
        
        /* Let things take their course... */
        previous.setBounds(previous.getX(), previous.getY(), previous.getWidth(), newPreviousHeight);
        previous.invalidate();
        previous.validate();
        
        current.setBounds(current.getX(), newY, current.getWidth(), newHeight);
        current.invalidate();
        current.validate();
    }
    
    private void relayoutAfterResize() {
        final Component[] components = getComponents();
        if (components.length == 0) {
            return;
        }
        
        setWidthsOfComponents(components, getWidth());
        
        /* FIXME: this assumes that the resize actually increased the column's height. */
        Component last = components[components.length - 1];
        int lastBottom = last.getY() + last.getHeight();
        if (lastBottom != getHeight()) {
            last.setSize(getWidth(), getHeight() - last.getY());
            last.invalidate();
            last.validate();
        }
    }
    
    /** Ensures all the components in the column have the given width. */
    private void setWidthsOfComponents(Component[] components, int width) {
        for (Component component : components) {
            setWidthOfComponent(component, width);
        }
    }
    
    /** Sets the width of the given component, if it isn't already that width, and ensures that the change is noticed. */
    private void setWidthOfComponent(Component c, int width) {
        if (c.getWidth() == width) {
            return;
        }
        /* Resize and revalidate. */
        c.setSize(width, c.getHeight());
        c.invalidate();
        c.validate();
    }
}
