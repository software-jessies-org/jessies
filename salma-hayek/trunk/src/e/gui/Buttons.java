package e.gui;

import java.awt.*;
import javax.swing.*;

public class Buttons {
    private static JButton makeButton(Icon normalIcon) {
        final JButton button = new JButton(normalIcon);
        // Ensure the LAF doesn't interfere with our drawing.
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        // Although we've told the button not to paint the border, if we don't
        // remove the border, it will still influence the button's margin.
        button.setBorder(null);
        // Not only do we not want to have the focus painted, we really don't want the focus at all.
        // Typically, a user hitting an icon button wants to go back to what they were doing, not start working with the stop button.
        button.setFocusable(false);
        return button;
    }
    
    /**
     * A "close tab" button. Based on the same button in Google Chrome.
     */
    public static JButton makeCloseTabButton() {
        final JButton button = makeButton(new CrossIcon(null, new Color(0xb7b8ba), 1.5f));
        button.setPressedIcon(new CrossIcon(Color.BLACK, Color.WHITE, 1.5f));
        button.setRolloverIcon(new CrossIcon(new Color(0xc13535), Color.WHITE, 1.5f));
        button.setRolloverEnabled(true);
        return button;
    }
    
    /**
     * A stop button. Visually, this is similar to the GNOME
     * stock "stop" icon. Mac OS uses a very different icon, the red octagon
     * similar to that of US traffic stop signs, but without any label. See
     * http://en.wikipedia.org/wiki/Stop_sign for pictures.
     * 
     * The problem with the stop sign (other than the localization problem of
     * using an appropriate label) is that it just doesn't look like a actuator
     * because it's so familiar as an indicator.
     */
    public static JButton makeStopButton() {
        final JButton button = makeButton(new CrossIcon(new Color(255, 100, 100), Color.WHITE, 2.0f));
        button.setDisabledIcon(new CrossIcon(Color.LIGHT_GRAY, Color.WHITE, 2.0f));
        button.setPressedIcon(new CrossIcon(Color.RED.darker(), Color.WHITE, 2.0f));
        button.setRolloverIcon(new CrossIcon(Color.RED, Color.WHITE, 2.0f));
        button.setRolloverEnabled(true);
        return button;
    }
    
    public static void main(String[] arguments) {
        MainFrame frame = new MainFrame();
        frame.setLayout(new FlowLayout());
        frame.add(makeStopButton());
        frame.add(makeCloseTabButton());
        frame.pack();
        frame.setVisible(true);
    }
}
