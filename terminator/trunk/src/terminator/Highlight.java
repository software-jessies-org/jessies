package terminatorn;

import java.awt.*;
import java.util.*;

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
	
	public Highlight(Highlighter highlighter, Location start, Location end, Style style) {
		this.highlighter = highlighter;
		this.start = start;
		this.end = end;
		this.style = style;
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
	
	public Style getStyle() {
		return style;
	}
	
	/**
	* Returns a modified set of styled text regions based upon the set passed in, which are
	* at the given location.  The returned array will be at least as long as the given array.
	*/
	public StyledText[] applyHighlight(StyledText[] unlit, Location unlitStart) {
		ArrayList result = new ArrayList();
		int startOffset = (unlitStart.getLineIndex() == start.getLineIndex()) ? start.getCharOffset() : 0;
		int endOffset = (unlitStart.getLineIndex() == end.getLineIndex()) ? end.getCharOffset() : 0;
		int offset = 0;
		for (int i = 0; i < unlit.length; i++) {
			String unlitText = unlit[i].getText();
			int unlitEnd = offset + unlitText.length();
			if (startOffset <= offset && endOffset >= offset + unlitEnd) {  // unlit[i] completely within highlight.
				result.add(new StyledText(unlitText, applyStyle(unlit[i].getStyle())));
			} else if (startOffset >= unlitEnd || endOffset <= offset) {  // unlit[i] completely outside highlight.
				result.add(unlit[i]);
			} else {  // unlit[i] is partially inside highlight.
				if (startOffset > offset) {  // highlight starts part-way through unlit[i].
					result.add(new StyledText(unlitText.substring(0, startOffset - offset), unlit[i].getStyle()));
				}
				String midText = unlitText.substring(Math.max(0, startOffset - offset),
						Math.min(unlitEnd - offset, endOffset - offset));
				result.add(new StyledText(midText, applyStyle(unlit[i].getStyle())));
				if (endOffset < unlitEnd) {  // highlight ends part-way through unlit[i].
					result.add(new StyledText(unlitText.substring(endOffset - offset), unlit[i].getStyle()));
				}
			}
			offset += unlitText.length();
		}
		return (StyledText[]) result.toArray(new StyledText[result.size()]);
	}
	
	public Style applyStyle(Style other) {
		Color foreground = style.hasForeground() ? style.getForeground() : other.getForeground();
		Color background = style.hasBackground() ? style.getBackground() : other.getBackground();
		boolean isBold = style.hasBold() ? style.isBold() : other.isBold();
		boolean isUnderlined = style.hasUnderlined() ? style.isUnderlined() : other.isUnderlined();
		return new Style(foreground, background, isBold, isUnderlined);
	}
}
