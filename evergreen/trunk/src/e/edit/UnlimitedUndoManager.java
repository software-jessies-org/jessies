package e.edit;

import javax.swing.undo.*;

/**
 * An UndoManager with no limit to the number of edits it can contain,
 * other than available memory.
 */
public class UnlimitedUndoManager extends UndoManager {
    /**
     * Overrides trimForLimit from UndoManager to do nothing. You should
     * never throw away undo information unless the user asks you to. That
     * happens via discardAllEdits, with which we don't interfere.
     */
    protected void trimForLimit() {
    }
}
