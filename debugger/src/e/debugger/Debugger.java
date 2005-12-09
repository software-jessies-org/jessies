package e.debugger;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.event.Event;
import com.sun.jdi.request.*;
import java.awt.*;
import java.awt.EventQueue;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.*;
import org.jdesktop.swingworker.SwingWorker;

import e.gui.*;
import e.util.*;

/**
 * The GUI for the debugger.
 */

public class Debugger extends JComponent implements DebuggerCommandHandler, LocationOpener {
    
    /**
     * The target is connected to outside of the EDT, which is especially important if we're
     * using a ListeningConnector.
     */
    private static final ExecutorService executorService = ThreadUtilities.newSingleThreadExecutor("VM Connector");
    
    TargetVm vm;
    
    private ThreadTree threadTree;
    private JList breakpointList;
    private Component callStackPane;
    private StackFrameList stackFrameList;
    private ProcessMonitorPanel processMonitorPanel;
    private EStatusBar statusBar;
    private JButton suspendButton;
    private List<JButton> stepButtons;
    
    /**
     * Stepping more than one thread at a time gets very confusing,
     * so only one step request is active at a time.
     */
    private StepRequest stepRequest;
    
    public Debugger() {
        createUi();
    }
    
    private void createUi() {
        setLayout(new BorderLayout());
        add(createToolBar(), BorderLayout.NORTH);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setLeftComponent(createThreadTreePanel());
        splitPane.setRightComponent(createTabbedPane());
        add(splitPane, BorderLayout.CENTER);
        add(statusBar = new EStatusBar(), BorderLayout.SOUTH);
        new InAppServer("DebuggerServer", System.getProperty("preferencesDirectory") + File.separator + "/debugger-server-port", DebuggerCommandHandler.class, this);
    }
    
    private Component createTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Output", createProcessMonitorPanel());
        tabbedPane.addTab("Call Stack", callStackPane);
        tabbedPane.addTab("Breakpoints", createBreakpointsPanel());
        return tabbedPane;
    }
    
    private Component createProcessMonitorPanel() {
        processMonitorPanel = new ProcessMonitorPanel();
        return new JScrollPane(processMonitorPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }
    
    private Component createThreadTreePanel() {
        threadTree = new ThreadTree();
        stackFrameList = new StackFrameList(this);
        threadTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                TreePath path = e.getPath();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                ThreadReference thread = (ThreadReference) node.getUserObject();
                try {
                    stackFrameList.setModel(thread.frames());
                } catch (IncompatibleThreadStateException ex) {
                    Log.warn("incompatible thread state.", ex);
                }
            }
        });
        
        final InspectorTree inspectorTree = new InspectorTree();
        stackFrameList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                inspectorTree.setModel(stackFrameList.getSelectedStackFrame());
            }
        });
        
        
        JScrollPane treeScroller = new JScrollPane(threadTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane stackTableScroller = new JScrollPane(stackFrameList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane inspectorTreeScroller = new JScrollPane(inspectorTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        callStackPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, stackTableScroller, inspectorTreeScroller);
        return treeScroller;
    }
    
    private Component createBreakpointsPanel() {
        breakpointList = new BreakpointList(this);
        return new JScrollPane(breakpointList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }
    
    private Component createToolBar() {
        JToolBar p = new JToolBar();
        JButton launchButton = new JButton("Start");
        launchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                connectToVm();
            }
        });
        suspendButton = new JButton("Resume");
        suspendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (suspendButton.getText().equals("Resume")) {
                    vm.resume();
                } else {
                    vm.suspend();
                }
            }
        });
        suspendButton.setEnabled(false);
        
        JButton stepButton = new JButton("Step");
        stepButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                step();
            }
        });
        stepButton.setEnabled(false);
        JButton stepOverButton = new JButton("Step Into");
        stepOverButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stepInto();
            }
        });
        stepOverButton.setEnabled(false);
        stepButton.setEnabled(false);
        JButton stepOutButton = new JButton("Step Out");
        stepOutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stepOut();
            }
        });
        stepOutButton.setEnabled(false);
        
        p.add(launchButton);
        p.add(suspendButton);
        p.add(stepButton);
        p.add(stepOverButton);
        p.add(stepOutButton);
        stepButtons = Arrays.asList(new JButton[] { stepButton, stepOverButton, stepOutButton });
        return p;
    }
    
    public void setStatusText(String text) {
        statusBar.setText(text);
    }
    
    public TargetVm getTargetVm() {
        return vm;
    }
    
    /**
     * Waits for the debug target to be available before enabling the UI.
     */
    private class VmConnectorWorker extends SwingWorker<TargetVm, TargetVm> {
        
        private Map<String, String> args;
        private TargetVm targetVm;
        
        public VmConnectorWorker(Map<String, String> args) {
            this.args = args;
        }
        
        @Override
        protected TargetVm doInBackground() {
            targetVm = new VmConnector().connectToTarget(args);
            return targetVm;
        }
        
        @Override
        protected void done() {
            setTargetVm(targetVm);
        }
    }
    
    private void connectToVm() {
        // FIXME
        Map<String, String> args = new HashMap<String, String>();
        args.put(VmConnector.CONNECTION_TYPE_KEY, VmConnector.CONNECTION_TYPE_LAUNCH);
        args.put(VmConnector.LAUNCH_CLASSPATH, System.getProperty("classpath"));
        args.put(VmConnector.LAUNCH_MAIN_CLASS, System.getProperty("main_class"));
        executorService.execute(new VmConnectorWorker(args));
    }
    
    public void setTargetVm(final TargetVm vm) {
        this.vm = vm;
        
        // Set status text and highlight the location in the editor when a breakpoint is reached.
        vm.getEventQueue().addVmEventListener(new VmEventListener() {
            public Class getEventClass() {
                return BreakpointEvent.class;
            }
            public void eventDispatched(Event e) {
                setEnabled(true);
                BreakpointEvent event = (BreakpointEvent) e;
                Location location = event.location();
                setStatusText("Reached breakpoint at " + location.declaringType().name() + ":" + location.lineNumber());
                threadTree.selectThread(event.thread());
                openLocation(location);
            }
        });
        
        // Requests are associated with a specific VirtualMachine, so the old one won't work any more.
        stepRequest = null;
        
        // Highlight the location in the editor when a step is performed.
        vm.getEventQueue().addVmEventListener(new VmEventListener() {
            public Class getEventClass() {
                return StepEvent.class;
            }
            public void eventDispatched(Event e) {
                setEnabled(true);
                statusBar.clearStatusBar();
                StepEvent event = (StepEvent) e;
                Location location = event.location();
                threadTree.selectThread(event.thread());
                openLocation(location);
            }
        });
        
        // Enable and disable the UI when the target VM is suspended or resumed
        vm.addVmSuspensionListener(new VmSuspensionListener() {
            public void vmSuspended() {
                setEnabled(true);
                setStatusText("Suspended");
            }
            public void vmResumed() {
                setEnabled(false);
                setStatusText("Running...");
            }
        });
        
        // Sort out the UI and begin execution when a VM is ready to start.
        vm.getEventQueue().addVmEventListener(new VmEventListener() {
            public Class getEventClass() {
                return VMStartEvent.class;
            }
            // The VM is launched, and suspended at the start of main(). Let the UI sort itself out.
            public void eventDispatched(Event e) {
                vm.resume();
            }
        });
        
        threadTree.enableThreadEvents(vm);
        BreakpointHandler breakpointHandler = vm.getBreakpointHandler();
        breakpointList.setModel(breakpointHandler);
        // FIXME
        setBreakpoint("e.debugger.TestProgram", "28");
        processMonitorPanel.setProcess(vm.getProcess());
        suspendButton.setEnabled(true);
        setStatusText("Connected to target VM.");
        // Connecting to an already-running VM doesn't send a VMStartEvent.
        vm.resume();
    }
    
    /**
     * When disabled, prevents any threads in the tree or frames in the call stack table being selected
     * while the target VM is running.
     */
    public void setEnabled(boolean enabled) {
        threadTree.setEnabled(enabled);
        stackFrameList.setEnabled(enabled);
        suspendButton.setText(enabled ? "Resume" : "Suspend");
        for (JButton button : stepButtons) {
            button.setEnabled(enabled);
        }
    }
    
    /**
     * Steps the thread that's selected in the thread tree. The step kind should be one
     * of StepRequest.STEP_INTO, StepRequest.STEP_OVER or StepRequest.STEP_OUT.
     */
    public void step(int type) {
        ThreadReference thread = threadTree.getSelectedThread();
        if (thread == null) {
            setStatusText("No thread selected.");
            return;
        }
        EventRequestManager mgr = vm.getEventRequestManager();
        if (stepRequest != null) {
            mgr.deleteEventRequest(stepRequest);
        }
        stepRequest = mgr.createStepRequest(thread, StepRequest.STEP_LINE, type);
        stepRequest.addCountFilter(1);
        stepRequest.enable();
        vm.resume();
    }
    
    // LocationOpener implementation opens the given Location in the editor, and goes to the line.
    public void openLocation(Location location) {
        try {
            String sourcePath = prefixPathTo(location.sourcePath());
            if (sourcePath != null) {
                // FIXME
                String editor = "edit";
                int line = location.lineNumber();
                if (editor.equals("edit")) {
                    // Edit understands more detailed addresses than vim or gvim do.
                    // Open the file with the entire line selected.
                    String command = editor + " " + sourcePath + ":" + line + ":0:" + ++line + ":";
                    ProcessUtilities.spawn(null, new String[] { "bash", "-c", command });
                } else {
                    // Just open the file at the right line.
                    String command = editor + " +" + line + " " + sourcePath;
                    ProcessUtilities.spawn(null, new String[] { "bash", "-c", command });
                }
            }
        } catch (AbsentInformationException ex) {
            setStatusText("No source file information available for this location.");
        }
    }
    
    public String prefixPathTo(String fileName) {
        for (String path : FileUtilities.getArrayOfPathElements(System.getProperty("sourcepath"))) {
            if (FileUtilities.exists(path, fileName)) {
                return path + File.separator + fileName;
            }
        }
        return null;
    }
    
    // DebuggerCommandHandler implementation. These methods are called by the DebugServer
    // in response to connections to the debugger-server-port.
    public void setBreakpoint(String className, String lineNumber) {
        vm.setBreakpoint(className, lineNumber);
    }
    
    public void clearBreakpoint(String className, String lineNumber) {
        vm.clearBreakpoint(className, lineNumber);
    }
    
    public void setExceptionBreakpoint(String className) {
        vm.setExceptionBreakpoint(className);
    }
    
    public void clearExceptionBreakpoint(String className) {
        vm.clearExceptionBreakpoint(className);
    }
    
    public void suspend() {
        vm.suspend();
    }
    
    public void resume() {
        vm.resume();
    }
    
    public void step() {
        step(StepRequest.STEP_OVER);
    }
    
    public void stepInto() {
        step(StepRequest.STEP_INTO);
    }
    
    public void stepOut() {
        step(StepRequest.STEP_OUT);
    }
    
    /**
     * Connect using dt_socket transport to a running process listening at address.
     */
    public void connect(String address) {
        Map<String, String> args = new HashMap<String, String>();
        args.put(VmConnector.CONNECTION_TYPE_KEY, VmConnector.CONNECTION_TYPE_ATTACH);
        args.put(VmConnector.ATTACH_TRANSPORT, VmConnector.ATTACH_TRANSPORT_SOCKET);
        args.put(VmConnector.ATTACH_ADDRESS, address);
        executorService.execute(new VmConnectorWorker(args));
    }
}
