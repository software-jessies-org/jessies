package terminator.model;

import java.util.*;

/**
A TextLine maintains the characters to be printed on a particular line, and the styles to be applied
to each.  It also provides mutator methods for removing and writing text, specifically designed for
use in a virtual terminal.

<p>The text we store internally contains information about tabs.  When text is passed back out to the
outside world, we either convert the tab information to spaces, or to tab characters.  Internally, a tab
is marked as beginning at a '\t' character, and each following character (assuming *all* characters are
the same width) which forms part of the area covered by that tab is denoted by a '\r' character.
We have to internally store all this tab position and length information because tab positions can change
in the outside world at any time, but each TextLine must retain its integrity once the tabs have been
inserted into it.

@author Phil Norman
*/

public class TextLine {
	private int lineStartIndex;
	private String text;
	private byte[] styles;
	
	private static final char TAB_START = '\t';
	private static final char TAB_CONTINUE = '\r';
	
	private static final byte[] EMPTY_STYLES = new byte[0];
	
	public TextLine() {
		clear();
	}
	
	public int getLineStartIndex() {
		return lineStartIndex;
	}
	
	public void setLineStartIndex(int lineStartIndex) {
		this.lineStartIndex = lineStartIndex;
	}
	
	public byte getStyleAt(int index) {
		return styles[index];
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
				result.add(new StyledText(getText().substring(startIndex, i), startStyle));
				startIndex = i;
				startStyle = styles[i];
			}
		}
		result.add(new StyledText(getText().substring(startIndex, styles.length), startStyle));
		return (StyledText[]) result.toArray(new StyledText[result.size()]);
	}
	
	public String getText() {
		return text.replace(TAB_START, ' ').replace(TAB_CONTINUE, ' ');
	}
	
	/** Returns the text, with all the tabs put back in for use with clipboard stuff. */
	public String getTabbedText(int start, int end) {
		StringBuffer buf = new StringBuffer();
		for (int i = start; i < end; i++) {
			char ch = text.charAt(i);
			if (ch != TAB_CONTINUE) {
				buf.append(ch);
			}
		}
		return buf.toString();
	}
	
	public int length() {
		return text.length();
	}
	
	public int lengthIncludingNewline() {
		return length() + 1;
	}
	
	/**
	* Returns the offset of the character specified by charOffset.
	* The returned value will be charOffset for most characters, but may
	* be smaller if the character at charOffset is part of a tab.
	*/
	public int getEffectiveCharStartOffset(int charOffset) {
		if (charOffset >= text.length()) {
			return charOffset;
		}
		for (int i = charOffset; i >= 0; i--) {
			if (text.charAt(i) != TAB_CONTINUE) {
				return i;
			}
		}
		return 0;
	}
	
	/**
	* Returns the offset of the character after that specified by charOffset.
	* The returned value will be charOffset + 1 for most characters, but may
	* be larger if the character at charOffset is part of a tab (after the start).
	*/
	public int getEffectiveCharEndOffset(int charOffset) {
		if (charOffset >= text.length()) {
			return charOffset;
		}
		for (int i = charOffset; i < text.length(); i++) {
			if (text.charAt(i) != TAB_CONTINUE) {
				return i;
			}
		}
		return text.length();
	}
	
	public void clear() {
		text = "";
		styles = EMPTY_STYLES;
	}
	
	public void killText(int startIndex, int endIndex) {
		if (startIndex >= endIndex || startIndex >= text.length()) {
			return;
		}
		endIndex = Math.min(endIndex, text.length());
		text = text.substring(0, startIndex) + text.substring(endIndex);
		styles = trim(styles, startIndex, endIndex);
	}
	
	public void insertTabAt(int offset, int tabLength, int style) {
		insertTextAt(offset, getTabString(tabLength), style);
	}
	
	public void writeTabAt(int offset, int tabLength, int style) {
		writeTextAt(offset, getTabString(tabLength), style);
		// If we've partially overwritten an existing tab, replace the first TAB_CONTINUE
		// character of the existing tab with a TAB_START, so we end up with two tabs,
		// the second slightly shorter than it was, rather than one long tab.
		int indexAfterTab = offset + tabLength;
		if (text.length() > indexAfterTab && text.charAt(indexAfterTab) == TAB_CONTINUE) {
			text = text.substring(0, indexAfterTab) + TAB_START + text.substring(indexAfterTab + 1);
		}
	}

	private String getTabString(int tabLength) {
		char[] spaces = new char[tabLength];
		spaces[0] = TAB_START;
		Arrays.fill(spaces, 1, spaces.length, TAB_CONTINUE);
		return new String(spaces);
	}
	
	/** Inserts text at the given position, moving anything already there further to the right. */
	public void insertTextAt(int offset, String newText, int style) {
		ensureOffsetIsOK(offset);
		styles = insertBytesInto(styles, offset, newText.length(), (byte) style);
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
			styles = extendWithBytes(styles, offset, newText.length(), (byte) style);
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
		styles = insertBytesInto(styles, styles.length, count, (byte) StyledText.getDefaultStyle());
	}
	
	private byte[] extendWithBytes(byte[] bytes, int offset, int count, byte value) {
		byte[] result = new byte[offset + count];
		System.arraycopy(bytes, 0, result, 0, offset);
		Arrays.fill(result, offset, offset + count, (byte) value);
		return result;
	}

	private byte[] insertBytesInto(byte[] bytes, int offset, int count, byte value) {
		byte[] result = new byte[bytes.length + count];
		System.arraycopy(bytes, 0, result, 0, offset);
		Arrays.fill(result, offset, offset + count, (byte) value);
		System.arraycopy(bytes, offset, result, offset + count, bytes.length - offset);
		return result;
	}
	
	private byte[] trim(byte[] bytes, int startIndex, int endIndex) {
		byte[] result = new byte[bytes.length - (endIndex - startIndex)];
		System.arraycopy(bytes, 0, result, 0, startIndex);
		System.arraycopy(bytes, endIndex, result, startIndex, bytes.length - endIndex);
		return result;
	}
}
