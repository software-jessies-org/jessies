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
    private ScaledImagePanel scaledImagePanel;
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
        setSize(new Dimension(250, 300));
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
        result.add(scaledImagePanel = new ScaledImagePanel(), BorderLayout.CENTER);
        result.add(makeControlPanel(), BorderLayout.SOUTH);
        return result;
    }
    
    private JComponent makeControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(makeScaleSlider(), BorderLayout.CENTER);
        panel.add(makeShowGridCheckBox(), BorderLayout.EAST);
        panel.setBorder(new javax.swing.border.EmptyBorder(0, 12, 4, 12));
        return panel;
    }
    
    private JCheckBox makeShowGridCheckBox() {
        final JCheckBox checkBox = new JCheckBox("Show Grid");
        checkBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                scaledImagePanel.setShowGrid(checkBox.isSelected());
            }
        });
        checkBox.setSelected(false);
        return checkBox;
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
        private Image image;
        private boolean showGrid;
        
        public void setImage(Image image) {
            this.image = image;
            repaint();
        }
        
        public void setShowGrid(boolean showGrid) {
            this.showGrid = showGrid;
            repaint();
        }
        
        public void paintComponent(Graphics g) {
            g.drawImage(image, 0, 0, null);
            paintGridLines(g);
        }
        
        private void paintGridLines(Graphics g) {
            if (showGrid == false) {
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
            Image scaledImage = capturedImage.getScaledInstance(scaledImagePanel.getWidth(), scaledImagePanel.getHeight(), Image.SCALE_REPLICATE);
            scaledImagePanel.setImage(scaledImage);
        }
        
        private Rectangle getScreenCaptureBounds(Point center) {
            Point topLeft = new Point(center.x - scaledImagePanel.getWidth() / (2 * scaleFactor), center.y - scaledImagePanel.getHeight() / (2 * scaleFactor));
            Rectangle result = new Rectangle(topLeft, scaledImagePanel.getSize());
            result.width /= scaleFactor;
            result.height /= scaleFactor;
            
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
