package e.edit;

import java.awt.event.*;

/**
 * Runs RevisionTool.
 */
public class ShowHistoryAction extends ExternalToolAction {
    public ShowHistoryAction() {
        super("Show History...", "revisiontool $EDIT_CURRENT_FILENAME:$EDIT_CURRENT_LINE_NUMBER");
        putValue(ACCELERATOR_KEY, EditMenuBar.makeKeyStroke("H", true));
        setNeedsFile(true);
    }
}
