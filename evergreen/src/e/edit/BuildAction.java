package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * Implements "Build Project" and "Build and Test Project".
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
    
    public static class BuildTool {
        // The name of the "makefile" (which may be, say, an Ant "build.xml" file).
        String makefileName;
        // The command to build this kind of "makefile".
        String command;
        
        BuildTool(String makefileName, String command) {
            this.makefileName = makefileName;
            this.command = command;
        }
    }
    
    // Returns a map from filenames to build tools.
    private static Map<String, BuildTool> getBuildTools() {
        final HashMap<String, BuildTool> result = new HashMap<String, BuildTool>();
        for (Map.Entry<String, String> tool : Parameters.getStrings("build.").entrySet()) {
            final String key = tool.getKey().substring("build.".length());
            result.put(key, new BuildTool(key, tool.getValue()));
        }
        return result;
    }
    
    private void buildProject() {
        final Workspace workspace = Evergreen.getInstance().getCurrentWorkspace();
        
        // Choosing the build tool relies on the focused text window, so do it before we risk popping up dialogs.
        final BuildTool buildTool = chooseBuildTool();
        if (buildTool == null) {
            // FIXME: list supported kinds of build instructions.
            Evergreen.getInstance().showAlert("Build instructions not found", "No build instructions could be found.");
            return;
        }
        
        if (building) {
            // FIXME: work harder to recognize possibly-deliberate duplicate builds (different targets or makefiles, for example).
            Evergreen.getInstance().showAlert("A target is already being built", "Please wait for the current build to complete before starting another.");
            return;
        }
        final boolean shouldContinue = workspace.prepareForAction("Save before building?", "Some files are currently modified but not saved.");
        if (shouldContinue == false) {
            return;
        }
        
        invokeBuildTool(workspace, buildTool);
    }
    
    private static String getMakefileSearchStartDirectory() {
        final ETextWindow focusedTextWindow = getFocusedTextWindow();
        if (focusedTextWindow != null) {
            return focusedTextWindow.getContext();
        } else {
            return Evergreen.getInstance().getCurrentWorkspace().getCanonicalRootDirectory();
        }
    }
    
    public static BuildTool chooseBuildTool() {
        final Map<String, BuildTool> buildTools = getBuildTools();
        File directory = FileUtilities.fileFromString(getMakefileSearchStartDirectory());
        while (directory != null) {
            for (String filename : directory.list()) {
                final BuildTool buildTool = buildTools.get(filename);
                if (buildTool != null) {
                    buildTool.makefileName = directory.toString() + File.separator + buildTool.makefileName;
                    return buildTool;
                }
            }
            directory = directory.getParentFile();
        }
        return null;
    }
    
    private void invokeBuildTool(Workspace workspace, BuildTool tool) {
        addTarget(workspace, tool);
        try {
            final String directory = FileUtilities.fileFromString(tool.makefileName).getParentFile().toString();
            final ShellCommand shellCommand = workspace.makeShellCommand(null, directory, tool.command, ToolInputDisposition.NO_INPUT, ToolOutputDisposition.ERRORS_WINDOW);
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
            Log.warn("Couldn't start \"" + tool.command + "\"", ex);
        }
    }
    
    private void addTarget(Workspace workspace, BuildTool tool) {
        // FIXME: it would be good if we better understood what was being specified here. Personally, I mainly pass "-j".
        final String workspaceTarget = workspace.getBuildTarget();
        if (workspaceTarget.length() != 0) {
            tool.command += " " + workspaceTarget;
        }
        // FIXME: does this work for Ant?
        if (test) {
            tool.command += " test";
        }
    }
}
