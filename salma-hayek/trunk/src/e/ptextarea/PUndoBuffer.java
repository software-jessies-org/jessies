package e.ptextarea;


public interface PUndoBuffer {
    public boolean canUndo();
    public boolean canRedo();
    public void undo();
    public void redo();
}
