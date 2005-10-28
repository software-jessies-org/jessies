package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import e.util.*;

/**
 * A column containing views.
 * 
 * <font color="red">Note that you have you call addComponent and not add. I don't know why I
 * didn't override add and invoke super.add at the critical point; this seems like a serious design
 * error that someone should check out and fix. As things stand, accidentally invoking add will
 * cause a whole can of Whoop Ass to be spilt in Edit's lap.</font>
 * 
 * @author Elliott Hughes <enh@acm.org>
 */
public class EColumn extends JSplitPane {
    private static final int MIN_HEIGHT = ETitleBar.TITLE_HEIGHT + 5;
    
    private JSplitPane splitPane = new JSplitPane();
    private TextsPanel bottomPanel = new TextsPanel();
    
    /**
     * Creates a new empty column. We use a null LayoutManager, because
     * we're going to do the layout ourselves.
     */
    public EColumn() {
        super(JSplitPane.VERTICAL_SPLIT, true);
        setBottomComponent(bottomPanel);
    }
    
    public void setErrorsWindow(EErrorsWindow errorsWindow) {
        // This, sad to say, is the least difficult way to do what JSplitPane.setDividerLocation should do.
        errorsWindow.setPreferredSize(new Dimension(10, 150));
        
        setTopComponent(errorsWindow);
    }
    
    public void setSelectedWindow(EWindow window) {
        window.requestFocus();
    }
    
    public void addComponent(EWindow c, int y) {
        bottomPanel.addComponent(c, y);
        updateTabForWorkspace();
    }
    
    public void removeComponent(EWindow c, boolean mustReassignFocus) {
        bottomPanel.removeComponent(c, mustReassignFocus);
        updateTabForWorkspace();
    }
    
    /**
     * Updates the title of the tab in the JTabbedPane that corresponds to the Workspace that this
     * EColumn represents (if you can follow that). Invoked when the column has a component added
     * or removed.
     */
    public void updateTabForWorkspace() {
        Workspace workspace = getWorkspace();
        JTabbedPane tabbedPane = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, this);
        if (workspace != null && tabbedPane != null) {
            String title = workspace.getTitle();
            int windowCount = getTextWindows().length;
            if (windowCount > 0) {
                title += " (" + windowCount + ")";
            }
            tabbedPane.setTitleAt(tabbedPane.indexOfComponent(workspace), title);
        }
    }
    
    private Workspace getWorkspace() {
        return (Workspace) SwingUtilities.getAncestorOfClass(Workspace.class, this);
    }
    
    public void expandComponent(Component luckyBoy) {
        bottomPanel.expandComponent(luckyBoy);
    }
    
    public void moveTo(Component c, int y) {
        bottomPanel.moveTo(c, y);
    }
    
    public void cycleWindow(EWindow window, int indexDelta) {
        bottomPanel.cycleWindow(window, indexDelta);
    }
    
    public ETextWindow[] getTextWindows() {
        ArrayList<ETextWindow> result = new ArrayList<ETextWindow>();
        for (Component c : bottomPanel.getComponents()) {
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
    
    private class TextsPanel extends JPanel {
        private TextsPanel() {
            setLayout(null);
            addListeners();
        }
        
        private void addListeners() {
            addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    relayoutAfterResize();
                }
            });
        }
        
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
                moveTo(titleBar.getParent(), SwingUtilities.convertMouseEvent(titleBar, e, bottomPanel).getY() - pointerOffset);
            }
        }
        
        private final TitleBarMouseInputListener titleBarMouseInputListener = new TitleBarMouseInputListener();
        
        public void addComponent(final EWindow c, final int y) {
            if (y == -1) {
                addComponentHeuristically(c);
            } else {
                add(c);
                moveTo(c, y);
            }
            addListenersTo(c);
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
                try {
                    moveTo(c, y);
                } catch (StackOverflowError ex) {
                    Log.warn("the total height of the title bars is too large for the parent window", ex);
                }
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
}
