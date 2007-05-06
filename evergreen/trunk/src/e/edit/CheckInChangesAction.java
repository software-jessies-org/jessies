package e.edit;

import e.util.*;

/**
 * Runs CheckInTool.
 */
public class CheckInChangesAction extends ExternalToolAction {
    public CheckInChangesAction() {
        super("Check in Changes...", ToolInputDisposition.NO_INPUT, ToolOutputDisposition.ERRORS_WINDOW, "cd $EDIT_CURRENT_DIRECTORY ; SCM_EDITOR=" + FileUtilities.findScriptFromBundle("evergreen", "org.jessies.Evergreen") + " " + FileUtilities.findScriptFromBundle("checkintool", "org.jessies.SCM"));
        setChecksEverythingSaved(true);
    }
}
