package e.ptextarea;

import java.awt.*;

public class PUnderlinedTextSegment extends PTextSegment {
    public PUnderlinedTextSegment(int styleIndex, String text) {
        super(styleIndex, text);
    }
    
    public void paint(Graphics2D graphics, int x, int yBaseline) {
        super.paint(graphics, x, yBaseline);
        FontMetrics metrics = graphics.getFontMetrics();
        yBaseline += Math.min(2, metrics.getMaxDescent());
        int width = getDisplayWidth(metrics, x);
        graphics.drawLine(x, yBaseline, x + width, yBaseline);
    }
}
