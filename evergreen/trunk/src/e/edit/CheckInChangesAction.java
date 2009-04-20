package e.edit;

import e.util.*;

/**
 * Runs CheckInTool.
 */
public class CheckInChangesAction extends ExternalToolAction {
    public CheckInChangesAction() {
        super("Check in _Changes...", ToolInputDisposition.NO_INPUT, ToolOutputDisposition.ERRORS_WINDOW, "cd $EVERGREEN_CURRENT_DIRECTORY ; SCM_EDITOR=" + Evergreen.getApplicationFilename() + " " + FileUtilities.findScriptFromBundle("checkintool", "org.jessies.SCM"));
        setCheckEverythingSaved(true);
    }
    
    @Override public boolean isEnabled() {
        return !Evergreen.getInstance().getWorkspaces().isEmpty() && super.isEnabled();
    }
}
