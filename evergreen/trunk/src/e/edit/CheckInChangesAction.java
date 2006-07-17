package e.edit;

/**
 * Runs CheckInTool.
 */
public class CheckInChangesAction extends ExternalToolAction {
    public CheckInChangesAction() {
        super("Check in Changes...", ToolInputDisposition.NO_INPUT, ToolOutputDisposition.ERRORS_WINDOW, "cd $EDIT_CURRENT_DIRECTORY ; SCM_EDITOR=evergreen checkintool");
        setChecksEverythingSaved(true);
    }
}
