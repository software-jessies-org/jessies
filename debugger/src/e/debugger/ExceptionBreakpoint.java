package e.debugger;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import java.util.*;

/**
 * Represents an exception breakpoint.
 * 
 */

public class ExceptionBreakpoint extends Breakpoint {
    
    public ExceptionBreakpoint(String className) {
        this.className = className;
        if (className.endsWith(".java")) {
            className = className.substring(0, className.length() - ".java".length());
        }
    }
    
    /**
     * Checks if this the exception type corresponds with a type
     * that the VM has loaded. It's no use until it does.
     */
    public boolean resolve(TargetVm vm) {
        for (ReferenceType refType : vm.getClassesByName(className)) {
            this.refType = refType;
            this.isResolved = true;
        }
        return isResolved;
    }
    
    /**
     * Creates the appropriate event request once the breakpoint's been resolved.
     */
    public EventRequest createEventRequest(EventRequestManager erm) {
        return erm.createExceptionRequest(getReferenceType(), true, true);
    }
    
    public boolean isResolved() {
        return getReferenceType() != null;
    }
    
    public String toString() {
        return className;
    }
    
    public boolean equals(Object o) {
        if (o instanceof ExceptionBreakpoint) {
            ExceptionBreakpoint b = (ExceptionBreakpoint) o;
            return b.getClassName().equals(className);
        } else {
            return false;
        }
    }
    
    public int hashCode() {
        return className.hashCode();
    }
    
    public int compareTo(Breakpoint b) {
        if (b instanceof LineBreakpoint) {
            // Sort exception breakpoints above line breakpoints
            return -1;
        } else {
            return toString().compareTo(b.toString());
        }
    }
}
