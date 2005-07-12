package e.tools;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import e.util.*;

/**
 * A Java equivalent of Apple's Pixie.app magnifying glass utility. This is
 * useful for checking layout, graphics, and colors in your own programs and
 * others.
 */
public class FatBits extends JFrame {
    private Robot robot;
    private Timer timer;
    private Image image;
    private int scaleFactor;
    
    public FatBits() {
        super("FatBits");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            Log.warn("failed to create a Robot", ex);
        }
        timer = new Timer(50, new MouseTracker());
        setSize(new Dimension(200, 200));
        setContentPane(makeUi());
        
        //
        // FIXME: support keyboard operation:
        //
        //   '+': increase scale (also with command, like Preview?)
        //   '-': decrease scale
        //   ' ': play/pause following pointer position (also C-L like Pixie?)
        //   arrow keys: move Robot for finer positioning than with the mouse
        //
        
        timer.start();
    }
    
    private JComponent makeUi() {
        // FIXME: NORTH should have a panel showing color information.
        JPanel result = new JPanel(new BorderLayout());
        result.add(new ScaledImagePanel(), BorderLayout.CENTER);
        result.add(makeScaleSlider(), BorderLayout.SOUTH);
        return result;
    }
    
    private JSlider makeScaleSlider() {
        final JSlider scaleSlider = new JSlider(1, 4);
        scaleSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                scaleFactor = (1 << scaleSlider.getValue());
                repaint();
            }
        });
        Hashtable<Integer, JComponent> labels = new Hashtable<Integer, JComponent>();
        for (int i = scaleSlider.getMinimum(); i <= scaleSlider.getMaximum(); ++i) {
            labels.put(i, new JLabel(Integer.toString(1 << i) + "x"));
        }
        scaleSlider.setLabelTable(labels);
        scaleSlider.setPaintLabels(true);
        scaleSlider.setPaintTicks(true);
        scaleSlider.setSnapToTicks(true);
        scaleSlider.setValue(1);
        return scaleSlider;
    }
    
    private class ScaledImagePanel extends JComponent {
        public void paintComponent(Graphics g) {
            g.drawImage(image, 0, 0, null);
            paintGridLines(g);
        }
        
        private void paintGridLines(Graphics g) {
            if (scaleFactor < 4) {
                return;
            }
            g.setColor(Color.BLACK);
            for (int x = scaleFactor; x < getWidth(); x += scaleFactor) {
                g.drawLine(x, 0, x, getHeight());
            }
            for (int y = scaleFactor; y < getHeight(); y += scaleFactor) {
                g.drawLine(0, y, getWidth(), y);
            }
        }
    }
    
    private class MouseTracker implements ActionListener {
        private Point lastPosition = null;

        public void actionPerformed(ActionEvent e) {
            PointerInfo pointerInfo = MouseInfo.getPointerInfo();
            Point center = pointerInfo.getLocation();
            if (lastPosition != null && lastPosition.equals(center)) {
                return;
            }
            lastPosition = center;
            
            Rectangle screenCaptureBounds = getScreenCaptureBounds(center);
            BufferedImage capturedImage = robot.createScreenCapture(screenCaptureBounds);
            Image scaledImage = capturedImage.getScaledInstance(getWidth() * scaleFactor, getHeight() * scaleFactor, Image.SCALE_REPLICATE);
            image = scaledImage;
            repaint();
        }
        
        private Rectangle getScreenCaptureBounds(Point center) {
            //
            // FIXME: we need to take the scale factor into account in this
            // method, both so that topLeft is right, and so we don't grab
            // more than we need (given that we can grab again if the user
            // decreases the scale factor).
            //
            
            Point topLeft = new Point(center.x - getWidth() / 2, center.y - getHeight() / 2);
            Rectangle result = new Rectangle(topLeft, getSize());
            
            // Constrain the capture to the display.
            // Apple's 1.5 VM crashes if you don't.
            result.x = Math.max(result.x, 0);
            result.y = Math.max(result.y, 0);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            if (result.x + result.width > screenSize.width) {
                result.x = screenSize.width - result.width;
            }
            if (result.y + result.height > screenSize.height) {
                result.y = screenSize.height - result.height;
            }
            return result;
        }
    }
    
    public static void main(String[] args) {
        new FatBits().setVisible(true);
    }
}
