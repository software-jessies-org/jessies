package e.gui;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import e.util.*;

/**
 * A bird's-eye view of the document. Intended to sit alongside the vertical scrollbar.
 * The find code informs us of matches, and we display little marks corresponding
 * to the matches' locations.
 * 
 * If you hover over the view, the nearest mark (if it's close enough) will be highlighted.
 * Clicking will take you to that match.
 * 
 * A mark on the line with the caret will be highlighted in the caret color.
 */
public class BirdView extends JComponent {
    private BirdsEye birdsEye;
    private JScrollBar scrollBar;

    private Method method;

    private BitSet matchingLines = new BitSet();
    
    private int nearestLineToMouseInBirdView = -1;
    
    private int selectionStartLine = -1;
    private int selectionEndLine = -1;
    
    private boolean isAdjusting = false;
    
    public BirdView(BirdsEye birdsEye, JScrollBar scrollBar) {
        this.birdsEye = birdsEye;
        this.scrollBar = scrollBar;
        try {
            method = BasicScrollBarUI.class.getDeclaredMethod("getTrackBounds");
            method.setAccessible(true);
        } catch (Exception ex) {
            Log.warn("Couldn't get access to getTrackBounds", ex);
        }
        initMouseListener();
        initScrollBarMotionListener();
    }
    
    private void initScrollBarMotionListener() {
        scrollBar.getModel().addChangeListener(new ChangeListener() {
            private int lastScrollRange;
            
            public void stateChanged(ChangeEvent event) {
                int newScrollRange = scrollBar.getModel().getMaximum();
                if (newScrollRange != lastScrollRange) {
                    repaint();
                    lastScrollRange = newScrollRange;
                }
            }
        });
    }
    
    private void initMouseListener() {
        MouseInputListener listener = new MouseInputAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && nearestLineToMouseInBirdView != -1) {
                    // Humans and goToLine() number lines from 1.
                    birdsEye.goToLineAtIndex(nearestLineToMouseInBirdView);
                }
            }
            
            public void mouseExited(MouseEvent e) {
                nearestLineToMouseInBirdView = -1;
                repaint();
            }
            
            public void mouseMoved(MouseEvent e) {
                findNearestMatchingLineTo(viewToModel(e.getY()));
                repaint();
            }
        };
        addMouseListener(listener);
        addMouseMotionListener(listener);
    }

    public int viewToModel(int y) {
        Rectangle usableArea = getUsableArea();
        double scaleFactor = getLineScaleFactor(usableArea);
        int visibleLineIndex = (int) ((y - usableArea.y) / scaleFactor);
        visibleLineIndex = Math.max(0, Math.min(visibleLineIndex, birdsEye.getVisibleLineCount() - 1));
        return birdsEye.getLogicalLineIndex(visibleLineIndex);
    }
    
    private void findNearestMatchingLineTo(int exactLine) {
        nearestLineToMouseInBirdView = -1;
        double pixelsPerLine = getLineScaleFactor(getUsableArea());
        int maxDistanceInPixels = 10;
        int maxDistanceInLines = Math.min((int) (maxDistanceInPixels / pixelsPerLine), birdsEye.getVisibleLineCount());
        // We want to go round this loop at least once - hence <=.
        for (int distance = 0; distance <= maxDistanceInLines; ++distance) {
            if (setNearestLineIfMatching(exactLine + distance) || setNearestLineIfMatching(exactLine - distance)) {
                return;
            }
        }
    }
    
    private boolean setNearestLineIfMatching(final int line) {
        if (line >= 0 && line < matchingLines.length() && matchingLines.get(line)) {
            nearestLineToMouseInBirdView = line;
            return true;
        } else {
            return false;
        }
    }

    public Dimension getPreferredSize() {
        // We care about our width, but want to take our height from our corresponding text component.
        // This assumes we'll be laid out by the same layout manager, but it seems like that must always be the case anyway.
        return new Dimension(4, 0);
    }
    
    public Rectangle getUsableArea() {
        Rectangle usableArea = new Rectangle(0, 0, getWidth() - 1, getHeight() - 1);
        ScrollBarUI scrollUi = scrollBar.getUI();
        if (method != null && scrollUi instanceof BasicScrollBarUI) {
            BasicScrollBarUI basicUi = (BasicScrollBarUI) scrollUi;
            try {
                Rectangle trackArea = (Rectangle) method.invoke(basicUi, new Object[0]);
                JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, scrollBar);
                usableArea.y = trackArea.y + pane.getInsets().top;
                usableArea.height = trackArea.height;
            } catch (Exception ex) {
                Log.warn("Couldn't invoke getTrackBounds; won't try again", ex);
                method = null;
            }
        } else if (GuiUtilities.isMacOs()) {
            // These values were measured using Pixie. I don't know how to get them at run-time.
            usableArea.y += 10;
            usableArea.height -= 42;
        }
        return usableArea;
    }

    private void updateCursor() {
        Cursor newCursor = (nearestLineToMouseInBirdView != -1) ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor();
        if (newCursor != getCursor()) {
            setCursor(newCursor);
        }
    }

    public void paintComponent(Graphics g) {
        if (GuiUtilities.isMacOs()) {
            // Mac OS' Panel.background describes itself as "apple.laf.CColorPaintUIResource[r=200,g=200,b=200]".
            // Unfortunately, this doesn't appear to be a color meant for use by normal Java code, and is presumably a tag for native code.
            // For Java's purposes, it appears to be transparent paint.
            // The "control" SystemColor does behave reasonably, though, so we use that.
            g.setColor(SystemColor.control);
        } else {
            g.setColor(UIManager.getColor("Panel.background"));
        }
        g.fillRect(0, 0, getWidth(), getHeight());

        updateCursor();

        final Rectangle usableArea = getUsableArea();
        final double scaleFactor = getLineScaleFactor(usableArea);
        
        // Make the extent of the current selection visible in the background.
        if (selectionStartLine != -1) {
            g.setColor(Color.LIGHT_GRAY);
            final int y0 = usableArea.y + (int) ((double) selectionStartLine * scaleFactor);
            final int y1 = usableArea.y + (int) ((double) selectionEndLine * scaleFactor);
            g.fillRect(usableArea.x, y0, usableArea.x + usableArea.width + 1, y1 - y0 + 1);
        }
        
        // Mark the individual matches.
        for (int i = matchingLines.nextSetBit(0); i != -1; i = matchingLines.nextSetBit(i + 1)) {
            Color color = Color.BLACK;
            if (i == nearestLineToMouseInBirdView) {
                color = Color.CYAN;
            } else if (birdsEye.isCurrentLineIndex(i) || (i >= selectionStartLine && i <= selectionEndLine)) {
                color = Color.RED;
            }
            g.setColor(color);
            int visibleIndex = birdsEye.getVisibleLineIndex(i);
            int y = usableArea.y + (int) ((double) visibleIndex * scaleFactor);
            g.drawLine(usableArea.x, y, usableArea.width, y);
        }
    }
    
    public double getLineScaleFactor(Rectangle usableArea) {
        // The '-1' in the following line is to force the last line of the file to be right at the bottom of
        // the bird view.  Otherwise the marker for the final line seems to be too high in short files.
        return ((double) usableArea.height) / (birdsEye.getVisibleLineCount() - 1);
    }
    
    // Lines are numbered from 0.
    public void setSelectedLines(int startLine, int endLine) {
        if (selectionStartLine != startLine || selectionEndLine != endLine) {
            selectionStartLine = startLine;
            selectionEndLine = endLine;
            repaint();
        }
    }
    
    public synchronized void addMatchingLine(int lineNumber) {
        matchingLines.set(lineNumber);
        maybeRepaint();
    }
    
    public synchronized void removeMatchingLine(int lineNumber) {
        matchingLines.set(lineNumber, false);
        maybeRepaint();
    }
    
    public synchronized void clearMatchingLines() {
        matchingLines = new BitSet();
        maybeRepaint();
    }
    
    public boolean getValueIsAdjusting() {
        return isAdjusting;
    }
    
    public void setValueIsAdjusting(boolean isAdjusting) {
        if (isAdjusting != this.isAdjusting) {
            this.isAdjusting = isAdjusting;
            if (isAdjusting == false) {
                repaint();
            }
        }
    }
    
    private void maybeRepaint() {
        if (isAdjusting == false) {
            repaint();
        }
    }
}
