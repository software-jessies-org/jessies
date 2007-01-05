package e.gui;

import java.awt.*;
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
    public static final StopIcon NORMAL = new StopIcon(Color.RED);
    public static final StopIcon PRESSED = new StopIcon(Color.RED.darker());
    public static final StopIcon ROLLOVER = new StopIcon(new Color(255, 100, 100));
    
    private Color color;
    
    public StopIcon(Color color) {
        super(new Dimension(15, 15));
        this.color = color;
    }
    
    public void paintIcon(Component c, Graphics oldGraphics, int x, int y) {
        Graphics2D g = (Graphics2D) oldGraphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        final int diameter = getIconWidth() - 1;
        
        g.setColor(color);
        g.fillOval(x, y, diameter, diameter);
        g.setColor(Color.BLACK);
        g.drawOval(x, y, diameter, diameter);
        
        g.setColor(Color.WHITE);
        Stroke originalStroke = g.getStroke();
        g.setStroke(new BasicStroke(2));
        g.drawLine(x + 1 + diameter/4, y + 3*diameter/4, x + 3*diameter/4, y + 1 + diameter/4);
        g.drawLine(x + 1 + diameter/4, y + 1 + diameter/4, x + 3*diameter/4, y + 3*diameter/4);
        g.setStroke(originalStroke);
    }
    
    public static JButton makeStopButton() {
        JButton button = new JButton(StopIcon.NORMAL);
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
        JFrame frame = new JFrame();
        frame.setLayout(new FlowLayout());
        frame.add(new JLabel(StopIcon.NORMAL));
        frame.add(new JLabel(StopIcon.PRESSED));
        frame.add(new JLabel(StopIcon.ROLLOVER));
        frame.add(makeStopButton());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
