package e.util;

import java.awt.event.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.Timer;

public class TimerUtilities {
    public static List<Timer> getQueuedSwingTimers() {
        ArrayList<Timer> result = new ArrayList<Timer>();
        try {
            Class<?> timerQueueClass = Class.forName("javax.swing.TimerQueue");
            Method sharedInstanceMethod = timerQueueClass.getDeclaredMethod("sharedInstance", new Class[0]);
            sharedInstanceMethod.setAccessible(true);
            Object sharedInstance = sharedInstanceMethod.invoke(null, new Object[0]);
            
            Field firstTimerField = timerQueueClass.getDeclaredField("firstTimer");
            firstTimerField.setAccessible(true);
            Field nextTimerField = Timer.class.getDeclaredField("nextTimer");
            nextTimerField.setAccessible(true);
            
            Timer nextTimer = (Timer) firstTimerField.get(sharedInstance);
            while (nextTimer != null) {
                result.add(nextTimer);
                nextTimer = (Timer) nextTimerField.get(nextTimer);
            }
        } catch (Exception ex) {
            Log.warn("Failed to get queued Swing timers.", ex);
        }
        return result;
    }
    
    public static String toString(Timer timer) {
        StringBuilder result = new StringBuilder();
        result.append(timer + " " + timer.getDelay() + "ms " + (timer.isRepeats() ? "repeating" : "one-shot") + "\n");
        ActionListener[] listeners = timer.getActionListeners();
        for (int i = 0; i < listeners.length; ++i) {
            result.append(" listener #" + i + " " + listeners[i] + "\n");
        }
        return result.toString();
    }
    
    private TimerUtilities() {
    }
}
