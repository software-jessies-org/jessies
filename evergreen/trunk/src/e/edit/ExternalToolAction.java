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
        this.needsFile = false;
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
    public void setCheckEverythingSaved(boolean newState) {
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
        final String context = (textWindow != null) ? textWindow.getContext() : workspace.getRootDirectory();
        final ShellCommand shellCommand = workspace.makeShellCommand(textWindow, context, command, inputDisposition, outputDisposition);
        if (textWindow != null) {
            shellCommand.setCompletionRunnable(new Runnable() {
                public void run() {
                    textWindow.updateWatermarkAndTitleBar();
                    textWindow.repaint();
                }
            });
        }
        runCommand(shellCommand);
    }
    
    @Override public boolean isEnabled() {
        return (needsFile == false || getFocusedTextWindow() != null);
    }
    
    private void runCommand(ShellCommand shellCommand) {
        try {
            shellCommand.runCommand();
        } catch (IOException ex) {
            Evergreen.getInstance().showAlert("Couldn't start task", "There was a problem starting the command \"" + shellCommand.getCommand() + "\": " + ex.getMessage() + ".");
        }
    }
}
