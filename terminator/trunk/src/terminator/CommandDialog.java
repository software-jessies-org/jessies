package terminator;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import org.jdesktop.swingworker.SwingWorker;
import terminator.view.*;

/**
 * Asks the user for a command to run.
 * Used by the "New Command..." and "New Command Tab..." menu items.
 */
public class CommandDialog {
    private static StringHistory history = new StringHistory(getHistoryFilename());
    
    private FormBuilder form;
    private JTextField commandField;
    private JList historyList;
    
    public CommandDialog() {
        this.commandField = new JTextField(40);
        this.form = new FormBuilder(TerminatorMenuBar.getFocusedTerminatorFrame(), "Run Command");
        
        initHistoryList();
        
        form.setTypingTimeoutActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showMatches();
            }
        });
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Command:", commandField);
        formPanel.addRow("History:", new JScrollPane(historyList));
        form.getFormDialog().setRememberBounds(false);
        form.getFormDialog().setShouldRestoreFocus(false);
    }
    
    private void initHistoryList() {
        historyList = new JList();
        historyList.setCellRenderer(new EListCellRenderer(true));
        
        // If the user double-clicks on a historical command, run it without further ado.
        historyList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = historyList.locationToIndex(e.getPoint());
                    commandField.setText((String) historyList.getModel().getElementAt(index));
                    form.getFormDialog().acceptDialog();
                }
            }
        });
        
        // If the user hits enter while the list has the focus, run the command selected in the list without further ado.
        historyList.getActionMap().put("accept-command", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                commandField.setText((String) historyList.getSelectedValue());
                form.getFormDialog().acceptDialog();
            }
        });
        historyList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "accept-command");
        
        // If the user hits delete while the list has the focus, remove the selected items.
        historyList.getActionMap().put("remove-entry", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                synchronized (history) {
                    for (Object item : historyList.getSelectedValues()) {
                        history.remove((String) item);
                    }
                }
                showMatches();
            }
        });
        historyList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "remove-entry");
    }
    
    private void showMatches() {
        new MatchFinder(commandField.getText()).execute();
    }
    
    private class MatchFinder extends SwingWorker<Object, Object> {
        private String regularExpression;
        private DefaultListModel model;
        private boolean statusGood;
        private String statusText;
        
        private MatchFinder(String regularExpression) {
            this.regularExpression = regularExpression;
        }
        
        @Override
        protected Object doInBackground() {
            model = new DefaultListModel();
            statusGood = true;
            try {
                List<String> unsortedMatches;
                synchronized (history) {
                    unsortedMatches = history.getStringsMatching(regularExpression);
                }
                String[] matches = unsortedMatches.toArray(new String[unsortedMatches.size()]);
                Arrays.sort(matches);
                for (String match : matches) {
                    model.addElement(match);
                }
            } catch (PatternSyntaxException ex) {
                statusGood = false;
                statusText = ex.getDescription();
            }
            return null;
        }
        
        @Override
        public void done() {
            // FIXME: we ought to report regular expression syntax errors (statusText) somewhere.
            commandField.setForeground(statusGood ? UIManager.getColor("TextField.foreground") : Color.RED);
            historyList.setModel(model);
            // If we don't set the selected index, the user won't be able to cycle the focus into the list with the Tab key.
            historyList.setSelectedIndex(0);
        }
    }
    
    public JTerminalPane askForCommandToRun() {
        boolean shouldRun = form.show("Run");
        if (shouldRun == false) {
            return null;
        }
        String command = commandField.getText().trim();
        synchronized (history) {
            history.add(command);
        }
        return JTerminalPane.newCommandWithName(command, null, null);
    }
    
    private static String getHistoryFilename() {
        return System.getProperty("org.jessies.terminator.dotDirectory") + File.separator + "command-history";
    }
}
