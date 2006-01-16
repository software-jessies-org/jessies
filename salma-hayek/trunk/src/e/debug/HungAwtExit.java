package e.debug;

import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

/**
 * Diagnoses a GUI application that doesn't exit when you close what
 * you think is the last Frame.
 *
 * The two most common problems in my experience involve Frames that
 * haven't had Window.dispose invoked on them, and timers. This class
 * offers a static method "explain" that you should use to register
 * the Frame that you'll be closing last. (I'd guess it's easiest to
 * register them all, but there's no need to.)
 *
 * In a constructor, something like <code>e.debug.HungAwtExit.explain(this);</code>
 * would do it.
 */
public class HungAwtExit {
    public static void showDisplayableFrames() {
        System.err.println("*** Examining Frames...");
        Frame[] frames = Frame.getFrames();
        System.err.println("Extant frames: " + frames.length);
        int displayableFrameCount = 0;
        for (Frame frame : frames) {
            if (frame.isDisplayable()) {
                System.err.println("Displayable frame: " + frame);
                ++displayableFrameCount;
            }
        }
        System.err.println("Problem (displayable) frames: " + displayableFrameCount);
    }
    
    public static void showSwingTimerQueue() {
        System.err.println("*** Examining Swing TimerQueue...");
        List<Timer> timers = TimerUtilities.getQueuedSwingTimers();
        for (Timer timer : timers) {
            System.err.println(TimerUtilities.toString(timer));
        }
        System.err.println("Problem (extant) timers: " + timers.size());
    }

    public static void explain(Frame f) {
        f.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                showDisplayableFrames();
                showSwingTimerQueue();
            }
        });
    }

    private HungAwtExit() {
        // Prevents instantiation.
    }
}
