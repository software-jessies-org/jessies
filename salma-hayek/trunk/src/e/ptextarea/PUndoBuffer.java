package e.ptextarea;

/**
 * The current undo buffer for a particular text buffer.
 */
public interface PUndoBuffer {
    /**
     * Starts a new compound edit (which may be nested within another). A
     * compound edit will be undone/redone as a unit.
     */
    public void startCompoundEdit();
    
    /**
     * Finishes the current most-nested compound edit.
     */
    public void finishCompoundEdit();

    /**
     * Tests whether "undo" will actually do anything. Useful for disabling
     * menu items.
     */
    public boolean canUndo();
    
    /**
     * Tests whether "redo" will actually do anything. Useful for disabling
     * menu items.
     */
    public boolean canRedo();
    
    /**
     * Attempt to undo. If there's nothing to undo, this is a no-op.
     */
    public void undo();
    
    /**
     * Attempt to redo. If there's nothing to redo, this is a no-op.
     */
    public void redo();
    
    /**
     * Discards all undo/redo history. Useful if you're reverting to a saved
     * copy, for example, so the history no longer applies to the current
     * content. (Hard to imagine this being useful if you're not replacing
     * the entire content of the text buffer.)
     */
    public void resetUndoBuffer();
    
    /**
     * Adds a change listener, which will be notified when the undo buffer
     * changes. This is probably most useful for calling "canUndo" to determine
     * whether a text should be considered dirty/modified.
     */
    public void addChangeListener(javax.swing.event.ChangeListener listener);
}
