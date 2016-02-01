package e.ptextarea;


/**
 * A PTextListener is something interested in knowing when the contents of a PText model
 * changes.
 * 
 * @author Phil Norman
 */

public interface PTextListener {
    /** Notification that some text has been inserted into the PText. */
    public void textInserted(PTextEvent event);
    
    /** Notification that some text has been removed from the PText. */
    public void textRemoved(PTextEvent event);
    
    /** Notification that all of the text held within the PText object has been completely replaced. */
    public void textCompletelyReplaced(PTextEvent event);
}
