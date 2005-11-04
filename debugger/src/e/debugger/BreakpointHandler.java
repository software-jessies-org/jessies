package e.debugger;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;
import java.util.*;
import javax.swing.*;

import e.util.*;

/**
 * Handles the setting and clearing of breakpoints.
 */

public class BreakpointHandler extends AbstractListModel {
    
    private TargetVm vm;
    
    /**
     * Resolved, active breakpoints and invalid, unresolvable breakpoints,
     * sorted into table display order.
     */
    private Set<Breakpoint> breakpoints = new TreeSet<Breakpoint>();
    
    /**
     * Breakpoints can't be set until the corresponding class is loaded by the target VM.
     * Breakpoints which can't be resolved straight away are held until the class is loaded.
     */
    private Set<Breakpoint> pending = new TreeSet<Breakpoint>();
    
    /**
     * Keep the breakpoint requests so that they can be disabled again when the
     * breakpoint is cleared.
     */
    private Map<Breakpoint, EventRequest> breakpointRequests = new HashMap<Breakpoint, EventRequest>();
    
    /**
     * Only require one class prepare event for all the unresolved breakpoints in that class.
     */
    private Map<String, ClassPrepareRequest> classPrepareRequests = new HashMap<String, ClassPrepareRequest>();
    
    public BreakpointHandler(final TargetVm vm) {
        this.vm = vm;
        VmEventQueue eventQueue = vm.getEventQueue();
        // Unresolved pending breakpoints can only be set when the classes they refer to
        // are loaded into the debugged VM.
        eventQueue.addVmEventListener(new VmEventListener() {
            public Class getEventClass() {
                return ClassPrepareEvent.class;
            }
            public void eventDispatched(Event e) {
                classPrepared((ClassPrepareEvent) e);
                vm.resume();
            }
        });
    }
    
    /**
     * Sets a breakpoint. If the breakpoint can't be resolved right away, defer the setting
     * until the relevant class is loaded.
     */
    public void setBreakpoint(Breakpoint breakpoint) {
        if (breakpoint.resolve(vm)) {
            enableBreakpoint(breakpoint);
        } else {
            deferBreakpoint(breakpoint);
        }
        showContents();
    }
    
    /**
     * Clears a breakpoint.
     */
    public void clearBreakpoint(Breakpoint breakpoint) {
        pending.remove(breakpoint);
        breakpoints.remove(breakpoint);
        if (breakpointRequests.containsKey(breakpoint)) {
            EventRequest breakpointRequest = breakpointRequests.remove(breakpoint);
            vm.getEventRequestManager().deleteEventRequest(breakpointRequest);
        }
        fireContentsChanged(this, 0, getSize());
        showContents();
    }
    
    private void enableBreakpoint(Breakpoint breakpoint) {
        EventRequestManager eventManager = vm.getEventRequestManager();
        EventRequest request = breakpoint.createEventRequest(eventManager);
        request.enable();
        breakpointRequests.put(breakpoint, request);
        breakpoints.add(breakpoint);
        fireContentsChanged(this, 0, getSize());
    }
    
    private void deferBreakpoint(Breakpoint breakpoint) {
        EventRequestManager eventManager = vm.getEventRequestManager();
        if (classPrepareRequests.containsKey(breakpoint.getClassName()) == false) {
            ClassPrepareRequest classPrepareRequest = eventManager.createClassPrepareRequest();
            classPrepareRequest.addClassFilter(breakpoint.getClassName());
            classPrepareRequest.enable();
            classPrepareRequests.put(breakpoint.getClassName(), classPrepareRequest);
        }
        pending.add(breakpoint);
        fireContentsChanged(this, 0, getSize());
    }
    
    /**
     * Enables resolvable breakpoints when the corresponding class is loaded.
     * Removes all matching breakpoints from the pending list, whether or not
     * they were successfully resolved.
     */
    private void classPrepared(ClassPrepareEvent e) {
        ReferenceType type = e.referenceType();
        List<Breakpoint> processed = new ArrayList<Breakpoint>();
        synchronized(pending) {
            for (Breakpoint breakpoint : pending) {
                if (type.name().equals(breakpoint.getClassName())) {
                    if (breakpoint.resolve(vm)) {
                        enableBreakpoint(breakpoint);
                    } else {
                        // The breakpoint wasn't resolvable. Leave it in the table, so
                        // that the UI can show a disabled breakpoint rather than
                        // silently discarding it.
                        breakpoints.add(breakpoint);
                    }
                    processed.add(breakpoint);
                }
            }
            pending.removeAll(processed);
        }
        ClassPrepareRequest cpr = classPrepareRequests.remove(type.name());
        vm.getEventRequestManager().deleteEventRequest(cpr);
        showContents();
    }
    
    private void showContents() {
        System.err.println("active = " + breakpoints);
        System.err.println("pending = " + pending);
        System.err.println("active requests = " + breakpointRequests.values());
        System.err.println("awaiting class load = " + classPrepareRequests.keySet());
    }
    
    //
    // ListModel stuff.
    //
    public int getSize() {
        return breakpoints.size() + pending.size();
    }
    
    public Object getElementAt(int row) {
        if (row < breakpoints.size()) {
            return breakpoints.toArray()[row];
        } else {
            return pending.toArray()[row - breakpoints.size()];
        }
    }
}
