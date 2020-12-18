package e.edit;

import e.util.*;
import java.io.*;
import java.nio.file.*;

public class BuildTool {
    // The name of the "makefile" (which may be, say, an Ant "build.xml" file).
    private final String makefileName;
    
    // The command to build this kind of "makefile".
    private final String command;
    
    public BuildTool(String makefileName, String command) {
        this.makefileName = makefileName;
        this.command = command;
    }
    
    public void invoke(Workspace workspace, Path directory, boolean test, Runnable onLaunch, Runnable onCompletion) {
        String command = this.command;
                
        // FIXME: it would be good if we better understood what was being specified here. Personally, I mainly pass "-j".
        final String workspaceTarget = workspace.getBuildTarget();
        if (workspaceTarget.length() != 0) {
            command += " " + workspaceTarget;
        }
        // FIXME: does this work for Ant?
        if (test) {
            command += " test";
        }
        
        try {
            final ShellCommand shellCommand = workspace.makeShellCommand(null, directory.toString(), command, ToolInputDisposition.NO_INPUT, ToolOutputDisposition.ERRORS_WINDOW);
            shellCommand.setLaunchRunnable(onLaunch);
            shellCommand.setCompletionRunnable(onCompletion);
            shellCommand.runCommand();
        } catch (IOException ex) {
            Evergreen.getInstance().showAlert("Unable to invoke build tool", "Can't start task (" + ex.getMessage() + ").");
            Log.warn("Couldn't start \"" + command + "\"", ex);
        }
    }
}
