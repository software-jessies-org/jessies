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
            boolean shouldContinue = Edit.getInstance().getCurrentWorkspace().prepareForAction("Save before running external tool?", "Some files are currently modified but not saved.");
            if (shouldContinue == false) {
                return;
            }
        }
        
        ShellCommand shellCommand = new ShellCommand(commandPattern);
        if (textWindow != null) {
            shellCommand.setFilename(textWindow.getFilename());
            shellCommand.setLineNumber(textWindow.getCurrentLineNumber());
            shellCommand.setWorkspace(textWindow.getWorkspace());
            shellCommand.setContext(textWindow.getContext());
            shellCommand.setCompletionRunnable(new Runnable() {
                public void run() {
                    textWindow.updateWatermarkAndTitleBar();
                    textWindow.repaint();
                }
            });
        } else {
            shellCommand.setContext(shellCommand.getWorkspace().getRootDirectory());
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
        FormBuilder form = new FormBuilder(Edit.getInstance().getFrame(), (String) getValue(Action.NAME));
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
            Edit.getInstance().showAlert("Couldn't start task", "There was a problem starting the command \"" + shellCommand.getCommand() + "\": " + ex.getMessage() + ".");
        }
    }
}
