package terminator;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import terminator.view.*;

/**
 * Asks the user for a command to run.
 * Used by the "New Command..." and "New Command Tab..." menu items.
 */
public class CommandDialog {
    private static final StringHistory history = new StringHistory(getHistoryFilename());
    
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
    
    @SuppressWarnings("deprecation") // getSelectedValueList requires Java 7.
    private void initHistoryList() {
        historyList = new JList();
        historyList.setCellRenderer(new EListCellRenderer(true));
        
        // If the user double-clicks on a historical command, or hits enter while the list has focus, run the command without further ado.
        ComponentUtilities.bindDoubleClickAndEnter(historyList, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                commandField.setText((String) historyList.getSelectedValue());
                form.getFormDialog().acceptDialog();
            }
        });
        
        // If the user hits delete while the list has the focus, remove the selected items.
        ComponentUtilities.initKeyBinding(historyList, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), new AbstractAction("remove-entry") {
            public void actionPerformed(ActionEvent e) {
                synchronized (history) {
                    for (Object item : historyList.getSelectedValues()) {
                        history.remove((String) item);
                    }
                }
                showMatches();
            }
        });
        
        ComponentUtilities.divertPageScrollingFromTo(commandField, historyList);
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
                // Usually, we sort matches to make it easier to switch to "eyeball grep" when the list is small.
                // Chris argues that this is effectively a command-line history, and is more useful in chronological order.
                //String[] matches = unsortedMatches.toArray(new String[unsortedMatches.size()]);
                //Arrays.sort(matches);
                for (String match : unsortedMatches) {
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
            // In other applications/dialogs, we set the selected index to 0.
            // Chris Reece makes the argument that if we did that, we'd want to reverse the order of the list entries to have most recent first.
            // He suggests two advantages to leaving the list oldest-first and selecting the last item:
            // 1. it's cheaper (we could work round this if we cared, though).
            // 2. it makes the cursor keys correspond to their behavior in the shell's history, where up moves backwards in time.
            historyList.setSelectedIndex(model.getSize() - 1);
            historyList.ensureIndexIsVisible(historyList.getSelectedIndex());
        }
    }
    
    public JTerminalPane askForCommandToRun() {
        while (form.show("Run")) {
            String command = commandField.getText().trim();
            if (command.isEmpty()) {
                command = (String) historyList.getSelectedValue();
            }
            synchronized (history) {
                history.add(command);
            }
            return JTerminalPane.newCommandWithName(command, null, null);
        }
        return null;
    }
    
    private static String getHistoryFilename() {
        return System.getProperty("org.jessies.terminator.dotDirectory") + File.separator + "command-history";
    }
}
