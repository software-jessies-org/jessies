package terminatorn;

import java.awt.*;
import javax.swing.*;

/**
 * Adds a border to a JTextBuffer (insulating it from having to fiddle its
 * co-ordinates all the time, as Swing expects it to do), and implements
 * Scrollable on its behalf, so the scroll bar buttons (and the scroll wheel,
 * if you have one) behave sensibly.
 */
public class BorderPanel extends JPanel implements Scrollable {
    private JComponent interior;
    private final int internalBorder;

    public BorderPanel(JComponent interior) {
        super(new BorderLayout());
        this.interior = interior;
        add(interior, BorderLayout.CENTER);
        setBackground(interior.getBackground());
        internalBorder = Options.getSharedInstance().getInternalBorder();
        setBorder(new javax.swing.border.EmptyBorder(internalBorder, internalBorder, internalBorder, internalBorder));
    }

    public Dimension getPreferredScrollableViewportSize() {
        Dimension interiorSize = interior.getPreferredSize();
        Dimension size = new Dimension(interiorSize.width + 2 * internalBorder, interiorSize.height + 2 * internalBorder);
        return size;
    }
    
    public int getScrollableUnitIncrement(Rectangle visibleRectangle, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return visibleRectangle.height / 10;
        } else {
            return visibleRectangle.width / 10;
        }
    }
    
    public int getScrollableBlockIncrement(Rectangle visibleRectangle, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return visibleRectangle.height;
        } else {
            return visibleRectangle.width;
        }
    }
    
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }
    
    public boolean getScrollableTracksViewportHeight() {
        return false; // We want a vertical scroll-bar.
    }
}
