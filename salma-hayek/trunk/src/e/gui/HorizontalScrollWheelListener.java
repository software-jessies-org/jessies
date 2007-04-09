package e.gui;

import java.awt.event.*;
import javax.swing.*;

/**
 * Supports Mac-style horizontal scrolling using the scroll wheel with shift held down.
 * This isn't quite as clever as the real JScrollPane wheel scrolling code, but it's good enough (and actually delegates much of the work to Swing).
 * This should be obsoleted by any fix to Sun bug 6440198.
 */
public class HorizontalScrollWheelListener implements MouseWheelListener {
    public static final HorizontalScrollWheelListener INSTANCE = new HorizontalScrollWheelListener();
    
    private HorizontalScrollWheelListener() {
    }
    
    public void mouseWheelMoved(MouseWheelEvent e) {
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, e.getComponent());
        if (scrollPane == null) {
            return;
        }
        
        if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
            if (e.getWheelRotation() == 0) {
                return;
            }
            
            JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
            if (scrollBar == null || scrollBar.isVisible() == false) {
                return;
            }
            
            int direction = Integer.signum(e.getWheelRotation());
            if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                // Java 5: BasicScrollBarUI.scrollByUnits(scrollBar, direction, units);
                // Java 6: BasicScrollBarUI.scrollByUnits(scrollBar, direction, units, limitScroll);
                if (System.getProperty("java.vm.version").startsWith("1.5.")) {
                    invokeBasicScrollBarUIMethod("scrollByUnits", new Class[] { JScrollBar.class, int.class, int.class }, scrollBar, direction, Math.abs(e.getUnitsToScroll()));
                } else {
                    invokeBasicScrollBarUIMethod("scrollByUnits", new Class[] { JScrollBar.class, int.class, int.class, boolean.class }, scrollBar, direction, Math.abs(e.getUnitsToScroll()), (Math.abs(e.getWheelRotation()) == 1));
                }
            } else if (e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
                //BasicScrollBarUI.scrollByBlock(scrollBar, direction);
                invokeBasicScrollBarUIMethod("scrollByBlock", new Class[] { JScrollBar.class, int.class }, scrollBar, direction);
            }
        } else {
            // Probably a vertical scroll wheel event, so let the enclosing scroll pane deal with it.
            scrollPane.dispatchEvent(e);
        }
    }
    
    private void invokeBasicScrollBarUIMethod(String methodName, Class[] parameterTypes, Object... arguments) {
        try {
            java.lang.reflect.Method method = javax.swing.plaf.basic.BasicScrollBarUI.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            method.invoke(null, arguments);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
