package e.debugger;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import java.util.*;

/**
 * The abstract superclass of line and exception breakpoints.
 * 
 */

public abstract class Breakpoint implements Comparable<Breakpoint> {
    
    protected String className = "";
    protected ReferenceType refType;
    protected Location location;
    protected boolean isResolved = false;
    protected boolean isValid = true;
    
    public String getClassName() {
        return className;
    }
    
    public ReferenceType getReferenceType() {
        return refType;
    }
    
    public Location getLocation() {
        return location;
    }
    
    /**
     * Tries to match this breakpoint matches to a loaded reference type in the target VM.
     * It can't be used until it's been resolved.
     */
    public abstract boolean resolve(TargetVm vm);
    
    public boolean isResolved() {
        return isResolved;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    /**
     * Creates an event request appropriate for this kind of breakpoint.
     */
    public abstract EventRequest createEventRequest(EventRequestManager erm);
    
    public String toString() {
        return "breakpoint: " + className + " (" + (isResolved == false ? "un" : "") + "resolved)";
    }
}
