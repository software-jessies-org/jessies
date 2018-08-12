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
    private static final Map<String, BuildTool> buildTools = initBuildTools();
    
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
    
    // Returns a map from filenames to build tools.
    private static Map<String, BuildTool> initBuildTools() {
        final HashMap<String, BuildTool> result = new HashMap<String, BuildTool>();
        result.put("Makefile", new BuildTool("Makefile", "make --print-directory"));
        result.put("build.xml", new BuildTool("build.xml", "ant -emacs -quiet"));
        // TODO: if you have source that only builds with clang, how do you tell meson that?
        result.put("meson.build", new BuildTool("meson.build", "if [ ! -d .out ]; then CXX=clang++ meson .out; fi && ninja -C `realpath .out`"));
        
        // Any user-defined build tools? TODO: does anyone use this?
        for (Map.Entry<String, String> tool : Parameters.getStrings("build.").entrySet()) {
            final String key = tool.getKey().substring("build.".length());
            result.put(key, new BuildTool(key, tool.getValue()));
        }
        return result;
    }
    
    private String getMakefileSearchStartDirectory() {
        final ETextWindow focusedTextWindow = getFocusedTextWindow();
        if (focusedTextWindow != null) {
            return focusedTextWindow.getContext();
        } else {
            return Evergreen.getInstance().getCurrentWorkspace().getCanonicalRootDirectory();
        }
    }
    
    private void buildProject() {
        // Choosing the build tool relies on the focused text window to tell us
        // where to start looking, so do it before we risk popping up dialogs.
        File directory = FileUtilities.fileFromString(getMakefileSearchStartDirectory());
        
        // We search up from the currently-selected source file's directory,
        // stopping at the first match.
        for (; directory != null; directory = directory.getParentFile()) {
            for (String filename : directory.list()) {
                final BuildTool buildTool = buildTools.get(filename);
                if (buildTool != null) {
                    invokeBuildTool(buildTool, directory);
                    return;
                }
            }
        }
        
        Evergreen.getInstance().showAlert("Build instructions not found", "No " + StringUtilities.join(buildTools.keySet(), "/") + " found.");
    }
    
    private void invokeBuildTool(BuildTool tool, File directory) {
        if (building) {
            // FIXME: work harder to recognize possibly-deliberate duplicate builds (different targets or makefiles, for example).
            Evergreen.getInstance().showAlert("A target is already being built", "Please wait for the current build to complete before starting another.");
            return;
        }
        final Workspace workspace = Evergreen.getInstance().getCurrentWorkspace();
        final boolean shouldContinue = workspace.prepareForAction("Save before building?", "Some files are currently modified but not saved.");
        if (shouldContinue == false) {
            return;
        }
        
        tool.invoke(workspace, directory, test, new Runnable() {
            public void run() {
                building = true;
            }
        }, new Runnable() {
            public void run() {
                building = false;
            }
        });
    }
}
