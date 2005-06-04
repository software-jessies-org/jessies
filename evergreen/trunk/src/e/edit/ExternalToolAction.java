package e.edit;

import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import e.forms.*;

public class ExternalToolAction extends ETextAction {
    private enum ToolType {
        NO_INPUT_AND_NO_OUTPUT,
        INPUT_ONLY,
        OUTPUT_ONLY,
        INPUT_AND_OUTPUT;
    }
    
    private String commandPattern;
    private ToolType type;
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
        this.type = ToolType.NO_INPUT_AND_NO_OUTPUT;
        if (newPattern.startsWith("<")) {
            this.type = ToolType.OUTPUT_ONLY;
            this.needsFile = true;
            newPattern = newPattern.substring(1);
        } else if (newPattern.startsWith(">")) {
            this.type = ToolType.INPUT_ONLY;
            this.needsFile = true;
            newPattern = newPattern.substring(1);
        } else if (newPattern.startsWith("|")) {
            this.type = ToolType.INPUT_AND_OUTPUT;
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
        
        // Set defaults.
        String filename = "";
        int lineNumber = 0;
        Workspace workspace = Edit.getCurrentWorkspace();
        String context = workspace.getRootDirectory();
        
        // Override them if we have a focused text window.
        if (textWindow != null) {
            filename = textWindow.getFilename();
            lineNumber = textWindow.getCurrentLineNumber();
            workspace = textWindow.getWorkspace();
            context = textWindow.getContext();
        }
        
        ShellCommand shellCommand = new ShellCommand(filename, lineNumber, workspace, context, commandPattern);
        if (textWindow != null) {
            shellCommand.setCompletionRunnable(new Runnable() {
                public void run() {
                    textWindow.updateWatermark();
                    textWindow.repaint();
                }
            });
        }
        runCommand(shellCommand);
    }

    public boolean isContextSensitive() {
        return commandPattern.contains("EDIT_");
    }

    private void runCommand(ShellCommand shellCommand) {
        if (requestConfirmation) {
            confirmRunCommand(shellCommand);
        } else {
            safeRunCommand(shellCommand);
        }
    }

    private void confirmRunCommand(ShellCommand shellCommand) {
        if (commandField == null) {
            commandField = new JTextField(shellCommand.getCommand(), 40);
            contextField = new JTextField(shellCommand.getContext(), 40);
        }
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Command:", commandField);
        formPanel.addRow("Directory:", contextField);
        boolean shouldRun = FormDialog.show(Edit.getFrame(), (String) getValue(Action.NAME), formPanel, "Run");
        if (shouldRun) {
            shellCommand.setCommand(commandField.getText());
            shellCommand.setContext(contextField.getText());
            safeRunCommand(shellCommand);
        }
    }

    private void safeRunCommand(ShellCommand shellCommand) {
        try {
            shellCommand.runCommand();
        } catch (IOException ex) {
            Edit.showAlert(shellCommand.getContext(), "Can't start task (" + ex.getMessage() + ").");
        }
    }
}
