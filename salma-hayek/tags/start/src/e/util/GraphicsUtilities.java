package e.util;

import java.awt.*;

public class GraphicsUtilities {
    private GraphicsUtilities() { /* Not instantiable. */ }
    
    /**
     * Paints the background using a paper effect similar to that in Mac OS X. The
     * first line in the pattern is brightest, followed by one medium gray, one dark
     * gray, another medium then repeat until fade. There's no kind of ripple to the
     * lines. We're not going out of our way to be slow, so we stick to horizontal
     * line drawing only!
     *
     * TODO: add a parameter to turn this off for people with slower computers?
     *       Does it even make a perceptible difference?
     */
    public static void paintPaper(Graphics g, int brightestGray) {
        int grayStep = 10; // Ed had 5, but 10 is more like Mac OS X, which was our model.
        Rectangle clipRect = g.getClipBounds();
        int startY = clipRect.y / 4 * 4 - 4;
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
}
