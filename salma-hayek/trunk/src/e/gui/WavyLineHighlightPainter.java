package e.gui;

import java.awt.*;
import javax.swing.plaf.*;
import javax.swing.text.*;

import e.util.*;

/**
 * Paints a highlight as a wavy line at the bottom of its allocated space.
 */

public class WavyLineHighlightPainter extends LayeredHighlighter.LayerPainter {
    
    private Color color = Color.BLACK;
    
    public WavyLineHighlightPainter(Color color) {
        this.color = color;
    }
    
    public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
        try {
            TextUI mapper = c.getUI();
            Rectangle p0 = mapper.modelToView(c, offs0);
            Rectangle p1 = mapper.modelToView(c, offs1);
            g.setColor(color);
            
            if (p0.y == p1.y) {
                Rectangle r = p0.union(p1);
                paintWavyHorizontalLine(g, r.x, r.x + r.width - 1, r.y + r.height - 2);
            } else {
                Rectangle r = bounds.getBounds();
                paintWavyHorizontalLine(g, p0.x, r.x + r.width - 1, p0.y + p0.height - 1);
                paintWavyHorizontalLine(g, r.x, p1.x - r.x, p1.y + p1.height - 1);
            }
        } catch (BadLocationException ex) {
            Log.warn("Couldn't render highlight.", ex);
        }
    }
    
    public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {
        g.setColor(color);
        if (offs0 == view.getStartOffset() && offs1 == view.getEndOffset()) {
            Rectangle r = bounds.getBounds();
            paintWavyHorizontalLine(g, r.x, r.x + r.width, r.y + r.height - 1);
            return r;
        } else {
            try {
                Shape shape = view.modelToView(offs0, Position.Bias.Forward, offs1, Position.Bias.Backward, bounds);
                Rectangle r = shape.getBounds();
                paintWavyHorizontalLine(g, r.x, r.x + r.width, r.y + r.height - 1);
                return r;
            } catch (BadLocationException ex) {
                Log.warn("Couldn't render highlight.", ex);
            }
        }
        return null;
    }
    
    public void paintWavyHorizontalLine(Graphics g, int x1, int x2, int y) {
        int x = Math.min(x1, x2);
        int end = Math.max(x1, x2);
        int yOff = 1;
        while (x < end) {
            g.drawLine(x, y + yOff, Math.min(end, x + 2), y - yOff);
            x += 2;
            yOff = -yOff;
        }
    }
}
