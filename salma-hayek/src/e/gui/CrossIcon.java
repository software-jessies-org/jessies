package e.gui;

import java.awt.*;
import java.awt.geom.*;

public class CrossIcon extends DrawnIcon {
    private Color fillColor;
    private Color lineColor;
    private float lineThickness;
    
    public CrossIcon(Color fillColor, Color lineColor, float lineThickness) {
        super(new Dimension(15, 15));
        this.fillColor = fillColor;
        this.lineColor = lineColor;
        this.lineThickness = lineThickness;
    }
    
    public void paintIcon(Component c, Graphics oldGraphics, int x, int y) {
        Graphics2D g = (Graphics2D) oldGraphics;
        Stroke originalStroke = g.getStroke();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        final int diameter = getIconWidth() - 1;
        
        final Ellipse2D.Double circle = new Ellipse2D.Double(x, y, diameter, diameter);
        g.setStroke(new BasicStroke(1.1f));
        if (fillColor != null) {
            g.setColor(fillColor);
            g.fill(circle);
        }
        
        g.setColor(lineColor);
        g.setStroke(new BasicStroke(lineThickness));
        g.drawLine(x + diameter/4, y + 3*diameter/4, x + 3*diameter/4, y + diameter/4);
        g.drawLine(x + diameter/4, y + diameter/4, x + 3*diameter/4, y + 3*diameter/4);
        g.setStroke(originalStroke);
    }
}
