package terminatorn;

import java.util.*;

/**
A TextLine maintains the characters to be printed on a particular line, and the styles to be applied
to each.  It also provides mutator methods for removing and writing text, specifically designed for
use in a virtual terminal.

@author Phil Norman
*/

public class TextLine {
	private int lineStartIndex;
	private String text = "";
	private byte[] styles = new byte[0];
	
	public int getLineStartIndex() {
		return lineStartIndex;
	}
	
	public void setLineStartIndex(int lineStartIndex) {
		this.lineStartIndex = lineStartIndex;
	}
	
	public StyledText[] getStyledTextSegments() {
		if (styles.length == 0) {
			return new StyledText[0];
		}
		ArrayList result = new ArrayList();
		int startIndex = 0;
		byte startStyle = styles[0];
		for (int i = 1; i < styles.length; i++) {
			if (styles[i] != startStyle) {
				result.add(new StyledText(text.substring(startIndex, i), startStyle));
				startIndex = i;
				startStyle = styles[i];
			}
		}
		result.add(new StyledText(text.substring(startIndex, styles.length), startStyle));
		return (StyledText[]) result.toArray(new StyledText[result.size()]);
	}
	
	public String getText() {
		return text;
	}
	
	public int length() {
		return text.length();
	}
	
	public void clear() {
		text = "";
		styles = new byte[0];
	}
	
	public void killText(int startIndex, int endIndex) {
		text = text.substring(0, startIndex) + text.substring(endIndex);
		byte[] newStyles = new byte[styles.length - (endIndex - startIndex)];
		System.arraycopy(styles, 0, newStyles, 0, startIndex);
		System.arraycopy(styles, endIndex, newStyles, startIndex, styles.length - endIndex);
		styles = newStyles;
	}
	
	/** Inserts text at the given position, moving anything already there further to the right. */
	public void insertTextAt(int offset, String newText, int style) {
		ensureOffsetIsOK(offset);
		
		// Extend the styles array and insert copies of the style into it.
		byte[] newStyles = new byte[styles.length + newText.length()];
		System.arraycopy(styles, 0, newStyles, 0, offset);
		Arrays.fill(newStyles, offset, offset + newText.length(), (byte) style);
		System.arraycopy(styles, offset, newStyles, offset + newText.length(), styles.length - offset);
		styles = newStyles;
		
		// Insert the new text into the existing text.
		text = text.substring(0, offset) + newText + text.substring(offset);
	}
	
	/** Writes text at the given position, overwriting anything underneath. */
	public void writeTextAt(int offset, String newText, int style) {
		ensureOffsetIsOK(offset);
		if (offset + newText.length() < styles.length) {
			text = text.substring(0, offset) + newText + text.substring(offset + newText.length());
			Arrays.fill(styles, offset, offset + newText.length(), (byte) style);
		} else {
			text = text.substring(0, offset) + newText;
			byte[] newStyles = new byte[offset + newText.length()];
			System.arraycopy(styles, 0, newStyles, 0, offset);
			Arrays.fill(newStyles, offset, offset + newText.length(), (byte) style);
			styles = newStyles;
		}
	}
	
	private void ensureOffsetIsOK(int offset) {
		if (offset < 0) {
			throw new IllegalArgumentException("Negative offset " + offset);
		}
		if (offset >= text.length()) {
			appendPadding(offset - styles.length);
		}
	}
	
	private void appendPadding(int count) {
		char[] pad = new char[count];
		Arrays.fill(pad, ' ');
		text += new String(pad);
		byte[] newStyles = new byte[styles.length + count];
		System.arraycopy(styles, 0, newStyles, 0, styles.length);
		Arrays.fill(newStyles, styles.length, newStyles.length, (byte) StyledText.getDefaultStyle());
		styles = newStyles;
	}
}
