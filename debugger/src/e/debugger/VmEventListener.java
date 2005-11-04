package e.debugger;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import java.util.*;

/**
 * The interface to be implemented by any class wanting to receive notifications about
 * events in the debug target VM.
 */

public interface VmEventListener extends EventListener {
    
    /**
     * Returns the JDI Event type that this listener is interested in.
     */
    public Class getEventClass();
    
    /**
     * Called when an event of the type returned in getEventClass is dispatched
     * by the debug target VM.
     */
    public void eventDispatched(Event e);
}
