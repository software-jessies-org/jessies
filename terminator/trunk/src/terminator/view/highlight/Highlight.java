package terminator.view.highlight;

import java.awt.*;
import java.util.*;
import java.util.List;

import terminator.model.*;

/**
A Highlight represents a region of text spanning between two Locations in which the text
takes on a different style.  The style is represented by a Style object in which zero or more
of the attributes are set.  Any attributes set within this highlighter overwrite those set within
the underlying text; any which aren't set allow the underlying text's style to show through.

@author Phil Norman
*/

public class Highlight {
	private Highlighter highlighter;
	private Location start;
	private Location end;
	private Style style;
	private Cursor cursor = null;
	
	public Highlight(Highlighter highlighter, Location start, Location end, Style style) {
		this.highlighter = highlighter;
		this.start = start;
		this.end = end;
		this.style = style;
	}
	
	public void setCursor(Cursor cursor) {
		this.cursor = cursor;
	}
	
	public Highlighter getHighlighter() {
		return highlighter;
	}
	
	public Location getStart() {
		return start;
	}
	
	public Location getEnd() {
		return end;
	}
	
	public Cursor getCursor() {
		return cursor;
	}
	
	public String toString() {
		return "Highlight[" + highlighter + " from " + start + " to " + end + "]";
	}

	/**
	* Returns a modified set of styled text regions based upon the set passed in, which are
	* at the given location.  The returned array will be at least as long as the given array.
	*/
	public List<StyledText> applyHighlight(List<StyledText> unlit, Location unlitStart) {
		ArrayList<StyledText> result = new ArrayList<StyledText>();
		int startOffset = (unlitStart.getLineIndex() == start.getLineIndex()) ? start.getCharOffset() : 0;
		int endOffset = (unlitStart.getLineIndex() == end.getLineIndex()) ? end.getCharOffset() : Integer.MAX_VALUE;
		int offset = 0;
		for (StyledText styledText : unlit) {
			String unlitText = styledText.getText();
			int unlitEnd = offset + unlitText.length();
			if (startOffset <= offset && endOffset >= offset + unlitEnd) {  // styledText completely within highlight.
				result.add(new StyledText(unlitText, style.appliedTo(styledText.getStyle())));
			} else if (startOffset >= unlitEnd || endOffset <= offset) {  // styledText completely outside highlight.
				result.add(styledText);
			} else {  // styledText is partially inside highlight.
				if (startOffset > offset) {  // highlight starts part-way through styledText.
					result.add(new StyledText(unlitText.substring(0, startOffset - offset), styledText.getStyle()));
				}
				String midText = unlitText.substring(Math.max(0, startOffset - offset),
						Math.min(unlitEnd - offset, endOffset - offset));
				result.add(new StyledText(midText, style.appliedTo(styledText.getStyle())));
				if (endOffset < unlitEnd) {  // highlight ends part-way through styledText.
					result.add(new StyledText(unlitText.substring(endOffset - offset), styledText.getStyle()));
				}
			}
			offset += unlitText.length();
		}
		
		// If the highlight extends past the end of this line, we highlight up to the RHS
		// of the screen.  Used for multi-line selections.
		if (end.getLineIndex() > unlitStart.getLineIndex()) {
			// If we're on an empty line, we need to add a zero-length entry to hold the
			// style to be propagated to the RHS.  Also, if the highlight starts after the end
			// of the visible text, we need to do the same.
			if ((result.size() == 0) || highlightStartsAtEndOfLine(unlit, unlitStart)) {
				StyledText empty = new StyledText("", StyledText.getDefaultStyle());
				result.add(new StyledText("", style.appliedTo(empty.getStyle())));
			}
			result.get(result.size() - 1).setContinueToEnd(true);
		}
		
		return result;
	}
	
	/** Returns true if the highlight's first character is just after the end of the line. */
	private boolean highlightStartsAtEndOfLine(List<StyledText> unlit, Location unlitStart) {
		if (start.getLineIndex() < unlitStart.getLineIndex()) {
			return false;
		}
		int totalLength = 0;
		for (StyledText styledText : unlit) {
			totalLength += styledText.getText().length();
		}
		return (start.getCharOffset() == totalLength);
	}
}
