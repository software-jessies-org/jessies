package e.edit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import e.gui.*;
import e.util.*;

/**
 * A simple title-bar with a label and a right-aligned close button.
 */
public class ETitleBar extends JComponent implements MouseListener, MouseMotionListener {
    /**
     * Ensures that all title bars correctly reflect the focus ownership of
     * their associated windows.
     */
    static {
        new KeyboardFocusMonitor() {
            public void focusChanged(Component oldOwner, Component newOwner) {
                setActive(oldOwner, false);
                setActive(newOwner, true);
            }
            
            public void setActive(Component c, boolean active) {
                EWindow window = (EWindow) SwingUtilities.getAncestorOfClass(EWindow.class, c);
                if (window == null) {
                    return;
                }
                window.getTitleBar().setActive(active);
            }
        };
    }
    
    private String name;
    
    private String displayTitle;
    
    private EWindow window;
    
    public static final int TITLE_HEIGHT = 20;
    
    private ECloseButton closeButton;
    private ESwitchButton switchButton;
    
    private boolean isActive;
    
    public ETitleBar(String name, EWindow window) {
        setFont(getDefaultFont());
        setOpaque(true);
        setTitle(name);
        this.window = window;
        
        this.closeButton = new ECloseButton(window);
        add(closeButton);
        
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    
    public void checkForCounterpart() {
        if (window instanceof e.edit.ETextWindow) {
            e.edit.ETextWindow textWindow = (e.edit.ETextWindow) window;
            if (switchButton == null && textWindow.getCounterpartFilename() != null) {
                this.switchButton = new ESwitchButton(textWindow);
                add(switchButton);
                revalidate();
                repaint();
            } else if (switchButton != null && textWindow.getCounterpartFilename() == null) {
                remove(switchButton);
                this.switchButton = null;
                revalidate();
                repaint();
            }
        }
    }
    
    public Font getDefaultFont() {
        return new Font(Parameters.getParameter("font.name"), Font.PLAIN,
        Parameters.getParameter("font.size", 12));
    }
    
    public void setTitle(String title) {
        this.name = title;
        updateDisplayTitle();
        revalidate();
        repaint();
    }
    
    public String getDisplayTitle() {
        return displayTitle;
    }
    
    public void updateDisplayTitle() {
        this.displayTitle = getTitle();
    }
    
    public String getTitle() {
        return this.name;
    }
    
    public void doLayout() {
        super.doLayout();
        if (closeButton != null) {
            closeButton.setBounds(getWidth() - 20, 1, 18, 18);
        }
        if (switchButton != null) {
            switchButton.setBounds(getWidth() - 40, 1, 18, 18);
        }
    }
    
    public void paintComponent(Graphics og) {
        Graphics2D g = (Graphics2D) og;
        paintBackground(g);
        paintText(g);
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean isActive) {
        this.isActive = isActive;
        if (isActive) {
            checkForCounterpart();
        }
        repaint();
    }
    
    /**
     * Paints a reasonable title bar background for Mac OS. Although you might
     * expect that this gives exactly the result you see with JInternalFrame,
     * it doesn't. As of 1.4.2, Apple invoke apple.laf.AquaImageFactory's
     * drawFrameTitleBackground method rather than using these colors. We
     * could use reflection to do the same, but that seems unnecessarily
     * fragile.
     */
    private void paintMacOsBackground(Graphics2D g) {
        if (isActive) {
            g.setColor(UIManager.getColor("InternalFrame.activeTitleBackground"));
        } else {
            g.setColor(UIManager.getColor("InternalFrame.inactiveTitleBackground"));
        }
        g.fillRect(0, 0, getWidth(), getHeight());
    }
    
    public void paintBackground(Graphics2D g) {
        if (GuiUtilities.isMacOs()) {
            paintMacOsBackground(g);
            return;
        }
        
        Color leftColor;
        Color rightColor;
        if (isActive) {
            leftColor = UIManager.getColor("InternalFrame.activeTitleBackground");
            rightColor = UIManager.getColor("InternalFrame.activeTitleGradient");
        } else {
            leftColor = UIManager.getColor("InternalFrame.inactiveTitleBackground");
            rightColor = UIManager.getColor("InternalFrame.inactiveTitleGradient");
        }
        
        // If the LAF doesn't have a title background, argh!
        if (leftColor == null) {
            leftColor = Color.GRAY;
        }
        // If the LAF doesn't use gradients, use a degenerate 'gradient'.
        if (rightColor == null) {
            rightColor = leftColor;
        }
        
        if (isActive == false) {
            if (UIManager.getLookAndFeel().getName().indexOf("GTK") != -1) {
                rightColor = rightColor.brighter();
                leftColor = leftColor.brighter();
            }
        }
        
        g.setPaint(rightColor);
        g.drawLine(0, 0, getWidth() - 1, 0);
        g.drawLine(0, 0, 0, getHeight() - 1);
        
        g.setPaint(leftColor);
        g.drawLine(0, getHeight() - 1, getWidth() - 1, getHeight() - 1);
        g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight() - 1);
        
        g.setPaint(new GradientPaint(0, 0, leftColor, getWidth(), getHeight(), rightColor));
        g.fillRect(1, 1, getWidth() - 2, getHeight() - 2);
    }
    
    public void paintText(Graphics2D g) {
        g.setColor(UIManager.getColor(isActive ? "InternalFrame.activeTitleForeground" : "InternalFrame.inactiveTitleForeground"));
        g.setFont(UIManager.getFont("InternalFrame.titleFont"));
        
        if (UIManager.getLookAndFeel().getName().indexOf("GTK") != -1) {
            g.setColor(Color.BLACK);
        }
        
        FontMetrics fm = g.getFontMetrics();
        int baseline = (getHeight() + fm.getAscent()) / 2 - 1;
        g.drawString(getDisplayTitle(), 2, baseline);
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(300, TITLE_HEIGHT);
    }
    
    public Dimension getMinimumSize() {
        return new Dimension(5, TITLE_HEIGHT);
    }
    
    public Dimension getMaximumSize() {
        return new Dimension(Short.MAX_VALUE, TITLE_HEIGHT);
    }
    
    /** Expands this window to maximum size on a double-click. */
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            window.expand();
        }
    }
    
    public void mouseEntered(MouseEvent e) {
    }
    
    public void mouseExited(MouseEvent e) {
    }
    
    public void mouseReleased(MouseEvent e) {
    }
    
    /**
    * Keeps track of how far down the title bar the mouse button is depressed. This allows
    * us to position the title bar correctly when we move it in the mouseDragged method.
    *
    * Also requests focus for the associated window.
    */
    public void mousePressed(MouseEvent e) {
        pointerOffset = e.getY();
        window.requestFocus();
    }
    
    /** How far down the title bar the pointer should remain. */
    private int pointerOffset;
    
    /** Causes the title bar to track the mouse's movements. */
    public void mouseDragged(MouseEvent e) {
        EColumn column = (EColumn) SwingUtilities.getAncestorOfClass(EColumn.class, this);
        column.moveTo(getParent(), SwingUtilities.convertMouseEvent(this, e, column).getY() - pointerOffset);
    }
    
    public void mouseMoved(MouseEvent e) { }
}
