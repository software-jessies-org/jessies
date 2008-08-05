package e.edit;

import e.util.*;

/**
 * Runs RevisionTool.
 */
public class ShowHistoryAction extends ExternalToolAction {
    public ShowHistoryAction() {
        super("Show _History...", ToolInputDisposition.NO_INPUT, ToolOutputDisposition.ERRORS_WINDOW, FileUtilities.findScriptFromBundle("revisiontool", "org.jessies.SCM") + " $EDIT_CURRENT_FILENAME:$EDIT_CURRENT_LINE_NUMBER");
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("H", true));
        setNeedsFile(true);
    }
}
