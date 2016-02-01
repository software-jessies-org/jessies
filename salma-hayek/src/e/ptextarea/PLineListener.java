package e.ptextarea;


/**
 * A PLineListener is a listener interested in knowing when lines are added, removed, or have their
 * contents changed.
 * 
 * @author Phil Norman
 */

public interface PLineListener {
    /** Notification that one or more lines have been added to the PLineList. */
    public void linesAdded(PLineEvent event);
    
    /** Notification that one or more lines have been removed from the PLineList. */
    public void linesRemoved(PLineEvent event);
    
    /** Notification that one or more lines have had their contents change in the PLineList. */
    public void linesChanged(PLineEvent event);
    
    /** Notification that the entire text of the buffer has been completely replaced. */
    public void linesCompletelyReplaced(PLineEvent event);
}
