package e.debugger;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import e.util.*;

/**
 * Receives events fired by the Java VM under observation, and notifies listeners
 * in the debugger. Listeners register their interest by implementing VmEventListener,
 * and stating what Class of VM event they should be notified about.
 * 
 * Examples of Events include "breakpoint events", "thread creation events",
 * "exception events", etc.
 * 
 * @see VmEventListener
 * @see com.sun.jdi.event
 */

public class VmEventQueue implements Runnable {
    
    private EventQueue eventQueue;
    
    private Map<Class, List<VmEventListener>> eventListeners = new HashMap<Class, List<VmEventListener>>();
    
    public VmEventQueue(EventQueue eventQueue) {
        this.eventQueue = eventQueue;
    }
    
    public void run() {
        try {
            for (;;) {
                final EventSet events = eventQueue.remove();
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            for (EventIterator i = events.eventIterator(); i.hasNext(); ) {
                                Event event = i.nextEvent();
                                dispatchEvent(event);
                            }
                        }
                    });
                } catch (Exception ex) {
                    Log.warn("Exception while dispatching events.", ex);
                }
           }
        } catch (InterruptedException ex) {
            Log.warn(Thread.currentThread().getName() + " interrupted.", ex);
        }
    }
    
    private void dispatchEvent(final Event event) {
        System.err.println(event);
        for (Class<?> c : eventListeners.keySet()) {
            if (c.isAssignableFrom(event.getClass())) {
                for (VmEventListener listener : eventListeners.get(c)) {
                    listener.eventDispatched(event);
                }
            }
        }
    }
    
    public void addVmEventListener(VmEventListener l) {
        List<VmEventListener> listeners = eventListeners.get(l.getEventClass());
        if (listeners == null) {
            listeners = new ArrayList<VmEventListener>();
            eventListeners.put(l.getEventClass(), listeners);
        }
        listeners.add(l);
    }
    
    public void removeVmEventListener(VmEventListener l) {
        List<VmEventListener> listeners = eventListeners.get(l.getEventClass());
        if (listeners == null) {
            return;
        }
        listeners.remove(l);
        if (listeners.isEmpty()) {
            eventListeners.remove(l.getEventClass());
        }
    }
}
