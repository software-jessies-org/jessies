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
 * 
 * There are two typical patterns for use. In the first, isDisplayedWhenStopped
 * is false, and you invoke startAnimation when your task begins, and invoke
 * stopAnimation when it finishes. In the second, isDisplayedWhenStopped is
 * true, and you repeatedly invoke animateOneFrame.
 */
public class JAsynchronousProgressIndicator extends JComponent {
    private static final int DIAMETER = 16;
    private static final int BAR_COUNT = 12;
    private static Area[] bars;
    private static Color[] colors;
    private Timer timer;
    private int currentBar = 0;
    private boolean painted = true;
    private boolean isDisplayedWhenStopped = false;
    
    private int currentValue = 0;
    private int maxValue = 0;
    
    public JAsynchronousProgressIndicator() {
        initBars();
        initColors();
        initTimer();
        setOpaque(false);
    }
    
    public void paintComponent(Graphics oldGraphics) {
        Graphics2D g = (Graphics2D) oldGraphics;
        if (painted && (isDisplayedWhenStopped || timer.isRunning())) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (maxValue != 0) {
                paintPie(g);
            } else {
                paintBars(g);
            }
        }
    }
    
    private void paintPie(Graphics2D g) {
        int width = getWidth() - 1;
        int height = getHeight() - 1;
        
        int x = width/2 - DIAMETER/2;
        int y = height/2 - DIAMETER/2;
        
        g.setColor(UIManager.getColor("TabbedPane.shadow"));
        
        // The documentation for drawArc and fillArc implies that they both cover an area (width+1, height+1).
        // With anti-aliasing off, I think they're right. With it on, I'm not convinced.
        // We'd be mad to turn anti-aliasing off when drawing arcs, so fudge things by using thick lines.
        // It's not pixel-perfect, but it's close enough for the casual observer.
        int degreesToFill = degreesFilledBy(currentValue, maxValue);
        g.fillArc(x, y, DIAMETER, DIAMETER, 90, degreesToFill);
        Stroke originalStroke = g.getStroke();
        g.setStroke(new BasicStroke(1.5f));
        g.drawArc(x, y, DIAMETER, DIAMETER, 90, -360);
        g.setStroke(originalStroke);
    }
    
    private static int degreesFilledBy(int current, int max) {
        return (int) (-360.0 * ((double) current / (double) max));
    }
    
    private void paintBars(Graphics2D g) {
        g.transform(AffineTransform.getTranslateInstance(getWidth() / 2, getHeight() /2));
        for (int i = 0; i < bars.length; ++i) {
            g.setColor(colors[(currentBar + i) % colors.length]);
            g.fill(bars[i]);
        }
    }
    
    private void initTimer() {
        timer = new Timer(50, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                animateOneFrame();
            }
        });
        timer.setRepeats(true);
    }
    
    public void startAnimation() {
        timer.start();
    }
    
    public void stopAnimation() {
        timer.stop();
        currentValue = maxValue = 0;
        repaint();
    }
    
    public void animateOneFrame() {
        currentBar = (currentBar + 1) % bars.length;
        repaint();
    }
    
    public boolean isDisplayedWhenStopped() {
        return isDisplayedWhenStopped;
    }
    
    public void setDisplayedWhenStopped(boolean newState) {
        if (isDisplayedWhenStopped != newState) {
            isDisplayedWhenStopped = newState;
            repaint();
        }
    }
    
    /**
     * Sometimes the resizing caused by setVisible is annoying, and you'd just like the component not to paint itself.
     */
    public void setPainted(boolean newState) {
        if (painted != newState) {
            painted = newState;
            repaint();
        }
    }
    
    /**
     * Switches from the usual spinning "indeterminate" animation to a "determinate" pie chart style.
     * To revert to the "indeterminate" style, call either setProgress(0, 0) or stopAnimation.
     */
    public void setProgress(int newCurrentValue, int newMaxValue) {
        final int oldCurrentValue = currentValue;
        final int oldMaxValue = maxValue;
        
        this.currentValue = newCurrentValue;
        this.maxValue = newMaxValue;
        
        // If the change can have caused a visible change on the display, repaint.
        if (maxValue == 0 || degreesFilledBy(oldCurrentValue, oldMaxValue) != degreesFilledBy(currentValue, maxValue)) {
            repaint();
        }
    }
    
    private synchronized static void initColors() {
        if (colors != null) {
            return;
        }
        
        colors = new Color[BAR_COUNT];
        for (int i = 0; i < colors.length; ++i) {
            int value = 210 - 128 / (i + 1);
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
        int barThickness = 20;
        Rectangle2D.Double body = new Rectangle2D.Double(6, 0, 30, barThickness);
        Ellipse2D.Double head = new Ellipse2D.Double(0, 0, barThickness, barThickness);
        Ellipse2D.Double tail = new Ellipse2D.Double(30, 0, barThickness, barThickness);
        Area tick = new Area(body);
        tick.add(new Area(head));
        tick.add(new Area(tail));
        return tick;
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(DIAMETER + 2, DIAMETER + 2);
    }
    
    public static void main(String[] args) {
        // FIXME: we could auto-generate this kind of code via reflection.
        JFrame f = new JFrame("JAsynchronousProgressIndicator test");
        
        final JAsynchronousProgressIndicator asynchronousProgressIndicator = new JAsynchronousProgressIndicator();
        
        final JCheckBox displayedWhenStoppedCheckbox = new JCheckBox("displayedWhenStopped", asynchronousProgressIndicator.isDisplayedWhenStopped());
        displayedWhenStoppedCheckbox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                asynchronousProgressIndicator.setDisplayedWhenStopped(displayedWhenStoppedCheckbox.isSelected());
            }
        });
        
        JButton startButton = new JButton("startAnimation");
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                asynchronousProgressIndicator.startAnimation();
            }
        });
        
        JButton stopButton = new JButton("stopAnimation");
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                asynchronousProgressIndicator.stopAnimation();
            }
        });
        
        JButton animateOneFrameButton = new JButton("animateOneFrame");
        animateOneFrameButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                asynchronousProgressIndicator.animateOneFrame();
            }
        });
        
        f.getContentPane().setLayout(new FlowLayout());
        f.getContentPane().add(asynchronousProgressIndicator);
        f.getContentPane().add(animateOneFrameButton);
        f.getContentPane().add(startButton);
        f.getContentPane().add(stopButton);
        f.getContentPane().add(displayedWhenStoppedCheckbox);
        f.pack();
        f.setVisible(true);
    }
}
