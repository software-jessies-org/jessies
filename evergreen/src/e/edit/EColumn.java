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
    private static final int MIN_HEIGHT = ETitleBar.TITLE_HEIGHT + 6;
    
    // recentlyClosedFiles contains a limited list of the most recently closed files, including
    // their 'address' (location of caret/selection).
    // This allows us to reopen recently-closed files at the same place the user was looking at
    // when the file was closed.
    private LinkedList<String> recentlyClosedFiles = new LinkedList<>();
    
    // [philjessies, 2024-05-05] While we don't generally like limits, once you've got to about
    // 10 recently closed files, scanning the list of them to find a file you want to edit again
    // becomes more cumbersome than using 'open quickly'.
    // However, the number 10 is a bit arbitrary; we should change it if it turns out to be wrong.
    private static final int MAX_RECENTLY_CLOSED_FILES = 10;
    
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
        ArrayList<ETextWindow> result = new ArrayList<>();
        for (Component c : getComponents()) {
            if (c instanceof ETextWindow) {
                result.add((ETextWindow) c);
            }
        }
        return result;
    }
    
    public EWindow findWindowByFilename(String filename) {
        for (ETextWindow text : getTextWindows()) {
            if (text.getFilename().equals(filename)) {
                return text;
            }
        }
        return null;
    }
    
    // Returns the list of recently-closed files in this EColumn, most recent first.
    // This is a copy of the internal structure, so the caller can modify it as it pleases.
    public ArrayList<String> getRecentlyClosedFiles() {
        ArrayList<String> result = new ArrayList<>();
        for (String item : recentlyClosedFiles) {
            result.add(item);
        }
        return result;
    }
    
    private void addRecentlyClosedFile(ETextWindow window) {
        removeRecentlyClosedFile(window);
        recentlyClosedFiles.addFirst(window.getFilename() + window.getAddress());
        if (recentlyClosedFiles.size() > MAX_RECENTLY_CLOSED_FILES) {
            recentlyClosedFiles.removeLast();
        }
    }
    
    private void removeRecentlyClosedFile(ETextWindow window) {
        // This is O(n), which is perfectly fine as we limit the list length anyway.
        // Note that recentlyClosedFiles is a list of addresses, which include the
        // caret position. This may not match up with the current ETextWindow's caret
        // position, so we need to use a predicate to ensure we just check the filename
        // portion.
        recentlyClosedFiles.removeIf(addr -> addr.startsWith(window.getFilename() + ":"));
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
        // [philjessies, 2024-05-05] The Workspace should remember which windows were closed,
        // so we can provide Chrome-like ctrl+shift+t "reopen last closed tab" functionality.
        if (c instanceof ETextWindow) {
            addRecentlyClosedFile((ETextWindow) c);
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
        if (c instanceof ETextWindow) {
            removeRecentlyClosedFile((ETextWindow) c);
        }
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
    
    public boolean canMoveWindow(EWindow c, int indexDelta) {
        int index = getComponentIndex(c);
        if (index == -1) {
            return false;
        }
        int newIndex = index + indexDelta;
        return newIndex > -1 && newIndex < getComponentCount();
    }
        
    public void moveWindow(EWindow c, int indexDelta) {
        int index = getComponentIndex(c);
        if (index == -1) {
            return;
        }
        int newIndex = index + indexDelta;
        // Don't do anything if we hit the top or bottom. This is not consistent with the
        // behaviour of the cycleWindow, as that will cycle between bottom and top, but
        // it seems to me like it's going to be more useful to allow the user to just hold
        // down the relevant hotkey and have the window zoom to the top or bottom and stay
        // there, than flipping windows between top and bottom.
        // We may want to revisit this if that turns out not to be the case. If we do, then
        // we'll need more complex logic to move components around, as while moving only
        // up or down one place involves just switching two windows' positions, flipping
        // between top and bottom moves everything.
        if (newIndex == -1 || newIndex == getComponentCount()) {
            return;
        }
        // First of all, switch the Y position of the two components.
        Component me = getComponent(index);
        Component them = getComponent(newIndex);
        Rectangle myBounds = me.getBounds();
        Rectangle theirBounds = them.getBounds();
        
        int topY = Math.min(me.getY(), them.getY());
        // Note: these look reversed, as the top and bottom we _want_ are the top and bottom
        // _final_ positions, which are currently in the opposite order to how they will be.
        Component top = getComponent(Math.max(index, newIndex));
        Component bottom = getComponent(Math.min(index, newIndex));
        Rectangle topRect = top.getBounds();
        Rectangle bottomRect = bottom.getBounds();
        topRect.y = topY;
        // The bottom rectangle's top Y position is going to change, as when we swap the windows
        // we keep their respective heights the same as they were. The divider's position
        // therefore changes.
        bottomRect.y = topY + topRect.height;
        
        // Now switch the components in the container's order. Unfortunately we can't do this
        // by just swapping the two things in the list - we have to remove a component and
        // re-add it, which is probably going to copy the latter part of the list twice.
        // That said, the list is short, so who cares?
        remove(index);
        add(me, newIndex);
        // Finally, set the bounds.
        top.setBounds(topRect);
        bottom.setBounds(bottomRect);
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
        for (Component c : components) {
            if (c.getWidth() != getWidth()) {
                reshapeAndRevalidate(c, c.getX(), c.getY(), getWidth(), c.getHeight());
            }
        }
        
        // Ensure the vertical space is being used properly. When height is being increased,
        // it's given to the lowest component that isn't minimised (or, if they're all minimised,
        // the top component). When height is being decreased, it's decreased from the lowest
        // component, and the components are squished together if they start falling off the
        // bottom of the screen.
        // Note that we set the availableHeight to be either the overall component height, or
        // the minimum height required to fit all the components in their squished form.
        // This allows us to simplify the logic in the loop a fair bit.
        int availableHeight = Math.max(getHeight(), components.length * MIN_HEIGHT);
        for (int i = components.length - 1; i >= 0; i--) {
            final Component comp = components[i];
            int compBottom = comp.getY() + comp.getHeight();
            if (compBottom == availableHeight) {
                // Already in the right position - nothing more to do, and the components
                // above this one don't need to change.
                break;
            }
            if (availableHeight > compBottom) {
                // If we're minimised but *not* the topmost component, then just move with
                // the bottom of the window, and allow the extra height to be given another
                // component (one that the user hasn't squished against the bottom, and probably
                // isn't interested in right now).
                if (i > 0 && comp.getHeight() == MIN_HEIGHT) {
                    availableHeight -= MIN_HEIGHT;
                    reshapeAndRevalidate(comp, comp.getX(), availableHeight, getWidth(), MIN_HEIGHT);
                    continue;
                }
                // We're not minimised, so expand this component to claim all the space.
                // This will leave this component with its minY unchanged, so we can exit
                // early at this point.
                reshapeAndRevalidate(comp, comp.getX(), comp.getY(), getWidth(), availableHeight - comp.getY());
                break;
            }
            // The window has shrunk.
            if (availableHeight >= comp.getY() + MIN_HEIGHT) {
                // This component can shrink to account for the window shrinkage just by itself,
                // so do that and exit early - no other components need move or be resized.
                reshapeAndRevalidate(comp, comp.getX(), comp.getY(), getWidth(), availableHeight - comp.getY());
                break;
            }
            // The window has shrunk, and effectively minimised this component. Ensure we minimise
            // properly, and check if we can squish up into the next component above.
            // As we started out with availableHeight being *at least* big enough to fit all the
            // components in minimised form, we know that there's no possibility of us moving
            // upwards too far, as all windows have at least enough room to be in minimised form.
            availableHeight -= MIN_HEIGHT;
            if (availableHeight != comp.getY() || MIN_HEIGHT != comp.getHeight()) {
                reshapeAndRevalidate(comp, comp.getX(), availableHeight, getWidth(), MIN_HEIGHT);
            }
        }
    }
    
    private static void reshapeAndRevalidate(Component c, int newX, int newY, int newWidth, int newHeight) {
        c.setBounds(newX, newY, newWidth, newHeight);
        c.invalidate();
        c.validate();
    }
}
