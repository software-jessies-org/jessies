package e.debugger;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import java.util.*;

import e.util.*;

/**
 * Represents a fully.qualified.class.Name:linenumber style breakpoint.
 * 
 */

public class LineBreakpoint extends Breakpoint {
    
    private int lineNumber;
    
    public LineBreakpoint(String className, int lineNumber) {
        // Coming from Edit, the class name is likely to be a file name.
        if (className.endsWith(".java")) {
            className = className.substring(0, className.length() - ".java".length());
        }
        this.className = className;
        this.lineNumber = lineNumber;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    /**
     * Attempts to match the classname:linenumber location to a Location in an
     * already-loaded reference type in the target VM.
     */
    public boolean resolve(TargetVm vm) {
        for (ReferenceType refType : vm.getClassesByName(className)) {
            try {
                this.location = findLocation(refType, lineNumber);
                this.refType = refType;
                this.isResolved = true;
                Log.warn("Resolving " + this);
                return true;
            } catch (LineNotFoundException lnfex) {
                this.isValid = false;
                Log.warn("Resolving " + this);
                Log.warn("Failed: Line not found in " + refType.name());
            } catch (AbsentInformationException aiex) {
                this.isValid = false;
                Log.warn("Resolving " + this);
                Log.warn("Failed: Absent information.");
                aiex.printStackTrace();
            }
        }
        return false;
    }
    
    private Location findLocation(ReferenceType type, int line) throws AbsentInformationException, LineNotFoundException {
        Location location = null;
        List locs = type.locationsOfLine(lineNumber);
        if (locs.size() == 0) {
            throw new LineNotFoundException();
        }
        location = (Location) locs.get(0);
        if (location.method() == null) {
            throw new LineNotFoundException();
        }
        return location;
    }
    
    /**
     * A marker exception type indicating that the line number isn't in the reference type.
     */
    private static class LineNotFoundException extends Exception { }
    
    public EventRequest createEventRequest(EventRequestManager erm) {
        return erm.createBreakpointRequest(getLocation());
    }
    
    public boolean isResolved() {
        return getLocation() != null;
    }
    
    public String toString() {
        return className + ".java:" + lineNumber;
    }
    
    public boolean equals(Object o) {
        if (o instanceof LineBreakpoint) {
            LineBreakpoint b = (LineBreakpoint) o;
            return b.getClassName().equals(className) && b.getLineNumber() == lineNumber;
        } else {
            return false;
        }
    }
    
    public int hashCode() {
        return className.hashCode() | lineNumber;
    }
    
    public int compareTo(Breakpoint b) {
        if (b instanceof ExceptionBreakpoint) {
            // Sort exception breakpoints above line breakpoints
            return 1;
        } else {
            return toString().compareTo(b.toString());
        }
    }
}
