package e.ptextarea;


/**
 * A PHighlightMatcher is used to identify PHighlight instances with certain characteristics.
 * It is used with the PTextArea.removeHighlights method to remove all highlights of the same
 * type (eg to remove all search matching highlights).
 * 
 * @author Phil Norman
 */

public interface PHighlightMatcher {
    
    /** Returns true when the given highlight is matched for whatever reason. */
    public boolean matches(PHighlight highlight);
}
