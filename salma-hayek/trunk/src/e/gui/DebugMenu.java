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
        JMenu menu = new JMenu("Debug");
        menu.add(new ShowEnvironmentAction());
        menu.add(new ShowSystemPropertiesAction());
        menu.addSeparator();
        menu.add(makeChangeLafMenu());
        menu.add(new ShowUiDefaultsAction());
        menu.addSeparator();
        menu.add(new ListFramesAction());
        menu.add(new ListTimersAction());
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
    
    private static class ShowEnvironmentAction extends AbstractAction {
        public ShowEnvironmentAction() {
            super("Show Environment");
        }
        
        public void actionPerformed(ActionEvent e) {
            JFrameUtilities.showTextWindow(null, "Environment", getEnvironmentAsString());
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
            JFrameUtilities.showTextWindow(null, "System Properties", getSystemPropertiesAsString());
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
                ex.printStackTrace();
            }
        }
    }
    
    private static class ListFramesAction extends AbstractAction {
        public ListFramesAction() {
            super("List Frames/Windows");
        }
        
        public void actionPerformed(ActionEvent e) {
            // FIXME: a table would be much nicer.
            // FIXME: a "Refresh" button would be very useful.
            JFrameUtilities.showTextWindow(null, "Frames/Windows", getFramesAsString());
        }
        
        private String getFramesAsString() {
            // What do Frame and Window and Dialog have in common? Container.
            ArrayList<Container> cs = new ArrayList<Container>();
            cs.addAll(Arrays.asList(Frame.getFrames()));
            
            // Add Java 6's collection of windows/dialogs, without yet requiring Java 6. Equivalent to:
            //cs.addAll(Arrays.asList(Window.getWindows()));
            boolean haveWindows = true;
            try {
                java.lang.reflect.Method getWindowsMethod = Window.class.getDeclaredMethod("getWindows", new Class[] {});
                cs.addAll(Arrays.asList((Window[]) getWindowsMethod.invoke(null, (Object[]) null)));
            } catch (Exception ex) {
                // Ignore. Likely we're on Java 5, where this functionality doesn't exist.
                haveWindows = false;
            }
            
            int nonDisplayableCount = 0;
            StringBuilder builder = new StringBuilder();
            builder.append("Displayable\n===========");
            for (Component c : cs) {
                if (c.isDisplayable()) {
                    builder.append("\n" + c.toString() + "\n");
                } else {
                    ++nonDisplayableCount;
                }
            }
            if (nonDisplayableCount > 0) {
                builder.append("\nNon-displayable\n===============");
                for (Component c : cs) {
                    if (c.isDisplayable() == false) {
                        builder.append("\n" + c.toString() + "\n");
                    }
                }
            }
            if (haveWindows == false) {
                builder.append("\n(Upgrade to Java 6 to get the windows too.)");
            }
            return builder.toString();
        }
    }
    
    private static class ListTimersAction extends AbstractAction {
        public ListTimersAction() {
            super("List Timers");
        }
        
        public void actionPerformed(ActionEvent e) {
            JFrameUtilities.showTextWindow(null, "Timers", getTimersAsString());
        }
        
        private String getTimersAsString() {
            StringBuilder result = new StringBuilder();
            List<Timer> timers = TimerUtilities.getQueuedSwingTimers();
            for (Timer timer : timers) {
                result.append(TimerUtilities.toString(timer));
            }
            if (timers.size() == 0) {
                result.append("(No timers.)");
            }
            return result.toString();
        }
    }
    
    private static class ShowUiDefaultsAction extends AbstractAction {
        public ShowUiDefaultsAction() {
            super("Show UI Defaults");
        }
        
        public void actionPerformed(ActionEvent e) {
            JFrameUtilities.showTextWindow(null, UIManager.getLookAndFeel().getName() + " UI Defaults", getUiDefaultsAsString());
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
            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
            controlPanel.add(gcButton);
            
            JPanel ui = new JPanel(new BorderLayout());
            ui.add(controlPanel, BorderLayout.NORTH);
            ui.add(new HeapView(), BorderLayout.CENTER);
            
            JFrame frame = new JFrame("Heap Usage");
            JFrameUtilities.setFrameIcon(frame);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(ui);
            frame.setSize(new Dimension(400, 200));
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
    }
}
