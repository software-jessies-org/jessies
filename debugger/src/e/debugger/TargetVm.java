package e.debugger;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.util.*;

import e.util.*;

/**
 * Wraps a com.sun.jdi.VirtualMachine. Access to all aspects of the target VM, and control
 * over its execution, is achieved directly or indirectly through this class.
 */

public class TargetVm {
    
    private VirtualMachine vm;
    private EventRequestManager eventRequestManager;
    private VmEventQueue eventQueue;
    private BreakpointHandler breakpointHandler;
    
    public TargetVm(final VirtualMachine vm) {
        this.vm = vm;
        this.eventRequestManager = vm.eventRequestManager();
        this.eventQueue = createEventQueue(vm);
        this.breakpointHandler = new BreakpointHandler(this);
        eventQueue.addVmEventListener(new VmEventListener() {
            public Class getEventClass() {
                return VMStartEvent.class;
            }
            // Start the VM running as soon as it's ready.
            public void eventDispatched(Event e) {
                fireVmSuspensionChange(false);
                vm.resume();
            }
        });
    }
    
    public EventRequestManager getEventRequestManager() {
        return eventRequestManager;
    }
    
    public VmEventQueue getEventQueue() {
        return eventQueue;
    }
    
    public BreakpointHandler getBreakpointHandler() {
        return breakpointHandler;
    }
    
    /**
     * Returns a List of all the types matching the given class name that are loaded
     * in the target VM.
     */
    public List<ReferenceType> getClassesByName(String className) {
        return vm.classesByName(className);
    }
    
    /**
     * Returns a List of all the threads currently running in the target VM. Threads that
     * have not been started or have completed execution aren't included in the list.
     */
    public List<ThreadReference> getAllThreads() {
        return vm.allThreads();
    }
    
    public Process getProcess() {
        return vm.process();
    }
    
    private VmEventQueue createEventQueue(final VirtualMachine vm) {
        VmEventQueue eventQueue = new VmEventQueue(vm.eventQueue());
         new Thread(eventQueue, "Target VM EventQueue").start();
        return eventQueue;
    }
    
    /**
     * Listeners to be informed whenever the VM is suspended or resumed.
     */
    private List<VmSuspensionListener> vmSuspensionListeners = new Vector<VmSuspensionListener>();
    
    public void addVmSuspensionListener(VmSuspensionListener l) {
        vmSuspensionListeners.add(l);
    }
    
    public void removeVmSuspensionListener(VmSuspensionListener l) {
        vmSuspensionListeners.remove(l);
    }
    
    public void fireVmSuspensionChange(boolean suspended) {
        synchronized(vmSuspensionListeners) {
            for (VmSuspensionListener l : vmSuspensionListeners) {
                if (suspended) {
                    l.vmSuspended();
                } else {
                    l.vmResumed();
                }
            }
        }
    }
    
    public void setBreakpoint(String className, String lineNumber) {
        breakpointHandler.setBreakpoint(new LineBreakpoint(className, Integer.parseInt(lineNumber)));
    }
    
    public void clearBreakpoint(String className, String lineNumber) {
        breakpointHandler.clearBreakpoint(new LineBreakpoint(className, Integer.parseInt(lineNumber)));
    }
    
    public void setExceptionBreakpoint(String className) {
        breakpointHandler.setBreakpoint(new ExceptionBreakpoint(className));
    }
    
    public void clearExceptionBreakpoint(String className) {
        breakpointHandler.clearBreakpoint(new ExceptionBreakpoint(className));
    }
    
    public void suspend() {
        vm.suspend();
        fireVmSuspensionChange(true);
    }
    
    public void resume() {
        fireVmSuspensionChange(false);
        vm.resume();
    }
}
