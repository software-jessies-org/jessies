package e.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public abstract class ESplitPane extends JComponent implements MouseListener, MouseMotionListener {
    protected Component firstComponent;
    protected Component secondComponent;
    
    protected double dividerLocation;
    protected int dividerWidth;
    
    protected ESplitPane(Component firstComponent, Component secondComponent) {
        this.firstComponent = firstComponent;
        this.secondComponent = secondComponent;
        
        setLayout(null);
        add(firstComponent);
        add(secondComponent);
        
        setDividerLocation(0.5);
        setDividerWidth(7);
        
        setOpaque(true);
        
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                layoutComponents();
            }
        });
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    
    public static ESplitPane createHorizontalSplitPane(Component firstComponent, Component secondComponent) {
        return new ESplitPane.HorizontalSplitPane(firstComponent, secondComponent);
    }
    
    public static ESplitPane createVerticalSplitPane(Component firstComponent, Component secondComponent) {
        return new ESplitPane.VerticalSplitPane(firstComponent, secondComponent);
    }
    
    public void setDividerLocation(double dividerLocation) {
        this.dividerLocation = Math.min(Math.max(dividerLocation, 0.0), 1.0);
        if (getParent() != null) {
            layoutComponents();
        }
    }
    
    public double getDividerLocation() {
        return dividerLocation;
    }
    
    public void setDividerWidth(int dividerWidth) {
        this.dividerWidth = dividerWidth;
    }
    
    private void layoutComponents() {
        setComponentBounds();
        firstComponent.invalidate();
        secondComponent.invalidate();
        validate();
        repaint();
    }
    
    public boolean isValidateRoot() {
        return true;
    }

    public abstract void setComponentBounds() ;
    
    protected int[] getComponentSizes(int total) {
        int available = total - dividerWidth;
        int first = (int) (available * dividerLocation);
        int second = available - first;
        return new int[] { first, second };
    }
    
    public abstract Rectangle getDividerBounds() ;
    
    public abstract int getMouseCoordinate(Point p) ;
    
    public abstract double getDividerPositionChange(int dPixels) ;
    
//
// MouseListener and MouseMotionListener
//
    private int oldCoord;
    private boolean dragging = false;
    
    public void mousePressed(MouseEvent e) {
        Rectangle divider = getDividerBounds();
        if (divider.contains(e.getPoint()) == false) {
            return;
        }
        oldCoord = getMouseCoordinate(e.getPoint());
        dragging = true;
    }
    
    public void mouseReleased(MouseEvent e) {
        dragging = false;
    }
    
    public void mouseClicked(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }

    public void mouseExited(MouseEvent e) {
        setCursor(Cursor.getDefaultCursor());
    }
    
    public void mouseDragged(MouseEvent e) {
        if (dragging == false) {
            return;
        }
        int newCoord = getMouseCoordinate(e.getPoint());
        int dCoord = newCoord - oldCoord;
        setDividerLocation(getDividerLocation() + getDividerPositionChange(dCoord));
        oldCoord = newCoord;
    }
    
    public void mouseMoved(MouseEvent e) {
        Rectangle divider = getDividerBounds();
        if (divider.contains(e.getPoint()) == false) {
            setCursor(Cursor.getDefaultCursor());
        } else {
            setCursor(getResizeCursor());
        }
    }
    
    public abstract Cursor getResizeCursor() ;
    
    
    public static class HorizontalSplitPane extends ESplitPane {
        public HorizontalSplitPane(Component firstComponent, Component secondComponent) {
            super(firstComponent, secondComponent);
        }
        
        public Rectangle getDividerBounds() {
            return new Rectangle(firstComponent.getWidth(), 0, dividerWidth, getHeight());
        }
        
        /** Lays out the two components side by side, with a vertical split. */
        public void setComponentBounds() {
            int[] widths = getComponentSizes(getWidth());
            int height = getHeight();
            firstComponent.setBounds(0, 0, widths[0], height);
            secondComponent.setBounds(widths[0] + dividerWidth, 0, widths[1], height);
        }
        public Cursor getResizeCursor() {
            return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        }
        
        public int getMouseCoordinate(Point p) {
            return p.x;
        }
        
        public double getDividerPositionChange(int dPixels) {
            return (double) dPixels / (double) getWidth();
        }
        
        public void paintComponent(Graphics og) {
            Graphics2D g = (Graphics2D) og;
            Color leftColor = Color.getColor("titlebar.leftColor");
            Color rightColor = Color.getColor("titlebar.rightColor");
            Rectangle r = getDividerBounds();
            GradientPaint gradient = new GradientPaint((float) r.x, 0f, leftColor, (float) (r.x + r.width / 2), 0f, rightColor, true);
            g.setPaint(gradient);
            g.fill(r);
        }
    }
    
    public static class VerticalSplitPane extends ESplitPane {
        public VerticalSplitPane(Component firstComponent, Component secondComponent) {
            super(firstComponent, secondComponent);
        }
        
        public Rectangle getDividerBounds() {
            return new Rectangle(0, firstComponent.getHeight(), getWidth(), dividerWidth);
        }
        
        /** Lays out the two components one above the other, with a horizontal split. */
        public void setComponentBounds() {
            int[] heights = getComponentSizes(getHeight());
            int width = getWidth();
            firstComponent.setBounds(0, 0, width, heights[0]);
            secondComponent.setBounds(0, heights[0] + dividerWidth, width, heights[1]);
        }
        
        public Cursor getResizeCursor() {
            return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
        }
        
        public int getMouseCoordinate(Point p) {
            return p.y;
        }
        
        public double getDividerPositionChange(int dPixels) {
            return (double) dPixels / (double) getHeight();
        }
        
        public void paintComponent(Graphics og) {
            Graphics2D g = (Graphics2D) og;
            Color leftColor = Color.getColor("titlebar.leftColor");
            Color rightColor = Color.getColor("titlebar.rightColor");
            Rectangle r = getDividerBounds();
            GradientPaint gradient = new GradientPaint(0f, (float) r.y, leftColor, 0f, (float) (r.y + r.height / 2), rightColor, true);
            g.setPaint(gradient);
            g.fill(r);
        }
    }
}
