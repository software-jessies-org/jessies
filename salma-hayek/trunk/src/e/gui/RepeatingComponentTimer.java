package e.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.Timer;

/**
 * Helps avoid writing applications that don't exit. If you use Swing's Timer,
 * your application won't exit as long as you have running timers. (You can
 * diagnose such problems with the HungAwtExit class.) If you have a one-shot
 * timer, that's not such a problem because it just leads to a slightly
 * delayed exit. If you have a repeating timer, your application won't exit.
 * 
 * Somehow, you need to stop your timers when their windows are disposed. It
 * may be convenient in your application to do so, but it probably isn't, and
 * even when it's convenient, it will still take some amount of unnecessary
 * code. Just use an instance of this class instead. Each Timer is associated
 * with a Component, and the timer only runs while the component is
 * displayable.
 */
public class RepeatingComponentTimer {
    private Component component;
    private Timer timer;
    private boolean armed;
    
    public RepeatingComponentTimer(Component component, int msDelay, ActionListener listener) {
        this.component = component;
        this.timer = new Timer(msDelay, listener);
        this.armed = false;
        initHierarchyListener();
        changeTimerState();
    }
    
    private void initHierarchyListener() {
        component.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
                    changeTimerState();
                }
            }
        });
    }
    
    private void changeTimerState() {
        if (component.isDisplayable() && armed) {
            timer.start();
        } else {
            timer.stop();
        }
    }
    
    public boolean isRunning() {
        return timer.isRunning();
    }
    
    public void start() {
        armed = true;
        changeTimerState();
    }
    
    public void stop() {
        armed = false;
        changeTimerState();
    }
}
