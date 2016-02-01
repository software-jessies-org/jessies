package e.debug;

import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.management.*;
import java.util.*;
import java.util.List;
import javax.management.*;
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
public class HungAwtExit implements HungAwtExitMBean {
    public static void initMBean() {
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            server.registerMBean(new HungAwtExit(), new ObjectName("e.debug:type=HungAwtExitMBean"));
        } catch (Exception ex) {
            Log.warn("Couldn't initialize HungAwtExit MBean", ex);
        }
    }
    
    public int getExtantFrameCount() {
        return Frame.getFrames().length;
    }
    
    public String[] getDisplayableFrames() {
        final Frame[] frames = Frame.getFrames();
        final ArrayList<String> result = new ArrayList<String>();
        int displayableFrameCount = 0;
        for (Frame frame : frames) {
            if (frame.isDisplayable()) {
                result.add("Displayable frame: " + frame);
                ++displayableFrameCount;
            }
        }
        result.add("Problem (displayable) frames: " + displayableFrameCount);
        return result.toArray(new String[result.size()]);
    }
    
    public String[] getSwingTimers() {
        final List<Timer> timers = TimerUtilities.getQueuedSwingTimers();
        final ArrayList<String> result = new ArrayList<String>();
        for (Timer timer : timers) {
            result.add(TimerUtilities.toString(timer));
        }
        result.add("Problem (extant) timers: " + timers.size());
        return result.toArray(new String[result.size()]);
    }
    
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
