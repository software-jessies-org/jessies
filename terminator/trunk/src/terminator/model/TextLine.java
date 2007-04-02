package terminator.model;

import java.util.*;

/**
 * Ties together the String containing the characters on a particular line, and the styles to be applied to each character.
 * TextLines are mutable, though it's not possible to change style information without rewriting the corresponding characters (because that's not how terminals work).
 */
public class TextLine {
	// The text we store internally contains information about tabs.
	// When text is passed back out to the outside world, we either convert the tab information to spaces (for the display), or to tab characters (for the clipboard).
	// Internally, a tab is marked as beginning with TAB_START.
	// Each following display position (assuming *all* characters are the same width) covered by the tab is denoted by TAB_CONTINUE.
	// We have to internally store all this tab position and length information because tab positions can change in the outside world at any time, but each TextLine must retain its integrity once the tabs have been inserted into it.
	private static final char TAB_START = '\t';
	private static final char TAB_CONTINUE = '\r';
	
	// The index in characters into the containing buffer of the first character of this line.
	private int lineStartIndex;
	
	// The characters on this line.
	private String text;
	
	// The styles to be applied to the characters on this line.
	// styles == null => all characters use the default style.
	// Otherwise, styles.length == text.length(), and the style information for text.charAt(i) is styles[i].
	private short[] styles;
	
	public TextLine() {
		clear();
	}
	
	public int getLineStartIndex() {
		return lineStartIndex;
	}
	
	public void setLineStartIndex(int lineStartIndex) {
		this.lineStartIndex = lineStartIndex;
	}
	
	public short getStyleAt(int index) {
		return (styles == null) ? StyledText.getDefaultStyle() : styles[index];
	}
	
	public List<StyledText> getStyledTextSegments() {
		final int textLength = text.length();
		if (textLength == 0) {
			return Collections.emptyList();
		}
		String string = getString();
		ArrayList<StyledText> result = new ArrayList<StyledText>();
		int startIndex = 0;
		short startStyle = getStyleAt(0);
		if (styles != null) {
			for (int i = 1; i < textLength; ++i) {
				if (styles[i] != startStyle) {
					result.add(new StyledText(string.substring(startIndex, i), startStyle));
					startIndex = i;
					startStyle = styles[i];
				}
			}
		}
		result.add(new StyledText(string.substring(startIndex, textLength), startStyle));
		return result;
	}
	
	/**
	 * Returns the text of this line with spaces instead of tabs (or,
	 * indeed, instead of the special representation we use internally).
	 * 
	 * This isn't called toString because you need to come here and think
	 * about whether you want this method of getTabbedString instead.
	 */
	public String getString() {
		return text.replace(TAB_START, ' ').replace(TAB_CONTINUE, ' ');
	}
	
	/** Returns the text, with all the tabs put back in for use with clipboard stuff. */
	public String getTabbedString(int start, int end) {
		StringBuilder buf = new StringBuilder();
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
		styles = null;
	}
	
	public void killText(int startIndex, int endIndex) {
		if (startIndex >= endIndex || startIndex >= text.length()) {
			return;
		}
		endIndex = Math.min(endIndex, text.length());
		text = text.substring(0, startIndex) + text.substring(endIndex);
		removeStyleData(startIndex, endIndex);
	}
	
	public void insertTabAt(int offset, int tabLength, short style) {
		insertTextAt(offset, getTabString(tabLength), style);
	}
	
	public void writeTabAt(int offset, int tabLength, short style) {
		writeTextAt(offset, getTabString(tabLength), style);
		// If we've partially overwritten an existing tab, replace the first TAB_CONTINUE
		// character of the existing tab with a TAB_START, so we end up with two tabs,
		// the second slightly shorter than it was, rather than one long tab.
		int indexAfterTab = offset + tabLength;
		if (text.length() > indexAfterTab && text.charAt(indexAfterTab) == TAB_CONTINUE) {
			text = text.substring(0, indexAfterTab) + TAB_START + text.substring(indexAfterTab + 1);
		}
	}
	
	private static String getTabString(int tabLength) {
		char[] spaces = new char[tabLength];
		spaces[0] = TAB_START;
		Arrays.fill(spaces, 1, spaces.length, TAB_CONTINUE);
		return new String(spaces);
	}
	
	/** Inserts text at the given position, moving anything already there further to the right. */
	public void insertTextAt(int offset, String newText, short style) {
		ensureOffsetIsOK(offset);
		text = text.substring(0, offset) + newText + text.substring(offset);
		insertStyleData(offset, newText.length(), style);
	}
	
	/** Writes text at the given position, overwriting anything underneath. */
	public void writeTextAt(int offset, String newText, short style) {
		ensureOffsetIsOK(offset);
		if (offset + newText.length() < text.length()) {
			text = text.substring(0, offset) + newText + text.substring(offset + newText.length());
		} else {
			text = text.substring(0, offset) + newText;
		}
		overwriteStyleData(offset, newText.length(), style);
	}
	
	private void ensureOffsetIsOK(int offset) {
		if (offset < 0) {
			throw new IllegalArgumentException("Negative offset " + offset);
		}
		if (offset >= text.length()) {
			appendPadding(offset - text.length());
		}
	}
	
	private void appendPadding(int count) {
		char[] pad = new char[count];
		Arrays.fill(pad, ' ');
		int oldTextLength = text.length();
		text += new String(pad);
		insertStyleData(oldTextLength, count, StyledText.getDefaultStyle());
	}
	
	private void overwriteStyleData(int offset, int count, short value) {
		if (styles == null && value == StyledText.getDefaultStyle()) {
			return;
		}
		short[] oldStyleData = maybeResizeStyleData();
		if (oldStyleData != null) {
			System.arraycopy(oldStyleData, 0, styles, 0, oldStyleData.length);
		} else {
			Arrays.fill(styles, 0, offset, StyledText.getDefaultStyle());
		}
		Arrays.fill(styles, offset, offset + count, value);
	}
	
	private void insertStyleData(int offset, int count, short value) {
		if (styles == null && value == StyledText.getDefaultStyle()) {
			return;
		}
		short[] oldStyleData = maybeResizeStyleData();
		if (oldStyleData != null) {
			System.arraycopy(oldStyleData, 0, styles, 0, offset);
			Arrays.fill(styles, offset, offset + count, value);
			System.arraycopy(oldStyleData, offset, styles, offset + count, oldStyleData.length - offset);
		} else {
			Arrays.fill(styles, 0, styles.length, StyledText.getDefaultStyle());
			Arrays.fill(styles, offset, offset + count, value);
		}
	}
	
	private void removeStyleData(int startIndex, int endIndex) {
		if (styles == null) {
			return;
		}
		short[] oldStyleData = maybeResizeStyleData();
		System.arraycopy(oldStyleData, 0, styles, 0, startIndex);
		System.arraycopy(oldStyleData, endIndex, styles, startIndex, oldStyleData.length - endIndex);
	}
	
	/**
	 * Ensures that the "styles" array is the right size for the current "text".
	 * You should only call this if you know that the line requires non-default styling.
	 */
	private short[] maybeResizeStyleData() {
		short[] oldStyleData = styles;
		if (styles == null || styles.length != text.length()) {
			styles = new short[text.length()];
		}
		return oldStyleData;
	}
}
