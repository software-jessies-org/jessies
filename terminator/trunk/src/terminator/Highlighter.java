package terminatorn;

/**

@author Phil Norman
*/

public interface Highlighter {
	public String getName();
	
	/** Request to add highlights to all lines of the view from the index given onwards. */
	public void addHighlights(JTextBuffer view, int firstLineIndex);
}
