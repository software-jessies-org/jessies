package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;
import java.io.*;

/**
 * Our build-project action.
 * We have built-in support for either Ant or make.
 * 
 * As an extension to support weird custom build tools, we look at a couple of special properties.
 * If the build directory path matches the given regular expression, we run the given custom build tool.
 * There may be a better solution, but this is the least worst hack that's come to mind.
 * It's certainly better than having to leave bogus Makefiles all over.
 * We'll need to see at least one other custom build system to get a feel for whether this is generally useful.
 * If it is, we should support an arbitrary number of such custom build tools, similar to the ExternalTool situation.
 */
public class BuildAction extends ETextAction {
    private final boolean test;
    
    private boolean building = false;

    public BuildAction(boolean test) {
        super(test ? "Build and _Test Project" : "_Build Project", GuiUtilities.makeKeyStroke("B", test));
        GnomeStockIcon.useStockIcon(this, "gtk-execute");
        this.test = test;
    }

    public void actionPerformed(ActionEvent e) {
        buildProject();
    }

    private void buildProject() {
        Workspace workspace = Evergreen.getInstance().getCurrentWorkspace();
        if (building) {
            // FIXME: work harder to recognize possibly-deliberate duplicate builds (different targets or makefiles, for example).
            Evergreen.getInstance().showAlert("A target is already being built", "Please wait for the current build to complete before starting another.");
            return;
        }
        
        // Assume we'll be building with GNU Make.
        String command = "make --print-directory";
        
        String makefileName = findMakefile();
        
        // See if we've got special fall-back instructions.
        if (makefileName == null) {
            String pathPattern = Parameters.getString("build.specialPathPattern", null);
            String buildTool = Parameters.getString("build.specialBuildTool", null);
            if (pathPattern != null && buildTool != null) {
                String directory = getMakefileSearchStartDirectory();
                if (directory.matches(pathPattern)) {
                    makefileName = directory + File.separator;
                    command = buildTool;
                }
            }
        }
        
        if (makefileName == null) {
            Evergreen.getInstance().showAlert("Build instructions not found", "Neither a Makefile for make(1) nor a build.xml for Ant could be found.");
            return;
        }
        
        boolean shouldContinue = workspace.prepareForAction("Save before building?", "Some files are currently modified but not saved.");
        if (shouldContinue == false) {
            return;
        }
        
        // Does it look like we should be using Ant?
        if (makefileName.endsWith("build.xml")) {
            command = "ant -emacs -quiet";
        }
        
        invokeBuildTool(workspace, makefileName, command);
    }
    
    private static String getMakefileSearchStartDirectory() {
        ETextWindow focusedTextWindow = getFocusedTextWindow();
        if (focusedTextWindow != null) {
            return focusedTextWindow.getContext();
        } else {
            return Evergreen.getInstance().getCurrentWorkspace().getCanonicalRootDirectory();
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
        return makefileName;
    }
    
    private void invokeBuildTool(Workspace workspace, String makefileName, String command) {
        String makefileDirectoryName = makefileName.substring(0, makefileName.lastIndexOf(File.separatorChar));
        command = addTarget(workspace, command);
        try {
            final ShellCommand shellCommand = new ShellCommand(workspace, command, ToolInputDisposition.NO_INPUT, ToolOutputDisposition.ERRORS_WINDOW);
            shellCommand.setContext(makefileDirectoryName);
            shellCommand.setLaunchRunnable(new Runnable() {
                public void run() {
                    building = true;
                }
            });
            shellCommand.setCompletionRunnable(new Runnable() {
                public void run() {
                    building = false;
                }
            });
            shellCommand.runCommand();
        } catch (IOException ex) {
            Evergreen.getInstance().showAlert("Unable to invoke build tool", "Can't start task (" + ex.getMessage() + ").");
            Log.warn("Couldn't start \"" + command + "\"", ex);
        }
    }
    
    private String addTarget(Workspace workspace, String command) {
        String result = command;
        
        // FIXME: it would be good if we better understood what was being specified here. Personally, I mainly pass "-j".
        String workspaceTarget = workspace.getBuildTarget();
        if (workspaceTarget.length() != 0) {
            result += " " + workspaceTarget;
        }
        // FIXME: does this work for Ant?
        if (test) {
            result += " test";
        }
        
        return result;
    }
}
