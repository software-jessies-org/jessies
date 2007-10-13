package e.gui;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * An icon for use on a stop button. Visually, this is similar to the GNOME
 * stock "stop" icon. Mac OS uses a very different icon, the red octagon
 * similar to that of US traffic stop signs, but without any label. See
 * http://en.wikipedia.org/wiki/Stop_sign for pictures.
 * 
 * The problem with the stop sign (other than the localization problem of
 * using an appropriate label) is that it just doesn't look like a actuator
 * because it's so familiar as an indicator.
 */
public class StopIcon extends DrawnIcon {
    public static final StopIcon NORMAL = new StopIcon(new Color(255, 100, 100));
    public static final StopIcon DISABLED = new StopIcon(Color.LIGHT_GRAY);
    public static final StopIcon PRESSED = new StopIcon(Color.RED.darker());
    public static final StopIcon ROLLOVER = new StopIcon(Color.RED);
    
    private Color color;
    
    public StopIcon(Color color) {
        super(new Dimension(15, 15));
        this.color = color;
    }
    
    public void paintIcon(Component c, Graphics oldGraphics, int x, int y) {
        Graphics2D g = (Graphics2D) oldGraphics;
        Stroke originalStroke = g.getStroke();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        final int diameter = getIconWidth() - 1;
        
        Ellipse2D.Double circle = new Ellipse2D.Double(x, y, diameter, diameter);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.1f));
        g.fill(circle);
        // Unless we're in the disabled state, make a nice clear edge.
        if (color.equals(Color.LIGHT_GRAY) == false) {
            g.setColor(color.darker());
        }
        g.draw(circle);
        
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawLine(x + 1 + diameter/4, y + 3*diameter/4, x + 3*diameter/4, y + 1 + diameter/4);
        g.drawLine(x + 1 + diameter/4, y + 1 + diameter/4, x + 3*diameter/4, y + 3*diameter/4);
        g.setStroke(originalStroke);
    }
    
    public static JButton makeStopButton() {
        JButton button = new JButton(StopIcon.NORMAL);
        button.setDisabledIcon(StopIcon.DISABLED);
        button.setPressedIcon(StopIcon.PRESSED);
        button.setRolloverIcon(StopIcon.ROLLOVER);
        button.setRolloverEnabled(true);
        // Ensure the LAF doesn't interfere with our drawing.
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        // Although we've told the button not to paint the border, if we don't
        // remove the border, it will still influence the button's margin.
        button.setBorder(null);
        // Not only do we not want to have the focus painted, we really don't want the focus at all.
        // Typically, a user hitting "stop" wants to go back to what they were doing, not start working with the stop button.
        button.setFocusable(false);
        return button;
    }
    
    public static void main(String[] arguments) {
        MainFrame frame = new MainFrame();
        frame.setLayout(new FlowLayout());
        frame.add(new JLabel(StopIcon.NORMAL));
        frame.add(new JLabel(StopIcon.DISABLED));
        frame.add(new JLabel(StopIcon.PRESSED));
        frame.add(new JLabel(StopIcon.ROLLOVER));
        frame.add(makeStopButton());
        frame.pack();
        frame.setVisible(true);
    }
}
