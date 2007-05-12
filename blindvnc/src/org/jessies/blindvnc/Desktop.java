package org.jessies.blindvnc;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import java.util.List;

public class Desktop {
    private Rectangle bounds;
    private List<Monitor> monitors = new ArrayList<Monitor>();
    private Monitor currentMonitor;
    private int oldButtonMask = 0;
    
    public Desktop() throws AWTException {
        Point topLeft = new Point(0, 0);
        Point bottomRight = new Point(0, 0);
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = env.getScreenDevices();
        for (GraphicsDevice device : devices) {
            Rectangle rect = device.getDefaultConfiguration().getBounds();
            monitors.add(new Monitor(rect, new Robot(device)));
            topLeft.x = Math.min(topLeft.x, rect.x);
            topLeft.y = Math.min(topLeft.y, rect.y);
            bottomRight.x = Math.max(bottomRight.x, rect.x + rect.width);
            bottomRight.y = Math.max(bottomRight.y, rect.y + rect.height);
        }
        bounds = new Rectangle(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
        currentMonitor = monitors.get(0);
    }
    
    public Dimension getSize() {
        return bounds.getSize();
    }
    
    private void setCurrentMonitor(Point point) {
        int closest = Integer.MAX_VALUE;
        for (Monitor monitor : monitors) {
            int distance = monitor.getDistance(point);
            if (distance < closest) {
                closest = distance;
                currentMonitor = monitor;
            }
        }
    }
    
    private int decodeVNCButtonMask(int vncButtonMask) {
        int result = 0;
        if ((vncButtonMask & 1) != 0) {
            result |= InputEvent.BUTTON1_MASK;
        }
        if ((vncButtonMask & 2) != 0) {
            result |= InputEvent.BUTTON2_MASK;
        }
        if ((vncButtonMask & 4) != 0) {
            result |= InputEvent.BUTTON3_MASK;
        }
        return result;
    }
    
    public void fireMouseEvent(int x, int y, int vncButtonMask) {
        x += bounds.x;
        y += bounds.y;
        setCurrentMonitor(new Point(x, y));
        Robot robot = currentMonitor.getRobot();
        Point point = currentMonitor.getConfinedPoint(new Point(x, y));
        robot.mouseMove(point.x, point.y);
        int buttonMask = decodeVNCButtonMask(vncButtonMask);
        int maskChange = buttonMask ^ oldButtonMask;
        if (maskChange != 0) {
            int buttonsToPress = maskChange & buttonMask;
            int buttonsToRelease = maskChange & ~buttonMask;
            if (buttonsToPress != 0) {
                robot.mousePress(buttonsToPress);
            }
            if (buttonsToRelease != 0) {
                robot.mouseRelease(buttonsToRelease);
            }
        }
        // Decode the special VNC mouse buttons 4 and 5, which if pressed mean that
        // the scroll wheel has been rotated up and down respectively by one unit.
        if ((vncButtonMask & (1 << 4)) != 0) {
            robot.mouseWheel(-1);    // Up.
        }
        if ((vncButtonMask & (1 << 5)) != 0) {
            robot.mouseWheel(1);    // Down.
        }
        oldButtonMask = buttonMask;
    }
    
    public void fireKeyEvent(int keyCode, boolean isDown) {
        Robot robot = currentMonitor.getRobot();
        if (isDown) {
            robot.keyPress(keyCode);
        } else {
            robot.keyRelease(keyCode);
        }
    }
}
