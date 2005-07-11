package e.tools;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;
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
        timer.start();
    }
    
    private JComponent makeUi() {
        final JSlider scaleSlider = new JSlider(2, 16);
        scaleSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                scaleFactor = scaleSlider.getValue();
            }
        });
        scaleSlider.setPaintTicks(true);
        scaleSlider.setSnapToTicks(true);
        scaleSlider.setValue(2);
        
        JPanel result = new JPanel(new BorderLayout());
        result.add(new ScaledImagePanel(), BorderLayout.CENTER);
        result.add(scaleSlider, BorderLayout.SOUTH);
        return result;
    }
    
    private class ScaledImagePanel extends JComponent {
        public void paintComponent(Graphics g) {
            g.drawImage(image, 0, 0, null);
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
    
    /**
     * Returns the bounds of the virtual device (all displays). This code
     * based on the example in the JavaDoc for GraphicsConfiguration.
     * I was hoping it would let me account for the screen menu bar on Mac OS,
     * but it doesn't. I think the problem is with the native Robot code.
     */
    private static Rectangle getVirtualDeviceBounds() {
        Rectangle result = new Rectangle();
        for (GraphicsDevice graphicsDevice : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            for (GraphicsConfiguration graphicsConfiguration : graphicsDevice.getConfigurations()) {
                result = result.union(graphicsConfiguration.getBounds());
            }
        }
        return result;
    }
    
    public static void main(String[] args) {
        new FatBits().setVisible(true);
    }
}
