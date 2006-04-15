package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;
import java.io.*;

/**
The ETextArea build-project action. Works for either Ant or make.
*/
public class BuildAction extends ETextAction {
    private static final String ACTION_NAME = "Build Project";
    
    private boolean building = false;

    public BuildAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("B", false));
        GnomeStockIcon.useStockIcon(this, "gtk-execute");
    }

    public void actionPerformed(ActionEvent e) {
        buildProject();
    }

    private void buildProject() {
        if (building) {
            Edit.getInstance().showAlert("A target is already being built", "Please wait for the current build to complete before starting another.");
            return;
        }
        
        Workspace workspace = Edit.getInstance().getCurrentWorkspace();
        boolean shouldContinue = workspace.prepareForAction("Save before building?", "Some files are currently modified but not saved.");
        if (shouldContinue == false) {
            return;
        }
        
        String makefileName = findMakefile();
        if (makefileName == null) {
            return;
        }
        
        String command = "make --print-directory";
        if (makefileName.endsWith("build.xml")) {
            command = "ant -emacs -quiet";
        }
        invokeBuildTool(workspace, makefileName, command);
    }
    
    private static String getMakefileSearchStartDirectory() {
        String startDirectory;
        ETextWindow focusedTextWindow = getFocusedTextWindow();
        if (focusedTextWindow != null) {
            return focusedTextWindow.getContext();
        } else {
            try {
                return Edit.getInstance().getCurrentWorkspace().getCanonicalRootDirectory();
            } catch (IOException ex) {
                Edit.getInstance().showAlert("Workspace root not found", "It's not possible to find the build instructions for this project because the workspace root could not be found (" + ex.getMessage() + ").");
                return null;
            }
        }
    }
    
    public static String findMakefile() {
        String startDirectory = getMakefileSearchStartDirectory();
        if (startDirectory == null) {
            return null;
        }
        
        String makefileName = FileUtilities.findFileByNameSearchingUpFrom("Makefile", startDirectory);
        if (makefileName == null) {
            makefileName = FileUtilities.findFileByNameSearchingUpFrom("build.xml", startDirectory);
        }
        if (makefileName == null) {
            Edit.getInstance().showAlert("Build instructions not found", "Neither a Makefile for make(1) nor a build.xml for Ant could be found.");
        }
        return makefileName;
    }
    
    private void invokeBuildTool(Workspace workspace, String makefileName, String command) {
        String makefileDirectoryName = makefileName.substring(0, makefileName.lastIndexOf(File.separatorChar));
        command = addTarget(workspace, command);
        try {
            final ShellCommand shellCommand = new ShellCommand(command);
            shellCommand.setWorkspace(workspace);
            shellCommand.setContext(makefileDirectoryName);
            shellCommand.setLaunchRunnable(new Runnable() {
                public void run() {
                    building = true;
                    Edit.getInstance().showProgressBar(shellCommand.getProcess());
                }
            });
            shellCommand.setCompletionRunnable(new Runnable() {
                public void run() {
                    Edit.getInstance().hideProgressBar();
                    building = false;
                }
            });
            shellCommand.runCommand();
        } catch (IOException ex) {
            Edit.getInstance().showAlert("Unable to invoke build tool", "Can't start task (" + ex.getMessage() + ").");
            Log.warn("Couldn't start \"" + command + "\"", ex);
        }
    }
    
    private String addTarget(Workspace workspace, String command) {
        String target = workspace.getBuildTarget();
        if (target.length() == 0) {
            return command;
        }
        return command + " " + target;
    }
}
