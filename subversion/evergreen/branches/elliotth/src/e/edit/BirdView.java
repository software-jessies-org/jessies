package e.edit;

import java.awt.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;

/**
A bird's-eye view of the document. Intended to sit alongside the vertical scrollbar.
The Find dialog informs us of matches, and we display little red ticks corresponding
to their locations.
*/
public class BirdView extends JComponent {
    private ETextArea textArea;

    private javax.swing.plaf.ScrollBarUI ui;
    private Method method;

    private BitSet matchingLines = new BitSet();

    public BirdView(ETextArea textArea, JScrollBar scrollBar) {
        this.textArea = textArea;
        this.ui = (javax.swing.plaf.ScrollBarUI) scrollBar.getUI();
        try {
            method = javax.swing.plaf.basic.BasicScrollBarUI.class.getDeclaredMethod("getTrackBounds", new Class[] {});
            method.setAccessible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(4, Integer.MAX_VALUE);
    }

    public void paintComponent(Graphics g) {
        g.setColor(SystemColor.control); //UIManager.getColor("Scrollbar.track"));
        g.fillRect(0, 0, getWidth(), getHeight());

        Rectangle usableArea = null;
        usableArea = new Rectangle(0, 0, getWidth() - 1, getHeight() - 1);
        usableArea.y += 10;
        usableArea.height -= 42;
        
        if (usableArea == null) {
            return;
        }

        if (false) {
            /* When testing, it's nice to see where we think the rectangle is. */
            g.setColor(Color.GRAY);
            g.drawRect(usableArea.x, usableArea.y, usableArea.width, usableArea.height);
        }

        int linesInDocument = textArea.getLineCount();
        double scaleFactor = (double) usableArea.height / (double) linesInDocument;

        g.setColor(Color.BLACK);
        for (int i = 0; i < matchingLines.length(); i++) {
            if (matchingLines.get(i)) {
                int y = usableArea.y + (int) ((double) i * scaleFactor);
                g.drawLine(usableArea.x, y, usableArea.width, y);
            }
        }
    }

    public synchronized void addMatchingLine(int lineNumber) {
        matchingLines.set(lineNumber);
        repaint();
    }

    public synchronized void clearMatchingLines() {
        matchingLines = new BitSet();
        repaint();
    }
}
