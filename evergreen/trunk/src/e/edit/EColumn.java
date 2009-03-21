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
        super(null);
        addListeners();
        initPopUpMenu();
    }
    
    private void initPopUpMenu() {
        EPopupMenu backgroundMenu = new EPopupMenu(this);
        backgroundMenu.addMenuItemProvider(new MenuItemProvider() {
            public void provideMenuItems(MouseEvent e, Collection<Action> actions) {
                actions.add(new OpenQuicklyAction());
                actions.add(new FindInFilesAction());
                actions.add(null);
                actions.add(new CheckInChangesAction());
            }
        });
    }
    
    public void setSelectedWindow(EWindow window) {
        window.requestFocus();
    }
    
    public Collection<ETextWindow> getTextWindows() {
        ArrayList<ETextWindow> result = new ArrayList<ETextWindow>();
        for (Component c : getComponents()) {
            if (c instanceof ETextWindow) {
                result.add((ETextWindow) c);
            }
        }
        return result;
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
            @Override
            public void componentResized(ComponentEvent e) {
                relayoutAfterResize();
            }
        });
    }
    
    /**
     * Paints the background using a paper effect similar to that in early Mac OS X. The
     * first line in the pattern is brightest, followed by one medium gray, one dark
     * gray, another medium then repeat until fade. There's no kind of ripple to the
     * lines. We're not going out of our way to be slow, so we stick to horizontal
     * line drawing only!
     */
    @Override public void paint(Graphics g) {
        if (getComponentCount() > 0) {
            super.paint(g);
            return;
        }
        
        // FIXME: Is this worth the bother? Who ever sees this? Is there a platform where it's a significant improvement?
        final int grayStep = 10; // Ed had 5, but 10 is more like Mac OS X, which was our model.
        final int brightestGray = 240;
        final Rectangle clipRect = g.getClipBounds();
        final int startY = clipRect.y / 4 * 4 - 4;
        for (int y = startY; y < clipRect.y + clipRect.height; y+=4) {
            g.setColor(new Color(brightestGray, brightestGray, brightestGray));
            g.drawLine(clipRect.x, y, clipRect.x + clipRect.width, y);
            
            int middleGray = brightestGray - grayStep;
            g.setColor(new Color(middleGray, middleGray, middleGray));
            g.drawLine(clipRect.x, y + 1, clipRect.x + clipRect.width, y + 1);
            g.drawLine(clipRect.x, y + 3, clipRect.x + clipRect.width, y + 3);
            
            int darkestGray = middleGray - grayStep;
            g.setColor(new Color(darkestGray, darkestGray, darkestGray));
            g.drawLine(clipRect.x, y + 2, clipRect.x + clipRect.width, y + 2);
        }
    }
    
    public void removeComponent(EWindow c, boolean mustReassignFocus) {
        final int which = getComponentIndex(c);
        remove(which);
        removeListenersFrom(c);
        if (which < getComponentCount()) {
            // Give free space to component below the one removed.
            final Component luckyBoy = getComponent(which);
            luckyBoy.requestFocus();
            reshapeAndRevalidate(luckyBoy, luckyBoy.getX(), c.getY(), luckyBoy.getWidth(), luckyBoy.getHeight() + c.getHeight());
        } else if (which > 0) {
            // Give free space to component above the one removed.
            final Component luckyBoy = getComponent(which - 1);
            luckyBoy.requestFocus();
            reshapeAndRevalidate(luckyBoy, luckyBoy.getX(), luckyBoy.getY(), luckyBoy.getWidth(), c.getY() + c.getHeight() - luckyBoy.getY());
        } else if (getComponentCount() > 0) {
            // Give free space to the topmost component.
            final Component luckyBoy = getComponent(0);
            luckyBoy.requestFocus();
            reshapeAndRevalidate(luckyBoy, 0, 0, luckyBoy.getWidth(), luckyBoy.getHeight() + c.getHeight());
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
            reshapeAndRevalidate(c, 0, 0, getWidth(), getHeight());
        } else {
            final Component last = getComponent(index - 1);
            final int y = last.getY() + last.getHeight()/2;
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
        for (int i = 0; i < getComponentCount(); ++i) {
            final Component c = getComponent(i);
            final int height = (c == luckyBoy) ? freeSpace : MIN_HEIGHT;
            reshapeAndRevalidate(c, 0, y, c.getWidth(), height);
            y += height;
        }
    }
    
    /**
     * Moves the given component to the given absolute Y position in the column.
     * Returns true on success, false on failure.
     */
    private boolean moveTo(Component c, int y) {
        final int which = getComponentIndex(c);
        if (which < 1) {
            // You can't move the top-most window in a column.
            return false;
        }
        
        // Dramatis personae.
        final Component previous = getComponent(which - 1);
        final Component current = getComponent(which);
        final Component next = isValidComponentIndex(which + 1) ? getComponent(which + 1) : null;
        int newY = y;
        
        // What happens to the window above us?
        int newPreviousHeight = newY - previous.getY();
        // If it would get squished...
        if (newPreviousHeight < MIN_HEIGHT) {
            // FIXME: wrong test. we're really interested in knowing whether there's room.
            if (isThereAnySpaceAboveComponent(which)) {
                // ...budge it up a bit if possible.
                if (!moveTo(previous, newY - MIN_HEIGHT)) {
                    return false;
                }
                newPreviousHeight = newY - previous.getY();
            } else {
                // ...or refuse to squish it.
                return false;
            }
        }
        
        // What happens to us?
        int newHeight = (next != null) ? next.getY() - newY : getHeight() - newY;
        // If we would get squished...
        if (newHeight < MIN_HEIGHT) {
            if (next != null) {
                // ...budge the window below us down a bit if possible.
                if (!moveTo(next, newY + MIN_HEIGHT)) {
                    return false;
                }
                newHeight = next.getY() - newY;
            } else {
                // ... or refuse to be squished.
                return false;
            }
        }
        
        // Make the decided-upon changes to the window above...
        reshapeAndRevalidate(previous, previous.getX(), previous.getY(), previous.getWidth(), newPreviousHeight);
        // ...and to this window.
        reshapeAndRevalidate(current, current.getX(), newY, current.getWidth(), newHeight);
        return true;
    }
    
    private void relayoutAfterResize() {
        if (getComponentCount() == 0) {
            return;
        }
        
        // Ensure all the components in the column have the same width.
        final Component[] components = getComponents();
        for (int i = 0; i < components.length - 1; ++i) {
            Component c = components[i];
            if (c.getWidth() != getWidth()) {
                reshapeAndRevalidate(c, c.getX(), c.getY(), getWidth(), c.getHeight());
            }
        }
        
        // FIXME: this assumes that the resize actually increased the column's height.
        final Component last = components[components.length - 1];
        final int lastBottom = last.getY() + last.getHeight();
        if (lastBottom != getHeight()) {
            reshapeAndRevalidate(last, last.getX(), last.getY(), getWidth(), getHeight() - last.getY());
        }
    }
    
    private static void reshapeAndRevalidate(Component c, int newX, int newY, int newWidth, int newHeight) {
        c.setBounds(newX, newY, newWidth, newHeight);
        c.invalidate();
        c.validate();
    }
}
