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
        putValue(ACCELERATOR_KEY, EditMenuBar.makeKeyStroke("B", false));
    }

    public void actionPerformed(ActionEvent e) {
        buildProject(getFocusedTextWindow());
    }

    public void buildProject(ETextWindow text) {
        boolean shouldContinue = Edit.getCurrentWorkspace().prepareForAction("Build", "Save before building?");
        if (shouldContinue == false) {
            return;
        }
        
        String makefileName = findMakefile(text.getContext());
        if (makefileName == null) {
            Edit.showAlert("Build", "It's not possible to build this project because neither a Makefile for make or a build.xml for Ant could be found.");
        } else if (makefileName.endsWith("build.xml")) {
            invokeBuildTool(text, makefileName, "ant -emacs -quiet");
        } else if (makefileName.endsWith("Makefile")) {
            String makeCommand = Parameters.getParameter("make.command", "make");
            invokeBuildTool(text, makefileName, makeCommand);
        }
    }
    
    public static String findMakefile(String startDirectory) {
        String makefileName = FileUtilities.findFileByNameSearchingUpFrom("build.xml", startDirectory);
        if (makefileName == null) {
            makefileName = FileUtilities.findFileByNameSearchingUpFrom("Makefile", startDirectory);
        }
        return makefileName;
    }
    
    public void invokeBuildTool(ETextWindow text, String makefileName, String command) {
        String makefileDirectoryName = makefileName.substring(0, makefileName.lastIndexOf(File.separatorChar));
        command = addTarget(command);
        text.invokeShellCommand(makefileDirectoryName, command, true);
    }
    
    public String addTarget(String command) {
        String target = Parameters.getParameter("make.target", "");
        if (target.length() == 0) {
            return command;
        }
        return command + " " + target;
    }
}
