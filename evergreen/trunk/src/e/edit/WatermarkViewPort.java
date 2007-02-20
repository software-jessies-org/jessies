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
    
    /**
     * A very light red. Okay, so it's pink. It doesn't mean anything.
     */
    private static final Color SERIOUS_COLOR = new Color(250, 201, 201);
    
    public WatermarkViewPort() {
        setBackground(Color.WHITE);
        setSerious(false);
    }
    
    /**
     * Reverts to the default foreground color for the watermark text.
     */
    public void setSerious(boolean isSerious) {
        setForeground(isSerious ? SERIOUS_COLOR : WATERMARK_COLOR);
    }
    
    /**
     * Sets the watermark string; ensures the view has the appropriate opacity.
     * Use null rather than the empty string if you want no watermark.
     */
    public void setWatermark(final String newWatermark) {
        this.watermark = newWatermark;
        JComponent view = JComponent.class.cast(getView());
        view.setOpaque(watermark == null);
        repaint();
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
        final int watermarkWidth = (int) (1.1 * fm.getStringBounds(watermark, g).getWidth());
        Rectangle clip = g.getClipBounds();
        g.setColor(getForeground());
        for (int x = clip.x - (clip.x % watermarkWidth); x < clip.x + clip.width; x += watermarkWidth) {
            for (int y = clip.y - (clip.y % watermarkHeight); y < clip.y + clip.height + watermarkHeight; y += watermarkHeight) {
                g.drawString(watermark, x, y);
            }
        }
    }
}
