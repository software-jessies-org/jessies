package e.debug;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;

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
        for (int i = 0; i < frames.length; ++i) {
            if (frames[i].isDisplayable()) {
                System.err.println("Displayable frame: " + frames[i]);
                ++displayableFrameCount;
            }
        }
        System.err.println("Problem (displayable) frames: " +
            displayableFrameCount);
    }

    public static void showSwingTimerQueue() {
        System.err.println("*** Examining Swing TimerQueue...");
        try {
            Class timerQueueClass = Class.forName("javax.swing.TimerQueue");
            Method sharedInstanceMethod =
                timerQueueClass.getDeclaredMethod("sharedInstance", null);
            sharedInstanceMethod.setAccessible(true);
            Object sharedInstance = sharedInstanceMethod.invoke(null, null);

            Field firstTimerField =
                timerQueueClass.getDeclaredField("firstTimer");
            firstTimerField.setAccessible(true);
            Field nextTimerField =
                javax.swing.Timer.class.getDeclaredField("nextTimer");
            nextTimerField.setAccessible(true);

            int extantTimers = 0;
            javax.swing.Timer nextTimer =
                (javax.swing.Timer) firstTimerField.get(sharedInstance);
            while (nextTimer != null) {
                ++extantTimers;
                System.err.println(nextTimer);
                showActionListeners(nextTimer.getActionListeners());

                nextTimer = (javax.swing.Timer) nextTimerField.get(nextTimer);
            }
            System.err.println("Problem (extant) timers: " + extantTimers);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void showActionListeners(ActionListener[] listeners) {
        for (int i = 0; i < listeners.length; ++i) {
            System.err.println(" listener #" + i + " " + listeners[i]);
        }
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
