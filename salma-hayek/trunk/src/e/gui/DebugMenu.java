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
        JMenu menu = new JMenu("Debugging Tools");
        menu.add(new ShowLogAction());
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
        menu.addSeparator();
        menu.add(new HeapViewAction());
        // FIXME: an action to turn on debugging of hung AWT exits. All frames or just the parent frame? Just the parent is probably the more obvious (given that new frames could be created afterwards).
        return menu;
    }
    
    private static JMenu makeChangeLafMenu() {
        JMenu menu = new JMenu("Look And Feel");
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
    
    private static class ShowLogAction extends AbstractAction {
        private final String logFilename = System.getProperty("e.util.Log.filename");
        
        public ShowLogAction() {
            putValue(NAME, "Show Debugging Messages");
        }
        
        public boolean isEnabled() {
            return (logFilename != null);
        }
        
        public void actionPerformed(ActionEvent e) {
            // We used to use WebLinkAction with a file: URL so that the user's default text viewer would be used.
            // Sadly, on Win32, another process can't open the log file while we've got it open for writing.
            
            // FIXME: we could use the file alteration monitor to update as the log gets written to. Or at least have a refresh button.
            JFrameUtilities.showTextWindow(null, Log.getApplicationName() + " Debugging Messages", StringUtilities.readFile(logFilename));
        }
    }
    
    private static class ShowEnvironmentAction extends AbstractAction {
        public ShowEnvironmentAction() {
            super("Show Environment");
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
            super("Show System Properties");
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
            super("Show Frames/Windows");
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
                java.lang.reflect.Method getWindowsMethod = Window.class.getDeclaredMethod("getWindows", new Class[0]);
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
            for (Window window : windows) {
                if (window.isDisplayable()) {
                    builder.append("\n" + window.toString() + "\n");
                } else {
                    ++nonDisplayableCount;
                }
            }
            if (nonDisplayableCount > 0) {
                builder.append("\nNon-displayable\n===============");
                for (Window window : windows) {
                    if (window.isDisplayable() == false) {
                        builder.append("\n" + window.toString() + "\n");
                    }
                }
            }
            if (haveWindows == false) {
                builder.append("\n(Upgrade to Java 6 to get the windows too.)");
            }
            return builder.toString();
        }
    }
    
    private static class ShowSwingTimersAction extends AbstractAction {
        public ShowSwingTimersAction() {
            super("Show Swing Timers");
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
            super("Show Stopwatches");
        }
        
        public void actionPerformed(ActionEvent e) {
            // FIXME: really, we want a table. It would be good to be able to reset a stopwatch too.
            JFrameUtilities.showTextWindow(null, Log.getApplicationName() + " Stopwatches", Stopwatch.toStringAll());
        }
    }
    
    private static class ShowUiDefaultsAction extends AbstractAction {
        public ShowUiDefaultsAction() {
            super("Show UI Defaults");
        }
        
        public void actionPerformed(ActionEvent e) {
            JFrameUtilities.showTextWindow(null, Log.getApplicationName() + " " + UIManager.getLookAndFeel().getName() + " UI Defaults", getUiDefaultsAsString());
        }
        
        private String getUiDefaultsAsString() {
            ArrayList<String> list = new ArrayList<String>();
            UIDefaults defaults = UIManager.getLookAndFeelDefaults();
            for (Enumeration e = defaults.keys(); e.hasMoreElements();) {
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
            super("Show Heap Usage");
        }
        
        public void actionPerformed(ActionEvent e) {
            JButton gcButton = new JButton("System.gc()");
            gcButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.gc();
                }
            });
            JButton histogramButton = new JButton("Histogram");
            histogramButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFrameUtilities.showTextWindow(null, Log.getApplicationName() + " Heap Histogram", getHeapHistogram());
                }
            });
            histogramButton.setEnabled(System.getProperty("java.version").startsWith("1.5") == false);
            JLabel currentHeapUsageLabel = new JLabel(" ");
            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
            controlPanel.add(gcButton);
            controlPanel.add(histogramButton);
            controlPanel.add(Box.createHorizontalStrut(10));
            controlPanel.add(currentHeapUsageLabel);
            
            JPanel ui = new JPanel(new BorderLayout());
            ui.add(controlPanel, BorderLayout.NORTH);
            ui.add(new HeapView(currentHeapUsageLabel), BorderLayout.CENTER);
            
            JFrame frame = new JFrame(Log.getApplicationName() + " Heap Usage");
            JFrameUtilities.setFrameIcon(frame);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(ui);
            frame.setSize(new Dimension(400, 200));
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
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
            super("Key Event Tester");
        }
        
        public void actionPerformed(ActionEvent e) {
            final PTextArea textArea = new PTextArea();
            textArea.setEditable(false);
            
            final JTextField textField = new JTextField(20);
            textField.addKeyListener(new KeyListener() {
                private void append(KeyEvent e) {
                    textArea.append(e.toString().replaceAll(" on javax.swing.JTextField\\[.*$", "") + "\n");
                }
                
                public void keyTyped(KeyEvent e) {
                    append(e);
                }
                
                public void keyPressed(KeyEvent e) {
                    append(e);
                }
                
                public void keyReleased(KeyEvent e) {
                    append(e);
                }
            });
            
            JButton clearButton = new JButton("Clear");
            clearButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.setText("");
                    textField.setText("");
                    textField.requestFocusInWindow();
                }
            });
            
            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
            controlPanel.add(clearButton);
            controlPanel.add(Box.createHorizontalStrut(10));
            controlPanel.add(textField);
            
            JPanel ui = new JPanel(new BorderLayout());
            ui.add(controlPanel, BorderLayout.NORTH);
            ui.add(new JScrollPane(textArea), BorderLayout.CENTER);
            
            JFrame frame = new JFrame("Key Event Tester");
            JFrameUtilities.setFrameIcon(frame);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(ui);
            frame.setSize(new Dimension(700, 400));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            textField.requestFocusInWindow();
        }
    }
    
}
