package e.edit;

import java.awt.event.*;
import java.io.*;
import e.util.*;

/**
The ETextArea build-project action. Works for either Ant or make.
*/
public class BuildAction extends ETextAction {
    public static final String ACTION_NAME = "Build Project";

    public BuildAction() {
        super(ACTION_NAME);
    }

    public void actionPerformed(ActionEvent e) {
        buildProject(getFocusedTextWindow());
    }

    public void buildProject(ETextWindow text) {
        boolean shouldContinue = Edit.getCurrentWorkspace().prepareForAction("Build", "Save before building?");
        if (shouldContinue == false) {
            return;
        }
        
        String makefileName = FileUtilities.findFileByNameSearchingUpFrom("build.xml", text.getContext());
        if (makefileName != null) {
            invokeBuildTool(text, makefileName, "ant -emacs -quiet");
            return;
        }
        makefileName = FileUtilities.findFileByNameSearchingUpFrom("Makefile", text.getContext());
        if (makefileName != null) {
            String makeCommand = Parameters.getParameter("make.command", "make");
            invokeBuildTool(text, makefileName, makeCommand);
            return;
        }
        Edit.showAlert("Build", "It's not possible to build this project because neither a Makefile for make or a build.xml for Ant could be found.");
    }
    
    public void invokeBuildTool(ETextWindow text, String makefileName, String command) {
        String makefileDirectoryName = makefileName.substring(0, makefileName.lastIndexOf(File.separatorChar));
        command = addTarget(command);
        text.invokeShellCommand(makefileDirectoryName, command);
    }
    
    public String addTarget(String command) {
        String target = Parameters.getParameter("make.target", "");
        if (target.length() == 0) {
            return command;
        }
        return command + " " + target;
    }
}
