package e.edit;

import java.awt.*;
import javax.swing.*;

/**
 * Displays a textual watermark in a view port. To use this, you
 * need to have a component in a JScrollPane with an instance of
 * this class as its view port. The component itself must be
 * non-opaque, or it'll overwrite our watermark.
 */
public class WatermarkViewPort extends JViewport {
    /**
     * The text to be repeated across the background.
     */
    private String watermark;
    
    /**
     * A very light gray.
     */
    private static final Color WATERMARK_COLOR = new Color(220, 220, 220);
    
    public WatermarkViewPort() {
        setBackground(Color.WHITE);
        setForeground(WATERMARK_COLOR);
    }
    
    /**
     * Sets the watermark string; ensures the view has the appropriate opacity.
     */
    public void setWatermark(final String newWatermark) {
        watermark = newWatermark;
        ((JComponent) getView()).setOpaque(watermark == null);
    }
    
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (watermark != null) {
            paintWatermark(g);
        }
    }
    
    public void paintWatermark(final Graphics g) {
        FontMetrics fm = g.getFontMetrics();
        final int watermarkHeight = (int) (1.2 * fm.getHeight());
        final int watermarkWidth = (int) (1.1 * fm.stringWidth(watermark));
        Rectangle clip = g.getClipBounds();
        g.setColor(getForeground());
        for (int x = clip.x - (clip.x % watermarkWidth); x < clip.x + clip.width; x += watermarkWidth) {
            for (int y = clip.y - (clip.y % watermarkHeight); y < clip.y + clip.height + watermarkHeight; y += watermarkHeight) {
                g.drawString(watermark, x, y);
            }
        }
    }
}
