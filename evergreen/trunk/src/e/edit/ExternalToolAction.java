package e.edit;

import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import e.forms.*;

public class ExternalToolAction extends ETextAction {
    // FIXME: use an enum in 1.5
    private static final int NO_INPUT_AND_NO_OUTPUT = 0;
    private static final int INPUT_ONLY = 1;
    private static final int OUTPUT_ONLY = 2;
    private static final int INPUT_AND_OUTPUT = 3;
    
    private String commandPattern;
    private int type;
    private boolean checkEverythingSaved;
    private boolean needsFile;
    private boolean requestConfirmation;
    private JTextField commandField;
    private JTextField contextField;
    
    /**
     * Creates a new tool action.
     */
    public ExternalToolAction(String name, String commandPattern) {
        super(name);
        setCommandPattern(commandPattern);
        this.checkEverythingSaved = false;
        this.requestConfirmation = false;
        this.needsFile = false;
    }
    
    private void setCommandPattern(String newPattern) {
        this.type = NO_INPUT_AND_NO_OUTPUT;
        if (newPattern.startsWith("<")) {
            this.type = OUTPUT_ONLY;
            this.needsFile = true;
            newPattern = newPattern.substring(1);
        } else if (newPattern.startsWith(">")) {
            this.type = INPUT_ONLY;
            this.needsFile = true;
            newPattern = newPattern.substring(1);
        } else if (newPattern.startsWith("|")) {
            this.type = INPUT_AND_OUTPUT;
            this.needsFile = true;
            newPattern = newPattern.substring(1);
        }
        this.commandPattern = newPattern;
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
        final ETextWindow textWindow = getFocusedTextWindow();
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
            ShellCommand shellCommand = runCommand(textWindow.getFilename(), textWindow.getCurrentLineNumber(), textWindow.getWorkspace(), textWindow.getContext());
            if (shellCommand != null) {
                shellCommand.setCompletionRunnable(new Runnable() {
                    public void run() {
                        textWindow.updateWatermark();
                        textWindow.repaint();
                    }
                });
            }
        } else {
            Workspace workspace = Edit.getCurrentWorkspace();
            runCommand("", 0, workspace, workspace.getRootDirectory());
        }
    }

    public boolean isContextSensitive() {
        return commandPattern.indexOf("EDIT_") != -1;
    }

    public ShellCommand runCommand(String filename, int lineNumber, Workspace workspace, String context) {
        String command = commandPattern;
        if (requestConfirmation) {
            return confirmRunCommand(filename, lineNumber, workspace, context, command);
        } else {
            return safeRunCommand(filename, lineNumber, workspace, context, command);
        }
    }

    public ShellCommand confirmRunCommand(String filename, int lineNumber, Workspace workspace, String context, String command) {
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
            return safeRunCommand(filename, lineNumber, workspace, context, command);
        } else {
            return null;
        }
    }

    public ShellCommand safeRunCommand(String filename, int lineNumber, Workspace workspace, String context, String command) {
        try {
            ShellCommand shellCommand = new ShellCommand(filename, lineNumber, workspace, context, command);
            shellCommand.runCommand();
            return shellCommand;
        } catch (IOException ex) {
            Edit.showAlert(context, "Can't start task (" + ex.getMessage() + ").");
        }
        return null;
    }
}
