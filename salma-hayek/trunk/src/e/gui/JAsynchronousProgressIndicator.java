package e.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;

/**
 * A progress indicator for background tasks whose duration isn't known until
 * it's finished. Especially useful when space is tight. The appearance is a
 * spinning highlight in a ring of bars, not unlike those seen in Cocoa or
 * Firefox.
 * 
 * Much of the rendering code was originally from O'Reilly's "Swing Hacks"
 * hack #47 "Indefinite Progress Indicator".
 */
public class JAsynchronousProgressIndicator extends JComponent {
    private static final int DIAMETER = 16;
    private static final int BAR_COUNT = 12;
    private static Area[] bars;
    private static Color[] colors;
    private Timer timer;
    private int currentBar = 0;
    
    public JAsynchronousProgressIndicator() {
        initBars();
        initColors();
        initTimer();
    }
    
    public void paintComponent(Graphics oldG) {
        Graphics2D g = (Graphics2D) oldG;
        
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.transform(AffineTransform.getTranslateInstance(getWidth() / 2, getHeight() /2));
        for (int i = 0; i < bars.length; ++i) {
            g.setColor(colors[(currentBar + i) % colors.length]);
            g.fill(bars[i]);
        }
    }
    
    private void initTimer() {
        timer = new Timer(50, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentBar = (currentBar + 1) % bars.length;
                repaint();
            }
        });
        timer.setRepeats(true);
        timer.start();
    }
    
    private synchronized static void initColors() {
        if (colors != null) {
            return;
        }
        
        colors = new Color[BAR_COUNT];
        for (int i = 0; i < colors.length; ++i) {
            int value = 224 - 128 / (i + 1);
            colors[i] = new Color(value, value, value);
        }
    }
    
    private synchronized static void initBars() {
        if (bars != null) {
            return;
        }
        
        bars = new Area[BAR_COUNT];
        
        final double fixedAngle = 2.0 * Math.PI / (double) bars.length;
        for (int i = 0; i < bars.length; ++i) {
            Area primitive = makeBar();
            
            Point2D.Double center = new Point2D.Double((double) DIAMETER / 2, (double) DIAMETER / 2);
            AffineTransform toCircle = AffineTransform.getRotateInstance(((double) -i) * fixedAngle, center.getX(), center.getY());
            AffineTransform toBorder = AffineTransform.getTranslateInstance(45.0, -6.0);
            
            AffineTransform toScale = AffineTransform.getScaleInstance(0.1, 0.1);
            
            primitive.transform(toBorder);
            primitive.transform(toCircle);
            primitive.transform(toScale);
            
            bars[i] = primitive;
        }
    }
    
    private static Area makeBar() {
        Rectangle2D.Double body = new Rectangle2D.Double(6, 0, 30, 12);
        Ellipse2D.Double head = new Ellipse2D.Double(0, 0, 12, 12);
        Ellipse2D.Double tail = new Ellipse2D.Double(30, 0, 12, 12);
        Area tick = new Area(body);
        tick.add(new Area(head));
        tick.add(new Area(tail));
        return tick;
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(DIAMETER, DIAMETER);
    }
    
    public static void main(String[] args) {
        JFrame f = new JFrame("test");
        f.getContentPane().add(new JAsynchronousProgressIndicator());
        f.pack();
        f.setVisible(true);
    }
}
