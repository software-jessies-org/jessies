package e.edit;

/**
 * Runs CheckInTool.
 */
public class CheckInChangesAction extends ExternalToolAction {
    public CheckInChangesAction() {
        super("Check in Changes...", "cd $EDIT_CURRENT_DIRECTORY ; SCM_EDITOR=edit checkintool");
        setChecksEverythingSaved(true);
    }
}
