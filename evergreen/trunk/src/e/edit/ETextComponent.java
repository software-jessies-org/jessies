package e.edit;

import javax.swing.undo.*;

public abstract class ETextComponent extends EWindow {
    public ETextComponent(String filename) {
        super(filename);
    }
    
    /** Returns the undo manager for this window. */
    public abstract UndoManager getUndoManager();
    
    /** Saves the document. */
    public abstract boolean save();
}
