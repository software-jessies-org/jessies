package e.edit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import e.edit.*;
import e.util.*;

/**
A column containing views.

<font color="red">Note that you have you call addComponent and not add. I don't know why I
didn't override add and invoke super.add at the critical point; this seems like a serious design
error that someone should check out and fix. As things stand, accidentally invoking add will
cause a whole can of Whup Ass to be spilt in Edit's lap.</font>

@author Elliott Hughes <enh@acm.org>
*/
public class EColumn extends JPanel implements ComponentListener {
    private static final int MIN_HEIGHT = ETitleBar.TITLE_HEIGHT + 5;
    
    /**
     * Creates a new empty column. We use a null LayoutManager, because
     * we're going to do the layout ourselves.
     */
    public EColumn() {
        setLayout(null);
        addListeners();
    }
    
    public void addListeners() {
        addComponentListener(this);
    }
    
    public void paint(Graphics g) {
        if (getComponentCount() == 0) {
            GraphicsUtilities.paintPaper(g, 240);
        } else {
            super.paintComponents(g);
        }
    }
    
    public void removeComponent(Component c) {
        boolean mustReassignFocus = false;
        if (c instanceof EWindow) {
            EWindow window = (EWindow) c;
            mustReassignFocus = window.getTitleBar().isActive();
            window.windowClosing();
        }
        int which = getComponentIndex(c);
        remove(which);
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
        updateTabForWorkspace();
    }
    
    /** Returns the number of windows that aren't errors windows. */
    public int getNonErrorsWindowCount() {
        int result = 0;
        for (int i = 0; i < getComponentCount(); i++) {
            if (getComponent(i) instanceof EErrorsWindow == false) {
                result++;
            }
        }
        return result;
    }
    
    /**
     * Updates the title of the tab in the JTabbedPane that corresponds to the Workspace that this
     * EColumn represents (if you can follow that). Invoked when the column has a component added
     * or removed.
     */
    public void updateTabForWorkspace() {
        Workspace workspace = (Workspace) SwingUtilities.getAncestorOfClass(Workspace.class, this);
        JTabbedPane tabbedPane = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, this);
        if (workspace != null && tabbedPane != null) {
            String title = workspace.getTitle();
            int windowCount = getNonErrorsWindowCount();
            if (windowCount > 0) {
                title += " (" + windowCount + ")";
            }
            tabbedPane.setTitleAt(tabbedPane.indexOfComponent(workspace), title);
            ensureNonEmptyColumnVisible(tabbedPane);
        }
    }
    
    public void ensureNonEmptyColumnVisible(JTabbedPane tabbedPane) {
        if (getComponentCount() > 0) {
            return;
        }
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Workspace workspace = (Workspace) tabbedPane.getComponentAt(i);
            if (workspace.isEmpty() == false) {
                tabbedPane.setSelectedIndex(i);
            }
        }
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
    
    public void addComponent(Component c) {
        int index = getIndexOfLargestComponent();
        if (index == -1) {
            add(c);
            placeComponent(0, c);
        } else {
            Component largest = getComponent(index);
            int y = largest.getY() + largest.getHeight() / 2;
            add(c, index + 1);
            placeComponent(index + 1, c);
            moveBy(c, y);
        }
        updateTabForWorkspace();
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
    
    public int getIndexOfLargestComponent() {
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
    
    public int getComponentIndex(Component c) {
        for (int i = 0; i < getComponentCount(); i++) {
            if (getComponent(i) == c) {
                return i;
            }
        }
        return -1;
    }
    
    public void moveBy(Component c, int y) {
        int which = getComponentIndex(c);
        moveBy(which, y);
    }
    
    private boolean isThereAnySpaceAboveComponent(int which) {
        for (int i = 0; i < which; i++) {
            if (getComponent(i).getHeight() > MIN_HEIGHT) {
                return true;
            }
        }
        return false;
    }
    
    /** FIXME: should be called moveTo. the 'y' coordinate is an absolute position in the column. */
    public void moveBy(int which, int y) {
        if (which < 1) {
            return;
        }
        
        /* Ensure a window can't be moved off the top or bottom. */
        int newY = Math.min(Math.max(MIN_HEIGHT, y), getHeight() - MIN_HEIGHT);
        
        /* Dramatis personae. */
        Component previous = getComponent(which - 1);
        Component current = getComponent(which);
        Component next = (getComponentCount() > which + 1) ? getComponent(which + 1) : null;
        
        /* What happens to the window above us? */
        int bottomOfPrevious = previous.getY() + previous.getHeight();
        int newPreviousHeight = newY - previous.getY();
        
        /* If it would get squished... */
        if (newPreviousHeight < MIN_HEIGHT) {
            /* FIXME: wrong test. we're really interested in knowing whether there's room. */
            if (isThereAnySpaceAboveComponent(which)) {
                /* ... budge it up a bit ... */
                moveBy(which - 1, newY - MIN_HEIGHT);
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
            if (which < getComponentCount()) {
                /* ... budge the window below us down a bit ... */
                moveBy(which + 1, newY + MIN_HEIGHT);
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
    
    public void componentHidden(ComponentEvent e) { }
    public void componentMoved(ComponentEvent e) { }
    public void componentShown(ComponentEvent e) { }
    
    public void componentResized(ComponentEvent e) {
        relayoutAfterResize();
    }
    
    public void relayoutAfterResize() {
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
    public void setWidthsOfComponents(Component[] components, int width) {
        for (int i = 0; i < components.length; i++) {
            setWidthOfComponent(components[i], width);
        }
    }
    
    /** Sets the width of the given component, if it isn't already that width, and ensures that the change is noticed. */
    public void setWidthOfComponent(Component c, int width) {
        if (c.getWidth() == width) {
            return;
        }
        /* Resize and revalidate. */
        c.setSize(width, c.getHeight());
        c.invalidate();
        c.validate();
    }
}
