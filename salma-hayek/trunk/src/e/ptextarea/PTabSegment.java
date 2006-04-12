package e.ptextarea;

import java.awt.*;

/**
 * A PTabSegment is a PLineSegment for runs of tab characters.
 * 
 * @author Phil Norman
 */
public class PTabSegment extends PAbstractSegment {
    public static final PTabSegment SINGLE_TAB = new PTabSegment(null, 0, 1);
    
    private static final int MIN_TAB_WIDTH_IN_PIXELS = 5;
    
    private static final int SPACES_PER_TAB = 8;
    
    private static final Stroke STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 7.0f, 1.0f }, 0.0f);
    
    public PTabSegment(PTextArea textArea, int start, int end) {
        super(textArea, start, end, PStyle.NORMAL);
    }
    
    @Override
    public PLineSegment subSegment(int start, int end) {
        return new PTabSegment(textArea, start + this.start, end + this.start);
    }
    
    private int tabWidth(FontMetrics metrics) {
        return (SPACES_PER_TAB * metrics.charWidth(' '));
    }
    
    @Override
    public int getDisplayWidth(FontMetrics metrics, int startX) {
        if (getModelTextLength() == 0) {
            return 0;
        }
        final int tabWidth = tabWidth(metrics);
        int x = startX + MIN_TAB_WIDTH_IN_PIXELS + tabWidth * getModelTextLength();
        x -= x % tabWidth;
        return (x - startX);
    }
    
    @Override
    public int getCharOffset(FontMetrics metrics, int startX, int x) {
        final int tabWidth = tabWidth(metrics);
        for (int i = 0; i < getModelTextLength(); i++) {
            int nextX = startX + MIN_TAB_WIDTH_IN_PIXELS + tabWidth;
            nextX -= nextX % tabWidth;
            if (x < nextX) {
                if (x > (nextX + startX) / 2) {
                    return i + 1;
                } else {
                    return i;
                }
            }
            startX = nextX;
        }
        return getModelTextLength();
    }
    
    private void paintArrow(Graphics2D g, FontMetrics metrics, int x, int yMiddle, int yBaseline) {
        g.drawLine(x, yBaseline, x, yBaseline - metrics.getMaxAscent());
        
        g.drawLine(x - 3, yMiddle - 3, x, yMiddle);
        g.drawLine(x - 3, yMiddle + 3, x, yMiddle);
    }
    
    @Override
    public void paint(Graphics2D g, int x, int yBaseline) {
        FontMetrics metrics = g.getFontMetrics();
        final int yMiddle = yBaseline - metrics.getAscent() / 2;
        final int xStop = x + getDisplayWidth(metrics, x) - 1;
        
        // Draw a dashed line for the entire length.
        g.setColor(Color.LIGHT_GRAY);
        Stroke oldStroke = g.getStroke();
        g.setStroke(STROKE);
        g.drawLine(x, yMiddle, xStop, yMiddle);
        g.setStroke(oldStroke);
        
        // Draw traditional tab-stop marks at each tab stop.
        final int tabWidth = tabWidth(metrics);
        for (int tabStopX = xStop; tabStopX > (x + MIN_TAB_WIDTH_IN_PIXELS); tabStopX -= tabWidth) {
            paintArrow(g, metrics, tabStopX, yMiddle, yBaseline);
        }
    }
}
