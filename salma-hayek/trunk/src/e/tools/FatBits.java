package e.tools;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import e.util.*;

/**
 * A Java equivalent of Apple's Pixie.app magnifying glass utility. This is
 * useful for checking layout, graphics, and colors in your own programs and
 * others.
 */
public class FatBits extends JFrame {
    private Robot robot;
    private Timer timer;
    private ImageIcon icon;
    
    public FatBits() {
        super("FatBits");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            Log.warn("failed to create a Robot", ex);
        }
        timer = new Timer(500, new MouseTracker());
        setSize(new Dimension(200, 200));
        icon = new ImageIcon();
        setContentPane(new JLabel(icon));
        timer.start();
    }
    
    private class MouseTracker implements ActionListener {
    private Point lastPosition = null;

        public void actionPerformed(ActionEvent e) {
            /*
             * FIXME: this code requires 1.5; I don't think we can implement
             * this for 1.4 at all.
             */
/*
            PointerInfo pointerInfo = MouseInfo.getPointerInfo();
            Point center = pointerInfo.getLocation();
            if (lastPosition != null && lastPosition.equals(center)) {
                return;
            }
            lastPosition = center;
            Point topLeft = new Point(center.x - getWidth() / 2, center.y - getHeight() / 2);
            Rectangle rectangle = new Rectangle(topLeft, getSize());
            icon.setImage(robot.createScreenCapture(rectangle));
            repaint();
*/
        }
    }
    
    public static void main(String[] args) {
        new FatBits().setVisible(true);
    }
}
