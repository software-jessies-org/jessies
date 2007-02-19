package terminator;

import e.forms.*;
import javax.swing.*;
import terminator.view.*;

/**
 * Asks the user for a command to run.
 * Used by the "New Command..." and "New Command Tab..." menu items.
 */
public class CommandDialog {
    private FormBuilder form;
    private JTextField commandField;
    
    public CommandDialog() {
        this.commandField = new JTextField(40);
        this.form = new FormBuilder(TerminatorMenuBar.getFocusedTerminatorFrame(), "Run Command");
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Command:", commandField);
        form.getFormDialog().setRememberBounds(false);
    }
    
    public JTerminalPane askForCommandToRun() {
        boolean shouldRun = form.show("Run");
        if (shouldRun == false) {
            return null;
        }
        return JTerminalPane.newCommandWithName(commandField.getText(), null, null);
    }
}
