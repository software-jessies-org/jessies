package e.gui;

import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

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
        menu.add(new ListFramesAction());
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
    
    private static void showTextWindow(String title, String content) {
        PTextArea textArea = new PTextArea(40, 80);
        textArea.setFont(new Font(GuiUtilities.getMonospacedFontName(), Font.PLAIN, 10));
        textArea.setText(content);
        showScrollableContentWindow(title, textArea);
    }
    
    private static void showScrollableContentWindow(String title, JComponent content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        
        JFrame frame = new JFrame(title);
        frame.setContentPane(scrollPane);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
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
            showTextWindow("Environment", getEnvironmentAsString());
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
            showTextWindow("System Properties", getSystemPropertiesAsString());
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
            super("List Frames");
        }
        
        public void actionPerformed(ActionEvent e) {
            // FIXME: a table would be much nicer.
            showTextWindow("Frames", getFramesAsString());
        }
        
        private String getFramesAsString() {
            StringBuilder builder = new StringBuilder();
            for (Frame frame : Frame.getFrames()) {
                builder.append(frame.toString() + "\n");
            }
            return builder.toString();
        }
    }
}
