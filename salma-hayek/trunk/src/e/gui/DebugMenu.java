package e.gui;

import e.ptextarea.*;
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
        return menu;
    }
    
    private DebugMenu() {
    }
    
    private static class ShowEnvironmentAction extends AbstractAction {
        public ShowEnvironmentAction() {
            super("Show Environment");
        }
        
        public void actionPerformed(ActionEvent e) {
            PTextArea textArea = new PTextArea();
            textArea.setText(getEnvironmentAsString());
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setBorder(null);
            
            JFrame frame = new JFrame("Environment");
            frame.setContentPane(scrollPane);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(true);
        }
        
        private String getEnvironmentAsString() {
            StringBuilder builder = new StringBuilder();
            Map<String, String> env = System.getenv();
            String[] keys = env.keySet().toArray(new String[env.size()]);
            Arrays.sort(keys);
            for (String key : keys) {
                builder.append(key + "=" + env.get(key) + "\n");
            }
            return builder.toString();
        }
    }
}
