package e.edit;

import java.awt.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import e.util.*;

/**
 * A bird's-eye view of the document. Intended to sit alongside the vertical scrollbar.
 * The find code informs us of matches, and we display little marks corresponding
 * to the matches' locations.
 */
public class BirdView extends JComponent {
    private ETextArea textArea;
    private JScrollBar scrollBar;

    private Method method;

    private BitSet matchingLines = new BitSet();

    public BirdView(ETextArea textArea, JScrollBar scrollBar) {
        this.textArea = textArea;
        this.scrollBar = scrollBar;
        try {
            method = BasicScrollBarUI.class.getDeclaredMethod("getTrackBounds", new Class[] {});
            method.setAccessible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(4, Integer.MAX_VALUE);
    }
    
    public Rectangle getUsableArea() {
        Rectangle usableArea = new Rectangle(0, 0, getWidth() - 1, getHeight() - 1);
        ScrollBarUI scrollUi = scrollBar.getUI();
        if (scrollUi instanceof BasicScrollBarUI) {
            BasicScrollBarUI basicUi = (BasicScrollBarUI) scrollUi;
            try {
                Rectangle trackArea = (Rectangle) method.invoke(basicUi, new Object[0]);
                JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, scrollBar);
                usableArea.y = trackArea.y + pane.getInsets().top;
                usableArea.height = trackArea.height;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (GuiUtilities.isMacOs()) {
            // These values were measured using Pixie. I don't know how to get them at run-time.
            usableArea.y += 10;
            usableArea.height -= 42;
        }
        return usableArea;
    }

    public void paintComponent(Graphics g) {
        g.setColor(SystemColor.control); //UIManager.getColor("Scrollbar.track"));
        g.fillRect(0, 0, getWidth(), getHeight());

        Rectangle usableArea = getUsableArea();

        if (false) {
            /* When testing, it's nice to see where we think the rectangle is. */
            g.setColor(Color.GRAY);
            g.drawRect(usableArea.x, usableArea.y, usableArea.width, usableArea.height);
        }

        int linesInDocument = textArea.getLineCount();
        // The '-1' in the following line is to force the last line of the file to be right at the bottom of
        // the bird view.  Otherwise the marker for the final line seems to be too high in short files.
        double scaleFactor = (double) usableArea.height / (double) (linesInDocument - 1);

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
