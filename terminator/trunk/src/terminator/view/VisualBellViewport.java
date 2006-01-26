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
    private static final int BELL_VISIBLE_MS = 200;
    
    private Color color;
    private boolean isBellVisible = false;
    private Timer timer;
    
    public VisualBellViewport() {
        initTimer();
    }
    
    private void initTimer() {
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setBellVisibility(false);
            }
        };
        timer = new Timer(BELL_VISIBLE_MS, actionListener);
        timer.setRepeats(false);
    }
    
    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (isBellVisible) {
            paintBell(g);
        }
    }
    
    private void paintBell(Graphics g) {
        if (Options.getSharedInstance().isVisualBell() == false) {
            return;
        }
        Color foreground = Options.getSharedInstance().getColor("foreground");
        if (Options.getSharedInstance().isFancyBell()) {
            // On decent hardware, we can produce a really tasteful effect
            // by compositing a transparent rectangle over the terminal.
            // We need to choose a color that will show up against the
            // background; a reasonable assumption is that the user has
            // already chosen such a color for the foreground.
            Color color = new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 100);
            g.setColor(color);
            g.fillRect(0, 0, getWidth(), getHeight());
        } else {
            // We used to composite a flash over the terminal, but on a remote
            // X11 display that was prohibitively expensive, so we offer XOR
            // instead.
            Color background = Options.getSharedInstance().getColor("background");
            final int R = blend(background.getRed(), foreground.getRed());
            final int G = blend(background.getGreen(), foreground.getGreen());
            final int B = blend(background.getBlue(), foreground.getBlue());
            g.setColor(background);
            g.setXORMode(new Color(R, G, B));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setPaintMode();
        }
    }
    
    /**
     * Imitates the effect of alpha-blending the foreground 40% with the background.
     */
    private static final int blend(float back, float fore) {
        return (int)(0.4f * fore + 0.6f * back);
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
