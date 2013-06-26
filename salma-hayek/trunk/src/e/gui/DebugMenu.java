package e.gui;

import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

/**
 * A "Debug" menu for any Java application.
 */
public class DebugMenu {
    public static JMenu makeJMenu() {
        JMenu menu = GuiUtilities.makeMenu("Debugging Tools", 'D');
        menu.add(new ShowDebuggingMessagesAction());
        menu.addSeparator();
        menu.add(new ShowEnvironmentAction());
        menu.add(new ShowSystemPropertiesAction());
        menu.addSeparator();
        menu.add(makeChangeLafMenu());
        menu.add(new ShowUiDefaultsAction());
        menu.addSeparator();
        menu.add(new ShowFramesAction());
        menu.add(new ShowSwingTimersAction());
        menu.add(new ShowStopwatchesAction());
        menu.addSeparator();
        menu.add(new KeyEventTester());
        menu.add(new MouseEventTester());
        menu.addSeparator();
        menu.add(new HeapViewAction());
        // FIXME: an action to turn on debugging of hung AWT exits. All frames or just the parent frame? Just the parent is probably the more obvious (given that new frames could be created afterwards).
        return menu;
    }
    
    private static JMenu makeChangeLafMenu() {
        JMenu menu = GuiUtilities.makeMenu("Look And Feel", 'L');
        UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo laf : lafs) {
            menu.add(new ChangeLookAndFeelAction(laf.getName(), laf.getClassName()));
        }
        return menu;
    }
    
    private DebugMenu() {
    }
    
    private static String sortedStringOfMap(Map<String, String> hash) {
        StringBuilder builder = new StringBuilder();
        String[] keys = hash.keySet().toArray(new String[hash.size()]);
        Arrays.sort(keys);
        for (String key : keys) {
            builder.append(key + "=" + hash.get(key) + "\n");
        }
        return builder.toString();
    }
    
    private static class ShowDebuggingMessagesAction extends AbstractAction {
        private final String logFilename = System.getProperty("e.util.Log.filename");
        
        public ShowDebuggingMessagesAction() {
            putValue(NAME, "Show Debugging Messages");
        }
        
        public boolean isEnabled() {
            return (logFilename != null);
        }
        
        public void actionPerformed(ActionEvent e) {
            // We used to use WebLinkAction with a file: URL so that the user's default text viewer would be used.
            // Sadly, on Windows, another process can't open the log file while we've got it open for writing, so we have to read it ourselves.
            
            final PTextArea textArea = JFrameUtilities.makeTextArea("");
            
            // A refresh button saves the user a lot of hassle.
            // (Automatically refreshing can be annoying if the log's changing while the user's trying to do something.)
            JButton refreshButton = new JButton("Refresh");
            refreshButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.getTextBuffer().readFromFile(FileUtilities.fileFromString(logFilename));
                }
            });
            refreshButton.doClick(0);
            
            showFrameWithButtonPanel(Log.getApplicationName() + " Debugging Messages", makeButtonPanel(refreshButton), new JScrollPane(textArea), new Dimension(400, 200));
        }
    }
    
    private static class ShowEnvironmentAction extends AbstractAction {
        public ShowEnvironmentAction() {
            GuiUtilities.configureAction(this, "Show _Environment", null);
        }
        
        public void actionPerformed(ActionEvent e) {
            JFrameUtilities.showTextWindow(null, Log.getApplicationName() + " Environment", getEnvironmentAsString());
        }
        
        private String getEnvironmentAsString() {
            return sortedStringOfMap(System.getenv());
        }
    }
    
    private static class ShowSystemPropertiesAction extends AbstractAction {
        public ShowSystemPropertiesAction() {
            GuiUtilities.configureAction(this, "Show System _Properties", null);
        }
        
        public void actionPerformed(ActionEvent e) {
            // FIXME: we can edit the system properties; should we expose this?
            JFrameUtilities.showTextWindow(null, Log.getApplicationName() + " System Properties", getSystemPropertiesAsString());
        }
        
        private String getSystemPropertiesAsString() {
            return sortedStringOfMap(getSystemProperties());
        }
        
        private Map<String, String> getSystemProperties() {
            HashMap<String, String> result = new HashMap<String, String>();
            Properties properties = System.getProperties();
            Enumeration<?> propertyNames = properties.propertyNames();
            while (propertyNames.hasMoreElements()) {
                String key = (String) propertyNames.nextElement();
                result.put(key, StringUtilities.escapeForJava(properties.getProperty(key)));
            }
            return result;
        }
    }
    
    private static class ChangeLookAndFeelAction extends AbstractAction {
        private String lafClassName;
        
        public ChangeLookAndFeelAction(String name, String lafClassName) {
            super(name);
            this.lafClassName = lafClassName;
        }
        
        public void actionPerformed(ActionEvent e) {
            changeLookAndFeel();
        }
        
        private void changeLookAndFeel() {
            try {
                UIManager.setLookAndFeel(lafClassName);
                for (Frame frame : Frame.getFrames()) {
                    SwingUtilities.updateComponentTreeUI(frame);
                }
            } catch (Exception ex) {
                SimpleDialog.showDetails(null, "Failed to change LAF", ex);
            }
        }
    }
    
    private static class ShowFramesAction extends AbstractAction {
        public ShowFramesAction() {
            GuiUtilities.configureAction(this, "Show _Frames/Windows", null);
        }
        
        public void actionPerformed(ActionEvent e) {
            // FIXME: a table would be much nicer.
            // FIXME: a "Refresh" button would be very useful.
            JFrameUtilities.showTextWindow(null, Log.getApplicationName() + " Frames/Windows", getFramesAsString());
        }
        
        private String getFramesAsString() {
            // What do Dialog, Frame, and Window all have in common? Window.
            ArrayList<Window> windows = new ArrayList<Window>();
            
            // Add Java 6's collection of dialogs/frames/windows, without yet requiring Java 6. Equivalent to:
            //windows.addAll(Arrays.asList(Window.getWindows()));
            boolean haveWindows = true;
            try {
                java.lang.reflect.Method getWindowsMethod = Window.class.getDeclaredMethod("getWindows");
                windows.addAll(Arrays.asList((Window[]) getWindowsMethod.invoke(null, (Object[]) null)));
            } catch (Exception ex) {
                // Ignore. Likely we're on Java 5, where this functionality doesn't exist.
                haveWindows = false;
                // We can get the Frames, though. (Every Frame is a Window, so Java 6 users already have the these.)
                windows.addAll(Arrays.asList(Frame.getFrames()));
            }
            
            int nonDisplayableCount = 0;
            StringBuilder builder = new StringBuilder();
            builder.append("Displayable\n===========");
            int i = 0;
            for (Window window : windows) {
                if (window.isDisplayable()) {
                    describeWindow(builder, i, window);
                    ++i;
                } else {
                    ++nonDisplayableCount;
                }
            }
            if (nonDisplayableCount > 0) {
                builder.append("\nNon-displayable\n===============");
                i = 0;
                for (Window window : windows) {
                    if (window.isDisplayable() == false) {
                        describeWindow(builder, i, window);
                        ++i;
                    }
                }
            }
            if (haveWindows == false) {
                builder.append("\n(Upgrade to Java 6 to get the windows too.)");
            }
            return builder.toString();
        }
        
        private static void describeWindow(StringBuilder sb, int i, Window window) {
            sb.append("\n");
            sb.append(i);
            sb.append(": ");
            // Work around the fact that there's no Window.getTitle method.
            if (window instanceof Frame) {
                sb.append("\"");
                sb.append(((Frame) window).getTitle());
                sb.append("\": ");
            } else if (window instanceof Dialog) {
                sb.append("\"");
                sb.append(((Dialog) window).getTitle());
                sb.append("\": ");
            }
            sb.append(window.toString());
            sb.append("\n");
        }
    }
    
    private static class ShowSwingTimersAction extends AbstractAction {
        public ShowSwingTimersAction() {
            GuiUtilities.configureAction(this, "Show Swing _Timers", null);
        }
        
        public void actionPerformed(ActionEvent e) {
            JFrameUtilities.showTextWindow(null, Log.getApplicationName() + " Swing Timers", getTimersAsString());
        }
        
        private String getTimersAsString() {
            StringBuilder result = new StringBuilder();
            List<Timer> timers = TimerUtilities.getQueuedSwingTimers();
            for (Timer timer : timers) {
                result.append(TimerUtilities.toString(timer));
            }
            if (timers.size() == 0) {
                result.append("(No Swing timers.)");
            }
            return result.toString();
        }
    }
    
    private static class ShowStopwatchesAction extends AbstractAction {
        public ShowStopwatchesAction() {
            GuiUtilities.configureAction(this, "Show Stop_watches", null);
        }
        
        public void actionPerformed(ActionEvent e) {
            // FIXME: really, we want a table. It would be good to be able to reset a stopwatch too.
            JFrameUtilities.showTextWindow(null, Log.getApplicationName() + " Stopwatches", Stopwatch.toStringAll());
        }
    }
    
    private static class ShowUiDefaultsAction extends AbstractAction {
        public ShowUiDefaultsAction() {
            GuiUtilities.configureAction(this, "Show _UI Defaults", null);
        }
        
        public void actionPerformed(ActionEvent e) {
            JFrameUtilities.showTextWindow(null, Log.getApplicationName() + " " + UIManager.getLookAndFeel().getName() + " UI Defaults", getUiDefaultsAsString());
        }
        
        private String getUiDefaultsAsString() {
            ArrayList<String> list = new ArrayList<String>();
            UIDefaults defaults = UIManager.getLookAndFeelDefaults();
            for (Enumeration<Object> e = defaults.keys(); e.hasMoreElements();) {
                Object key = e.nextElement();
                list.add(key + "=" + defaults.get(key) + "\n");
            }
            Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
            StringBuilder result = new StringBuilder();
            for (String line : list) {
                result.append(line);
            }
            return result.toString();
        }
    }
    
    private static class HeapViewAction extends AbstractAction {
        public HeapViewAction() {
            GuiUtilities.configureAction(this, "Show _Heap Usage", null);
        }
        
        private class GcAction extends AbstractAction {
            public GcAction() {
                GuiUtilities.configureAction(this, "_Collect", null);
            }
            
            public void actionPerformed(ActionEvent e) {
                System.gc();
            }
        }
        
        private class HistogramAction extends AbstractAction {
            public HistogramAction() {
                GuiUtilities.configureAction(this, "_Histogram", null);
                // You need a Java 6 (or later) JDK.
                // (A JRE is insufficient, but I don't know how to recognize one.)
                // On Windows, ProcessUtilities.getVmProcessId doesn't return a pid we can give to jmap.
                setEnabled(System.getProperty("java.specification.version").equals("1.5") == false && GuiUtilities.isWindows() == false);
            }
            
            public void actionPerformed(ActionEvent e) {
                JFrameUtilities.showTextWindow(null, Log.getApplicationName() + " Heap Histogram", getHeapHistogram());
            }
        }
        
        public void actionPerformed(ActionEvent e) {
            JButton gcButton = new JButton(new GcAction());
            JButton histogramButton = new JButton(new HistogramAction());
            ELabel currentHeapUsageLabel = new ELabel();
            JPanel buttonPanel = makeButtonPanel(gcButton, histogramButton, Box.createHorizontalStrut(10), currentHeapUsageLabel);
            
            JFrame frame = showFrameWithButtonPanel(Log.getApplicationName() + " Heap Usage", buttonPanel, new HeapView(currentHeapUsageLabel), new Dimension(400, 200));
            frame.setResizable(false);
        }
        
        public String getHeapHistogram() {
            String[] command = new String[] {
                "jmap", "-histo:live", Integer.toString(ProcessUtilities.getVmProcessId())
            };
            ArrayList<String> lines = new ArrayList<String>();
            ProcessUtilities.backQuote(null, command, lines, lines);
            return StringUtilities.join(lines, "\n");
        }
    }
    
    private static class KeyEventTester extends AbstractAction {
        public KeyEventTester() {
            GuiUtilities.configureAction(this, "_Key Event Tester", null);
        }
        
        public void actionPerformed(ActionEvent e) {
            final EventListener listener = new EventListener();
            
            final JTextField textField = new JTextField(20);
            textField.addKeyListener(listener);
            
            JButton clearButton = new JButton("Clear");
            clearButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    listener.textArea.setText("");
                    textField.setText("");
                    textField.requestFocusInWindow();
                }
            });
            
            showFrameWithButtonPanel("Key Event Tester", makeButtonPanel(clearButton, Box.createHorizontalStrut(10), textField), listener.scrollableTextArea, new Dimension(700, 400));
            textField.requestFocusInWindow();
        }
    }
    
    private static class MouseEventTester extends AbstractAction {
        public MouseEventTester() {
            GuiUtilities.configureAction(this, "_Mouse Event Tester", null);
        }
        
        public void actionPerformed(ActionEvent e) {
            final EventListener listener = new EventListener();
            
            final JPanel mouseArea = new JPanel();
            mouseArea.setBackground(new Color(240, 255, 240));
            mouseArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            mouseArea.setOpaque(true);
            mouseArea.setMinimumSize(new Dimension(100, 100));
            mouseArea.addMouseListener(listener);
            mouseArea.addMouseMotionListener(listener);
            mouseArea.addMouseWheelListener(listener);
            
            JButton clearButton = new JButton("Clear");
            clearButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    listener.textArea.setText("");
                }
            });
            
            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mouseArea, listener.scrollableTextArea);
            showFrameWithButtonPanel("Mouse Event Tester", makeButtonPanel(clearButton), splitPane, new Dimension(700, 400));
        }
    }
    
    private static class EventListener implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
        private PTextArea textArea;
        private JScrollPane scrollableTextArea;
        
        private EventListener() {
            textArea = new PTextArea();
            textArea.setEditable(false);
            scrollableTextArea = new JScrollPane(textArea);
            GuiUtilities.keepMaximumShowing(scrollableTextArea.getVerticalScrollBar());
        }
        
        private void reportEvent(Object e) {
            textArea.append(e.toString().replaceAll(" on javax\\.swing\\.J.*$", "") + "\n");
        }
        
        // KeyListener.
        public void keyTyped(KeyEvent e) { reportEvent(e); }
        public void keyPressed(KeyEvent e) { reportEvent(e); }
        public void keyReleased(KeyEvent e) { reportEvent(e); }
        
        // MouseListener.
        public void mouseClicked(MouseEvent e) { reportEvent(e); }
        public void mouseEntered(MouseEvent e) { reportEvent(e); }
        public void mouseExited(MouseEvent e) { reportEvent(e); }
        public void mousePressed(MouseEvent e) { reportEvent(e); }
        public void mouseReleased(MouseEvent e) { reportEvent(e); }
        
        // MouseMotionListener.
        public void mouseDragged(MouseEvent e) { reportEvent(e); }
        public void mouseMoved(MouseEvent e) { reportEvent(e); }
        
        // MouseWheelListener.
        public void mouseWheelMoved(MouseWheelEvent e) { reportEvent(e); }
    }
    
    private static JFrame showFrameWithButtonPanel(String title, Component buttonPanel, Component content, Dimension size) {
        JPanel ui = new JPanel(new BorderLayout());
        ui.add(buttonPanel, BorderLayout.NORTH);
        ui.add(content, BorderLayout.CENTER);
        
        JFrame frame = new JFrame(title);
        frame.setContentPane(ui);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setSize(size);
        JFrameUtilities.constrainToScreen(frame);
        JFrameUtilities.setFrameIcon(frame);
        frame.setVisible(true);
        return frame;
    }
    
    private static JPanel makeButtonPanel(Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        for (Component c : components) {
            panel.add(c);
        }
        return panel;
    }
}
