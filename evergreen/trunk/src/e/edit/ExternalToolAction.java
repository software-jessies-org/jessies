package e.edit;

import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import e.forms.*;

public class ExternalToolAction extends ETextAction {
    private String commandPattern;
    private boolean checkEverythingSaved;
    private boolean needsFile;
    private boolean requestConfirmation;
    private JTextField commandField;
    private JTextField contextField;

    public ExternalToolAction(String name, String commandPattern) {
        super(name);
        this.commandPattern = commandPattern;
        this.checkEverythingSaved = false;
        this.requestConfirmation = false;
        this.needsFile = false;
    }
    
    public void setRequestsConfirmation(boolean newState) {
        this.requestConfirmation = newState;
    }
    
    public void setNeedsFile(boolean newState) {
        this.needsFile = newState;
    }
    
    public void setChecksEverythingSaved(boolean newState) {
        this.checkEverythingSaved = newState;
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow textWindow = getFocusedTextWindow();
        if (needsFile && textWindow == null) {
            return;
        }
        
        if (checkEverythingSaved) {
            boolean shouldContinue = Edit.getCurrentWorkspace().prepareForAction(NAME, "Save unsaved files first?");
            if (shouldContinue == false) {
                return;
            }
        }
        
        if (textWindow != null) {
            runCommand(textWindow.getFilename(), textWindow.getCurrentLineNumber(), textWindow.getWorkspace(), textWindow.getContext());
        } else {
            Workspace workspace = Edit.getCurrentWorkspace();
            runCommand("", 0, workspace, workspace.getRootDirectory());
        }
    }

    public boolean isContextSensitive() {
        return commandPattern.indexOf("EDIT_") != -1;
    }

    public void runCommand(String filename, int lineNumber, Workspace workspace, String context) {
        String command = commandPattern;
        if (requestConfirmation) {
            confirmRunCommand(filename, lineNumber, workspace, context, command);
        } else {
            safeRunCommand(filename, lineNumber, workspace, context, command);
        }
    }

    public void confirmRunCommand(String filename, int lineNumber, Workspace workspace, String context, String command) {
        if (commandField == null) {
            commandField = new JTextField(command, 40);
            contextField = new JTextField(context, 40);
        }
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Command:", commandField);
        formPanel.addRow("Directory:", contextField);
        boolean shouldRun = FormDialog.show(Edit.getFrame(), (String) getValue(Action.NAME), formPanel, "Run");
        command = commandField.getText();
        context = contextField.getText();

        if (shouldRun) {
            safeRunCommand(filename, lineNumber, workspace, context, command);
        }
    }

    public void safeRunCommand(String filename, int lineNumber, Workspace workspace, String context, String command) {
        try {
            new ShellCommand(filename, lineNumber, workspace, false, context, command);
        } catch (IOException ex) {
            Edit.showAlert(context, "Can't start task (" + ex.getMessage() + ").");
        }
    }
}
