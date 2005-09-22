package terminator.view;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import terminator.Options;

/**
 * Implements our visual bell by flashing a transparent rectangle over the
 * terminal content. The first implementation composited a bell in a rounded
 * rectangle like Mac OS' mute symbol, only using the beamed sixteenth notes
 * symbol instead of the loudspeaker symbol. Believe it or not, the current
 * simple implementation looks a lot more appropriate, even if it's not
 * particularly impressive.
 */
public class VisualBellViewport extends JViewport {
    private Color color;
    private boolean isBellVisible = false;
    private Timer timer;
    
    public VisualBellViewport() {
        initColor();
        initTimer();
    }
    
    private void initColor() {
        // We need to choose a color that will show up against the background.
        // A reasonable assumption is that the user's chosen such a color for their foreground.
        Color baseColor = Options.getSharedInstance().getColor("foreground");
        color = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 100);
    }
    
    private void initTimer() {
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setBellVisibility(false);
            }
        };
        timer = new Timer(100, actionListener);
    }
    
    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (isBellVisible) {
            paintBell(g);
        }
    }
    
    private void paintBell(Graphics g) {
        g.setColor(color);
        g.fillRect(0, 0, getWidth(), getHeight());
    }
    
    public void flash() {
        setBellVisibility(true);
        timer.restart();
    }
    
    private void setBellVisibility(final boolean newState) {
        isBellVisible = newState;
        JComponent.class.cast(getView()).setOpaque(!isBellVisible);
        repaint();
    }
}
