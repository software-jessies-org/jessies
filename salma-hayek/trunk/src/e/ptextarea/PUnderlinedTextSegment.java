package e.ptextarea;

import java.awt.*;

public class PUnderlinedTextSegment extends PTextSegment {
    public PUnderlinedTextSegment(PStyle styleIndex, String text) {
        super(styleIndex, text);
    }

    public PUnderlinedTextSegment(PStyle styleIndex, String text, PTextSegment superSegment) {
        super(styleIndex, text, superSegment);
    }
    
    public void paint(Graphics2D graphics, int x, int yBaseline) {
        super.paint(graphics, x, yBaseline);
        FontMetrics metrics = graphics.getFontMetrics();
        yBaseline += Math.min(2, metrics.getMaxDescent());
        int width = getDisplayWidth(metrics, x);
        graphics.drawLine(x, yBaseline, x + width, yBaseline);
    }
    
    public PTextSegment subSegment(int start) {
        return new PUnderlinedTextSegment(getStyle(), getText().substring(start), getSuperSegment());
    }
    
    public PTextSegment subSegment(int start, int end) {
        return new PUnderlinedTextSegment(getStyle(), getText().substring(start, end), getSuperSegment());
    }
}
