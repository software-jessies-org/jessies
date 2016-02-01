package e.edit;

/**
Defines the interface implemented by components interested in hyperlink clicks.
*/
public interface LinkListener {
    /** Invoked when a link is clicked on, and passed the text of the link. */
    public void linkActivated(String link);
}
