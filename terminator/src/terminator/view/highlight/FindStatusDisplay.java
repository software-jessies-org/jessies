package terminator.view.highlight;

public interface FindStatusDisplay {
    /**
     * Invoked when the find status changes.
     */
    public void setStatus(String text, boolean isError);
}
