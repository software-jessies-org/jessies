package terminator.model;

import java.awt.*;
import terminator.*;

/**
 * Represents a run of text with one particular style applied.
 */
public class StyledText {
	private String text;
	private Style style;
	private boolean continueToEnd = false;
	
	public StyledText(String text, Style style) {
		this.text = text;
		this.style = style;
	}
	
	public void setContinueToEnd(boolean continueToEnd) {
		this.continueToEnd = continueToEnd;
	}
	
	public boolean continueToEnd() {
		return continueToEnd;
	}
	
	public String getText() {
		return text;
	}
	
	public int length() {
		return text.length();
	}
	
	public Style getStyle() {
		return style;
	}
}
