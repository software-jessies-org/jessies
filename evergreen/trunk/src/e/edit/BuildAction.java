package e.edit;

import java.awt.event.*;
import java.io.*;
import e.util.*;

/**
The ETextArea build-project action. Works for either Ant or make.
*/
public class BuildAction extends ETextAction {
    private static final String ACTION_NAME = "Build Project";
    
    private boolean building = false;

    public BuildAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("B", false));
    }

    public void actionPerformed(ActionEvent e) {
        buildProject(getFocusedTextWindow());
    }

    private void buildProject(ETextWindow text) {
        if (building) {
            Edit.getInstance().showAlert(ACTION_NAME, "A target is already being built. Please wait for the build to complete.");
            return;
        }
        
        Workspace workspace = Edit.getInstance().getCurrentWorkspace();
        boolean shouldContinue = workspace.prepareForAction("Build", "Save before building?");
        if (shouldContinue == false) {
            return;
        }
        
        String context;
        if (text != null) {
            context = text.getContext();
        } else {
            try {
                context = workspace.getCanonicalRootDirectory();
            } catch (IOException ex) {
                Edit.getInstance().showAlert(ACTION_NAME, "It's not possible to build this project because the workspace root could not be found (" + ex.getMessage() + ").");
                return;
            }
        }
        
        String makefileName = findMakefile(context);
        if (makefileName == null) {
            Edit.getInstance().showAlert(ACTION_NAME, "It's not possible to build this project because neither a Makefile for make(1) nor a build.xml for Ant could be found.");
        } else if (makefileName.endsWith("build.xml")) {
            invokeBuildTool(workspace, makefileName, "ant -emacs -quiet");
        } else if (makefileName.endsWith("Makefile")) {
            String makeCommand = Parameters.getParameter("make.command", "make") + " --print-directory";
            invokeBuildTool(workspace, makefileName, makeCommand);
        }
    }
    
    public static String findMakefile(String startDirectory) {
        String makefileName = FileUtilities.findFileByNameSearchingUpFrom("build.xml", startDirectory);
        if (makefileName == null) {
            makefileName = FileUtilities.findFileByNameSearchingUpFrom("Makefile", startDirectory);
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
            Edit.getInstance().showAlert(ACTION_NAME, "Can't start task (" + ex.getMessage() + ").");
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
