package e.ptextarea;

public interface PFindListener {
    /**
     * Invoked when PTextArea.findNext or findPrevious is about to be called.
     * This allows listeners to ensure their find results are kept up to date just in time.
     */
    public void aboutToFind();
}
