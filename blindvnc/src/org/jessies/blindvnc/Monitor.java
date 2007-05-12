package org.jessies.blindvnc;

import java.awt.*;

public class Monitor {
    private Rectangle bounds;
    private Robot robot;
    
    public Monitor(Rectangle bounds, Robot robot) {
        this.bounds = bounds;
        this.robot = robot;
    }
    
    public String toString() {
        return "Monitor[" + bounds.x + "," + bounds.y + "; " + bounds.width + "," + bounds.height + "]";
    }
    
    public int getDistance(Point point) {
        int yDistance = getDistance(point.y, bounds.y, bounds.height);
        int xDistance = getDistance(point.x, bounds.x, bounds.width);
        if (xDistance == -1 && yDistance == -1) {
            return -1;
        } else if (xDistance == -1) {
            return yDistance;
        } else if (yDistance == -1) {
            return xDistance;
        } else {
            return (int) Math.sqrt(xDistance * xDistance + yDistance * yDistance);
        }
    }
    
    public Point getConfinedPoint(Point point) {
        Point result = new Point(point.x - bounds.x, point.y - bounds.y);
        result.x = Math.min(Math.max(result.x, 0), bounds.width - 1);
        result.y = Math.min(Math.max(result.y, 0), bounds.height - 1);
        return result;
    }
    
    private int getDistance(int point, int min, int size) {
        if (point < min) {
            return min - point;
        } else if (point < min + size) {
            return -1;
        } else {
            return point - (min + size);
        }
    }
    
    public Robot getRobot() {
        return robot;
    }
}
