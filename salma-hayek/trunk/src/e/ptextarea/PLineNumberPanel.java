package e.ptextarea;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * A JScrollPane row header that shows line numbers.
 */
public class PLineNumberPanel extends JComponent {
    private final static Color BACKGROUND_COLOR = new Color(0x888888);
    private final static Color FOREGROUND_COLOR = new Color(0xeeeeee);
    
    private final PTextArea textArea;
    
    public PLineNumberPanel(PTextArea textArea) {
        this.textArea = textArea;
        
        setBorder(BorderFactory.createEmptyBorder(1, 4, 0, 4));
        setBackground(BACKGROUND_COLOR);
        setForeground(FOREGROUND_COLOR);
        setOpaque(true);
        
        textArea.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                updatePreferredWidth();
                repaint();
            }
        });
        updatePreferredWidth();
        
        textArea.getTextBuffer().addTextListener(new PTextListener() {
            public void textInserted(PTextEvent e) {
                textChanged();
            }
            
            public void textRemoved(PTextEvent e) {
                textChanged();
            }
            
            public void textCompletelyReplaced(PTextEvent e) {
                textChanged();
            }
            
            private void textChanged() {
                repaint();
            }
        });
    }
    
    private void updatePreferredWidth() {
        if (getHeight() != textArea.getHeight() && !textArea.isLineWrappingInvalid()) {
            // The text area changed height, implying the number of lines changed.
            // Make sure we have enough space for the largest line number.
            final int stringSize = Math.max(Integer.toString(textArea.getLineCount()).length(), 4);
            final int width = getFontMetrics(textArea.getFont()).charWidth('0') * stringSize;
            final Insets insets = getInsets();
            final Dimension size = textArea.getSize();
            size.width = insets.left + width + insets.right;
            setSize(size);
            setPreferredSize(size);
        }
    }
    
    @Override public void paintComponent(Graphics oldGraphics) {
        final Graphics2D g = (Graphics2D) oldGraphics;
        
        if (textArea.isLineWrappingInvalid()) {
            return;
        }
        
        // Get the desktop rendering hints so that if the user's chosen anti-aliased text, we give it to them.
        Map<?, ?> map = (Map<?, ?>) (Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints"));
        if (map != null) {
            g.addRenderingHints(map);
        }
        
        final FontMetrics fontMetrics = textArea.getFontMetrics(Font.PLAIN);
        final Insets insets = getInsets();
        final int actualWidth = getWidth() - insets.left - insets.right;
        
        // Work out which lines we need to paint.
        final Rectangle bounds = g.getClipBounds();
        final int lineHeight = textArea.getLineHeight();
        final int maxSplitLine = textArea.getSplitLineCount() - 1;
        final int minLine = Math.max(0, Math.min(maxSplitLine, (bounds.y - insets.top) / lineHeight));
        final int maxLine = Math.min(maxSplitLine, (bounds.y - insets.top + bounds.height) / lineHeight);
        
        //System.err.println("paint size=" + getSize() + " bounds=" + bounds + " lines=" + minLine + ".." + maxLine);
        
        // Clear the background.
        g.setColor(getBackground());
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        
        g.setColor(getForeground());
        g.setFont(textArea.getFont());
        for (int i = minLine; i <= maxLine; i++) {
            final SplitLine splitLine = textArea.getSplitLine(i);
            // Check this isn't the continuation of a wrapped line.
            if (splitLine.getOffset() == 0) {
                final int lineNumber = splitLine.getLineIndex() + 1; // Humans number lines from 1.
                final String label = Integer.toString(lineNumber);
                final int x = insets.left + (actualWidth - fontMetrics.stringWidth(label));
                g.drawString(label, x, insets.top + textArea.getBaseline(i));
            }
        }
    }
}
