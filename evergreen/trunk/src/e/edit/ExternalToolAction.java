package e.edit;

import e.forms.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class ExternalToolAction extends ETextAction {
    private String command;
    private ToolInputDisposition inputDisposition;
    private ToolOutputDisposition outputDisposition;
    private boolean checkEverythingSaved;
    private boolean needsFile;
    private boolean requestConfirmation;
    
    private JTextField commandField;
    private JTextField contextField;
    
    /**
     * Creates a new tool action.
     */
    public ExternalToolAction(String name, ToolInputDisposition inputDisposition, ToolOutputDisposition outputDisposition, String command) {
        super(name, null);
        this.inputDisposition = inputDisposition;
        this.outputDisposition = outputDisposition;
        this.command = command;
        this.checkEverythingSaved = false;
        this.requestConfirmation = false;
        this.needsFile = false;
    }
    
    /**
     * Sets whether or not the user will be asked to confirm the running of this command. Defaults to false.
     */
    public void setRequestsConfirmation(boolean newState) {
        this.requestConfirmation = newState;
    }
    
    /**
     * Sets whether or not this command will only run with a file selected. Defaults to false.
     */
    public void setNeedsFile(boolean newState) {
        this.needsFile = newState;
    }
    
    /**
     * Sets whether or not this command will warn the user if there are unsaved files in the workspace before running. Defaults to false.
     */
    public void setChecksEverythingSaved(boolean newState) {
        this.checkEverythingSaved = newState;
    }
    
    public void actionPerformed(ActionEvent e) {
        final ETextWindow textWindow = getFocusedTextWindow();
        if (needsFile && textWindow == null) {
            return;
        }
        
        if (checkEverythingSaved) {
            boolean shouldContinue = Evergreen.getInstance().getCurrentWorkspace().prepareForAction("Save before running external tool?", "Some files are currently modified but not saved.");
            if (shouldContinue == false) {
                return;
            }
        }
        
        final Workspace workspace = (textWindow != null) ? textWindow.getWorkspace() : Evergreen.getInstance().getCurrentWorkspace();
        final ShellCommand shellCommand = new ShellCommand(workspace, command, inputDisposition, outputDisposition);
        if (textWindow != null) {
            shellCommand.setTextWindow(textWindow);
            shellCommand.setContext(textWindow.getContext());
            shellCommand.setCompletionRunnable(new Runnable() {
                public void run() {
                    textWindow.updateWatermarkAndTitleBar();
                    textWindow.repaint();
                }
            });
        } else {
            shellCommand.setContext(workspace.getRootDirectory());
        }
        runCommand(shellCommand);
    }
    
    public boolean isEnabled() {
        return (needsFile == false || getFocusedTextWindow() != null);
    }
    
    public boolean isContextSensitive() {
        return needsFile || command.contains("EDIT_");
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
        FormBuilder form = new FormBuilder(Evergreen.getInstance().getFrame(), (String) getValue(Action.NAME));
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Command:", commandField);
        formPanel.addRow("Directory:", contextField);
        boolean shouldRun = form.show("Run");
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
            Evergreen.getInstance().showAlert("Couldn't start task", "There was a problem starting the command \"" + shellCommand.getCommand() + "\": " + ex.getMessage() + ".");
        }
    }
}
