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
	private byte[] tabRecords = null;
	
	private static final byte TAB_NONE = (byte) 0;
	private static final byte TAB_START = (byte) 1;
	private static final byte TAB_CONTINUE = (byte) 2;
	
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
	
	/** Returns the text, with all the tabs put back in for use with clipboard stuff. */
	public String getTabbedText(int start, int end) {
		if (tabRecords == null) {
			return getText().substring(start, end);
		}
		StringBuffer buf = new StringBuffer();
		for (int i = start; i < end; i++) {
			switch (tabRecords[i]) {
				case TAB_NONE: buf.append(text.charAt(i)); break;
				case TAB_START: buf.append('\t'); break;
				case TAB_CONTINUE: break; // Ignore.
			}
		}
		return buf.toString();
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
		styles = trim(styles, startIndex, endIndex);
		if (tabRecords != null) {
			tabRecords = trim(styles, startIndex, endIndex);
			if (tabRecords.length == 0) {
				tabRecords = null;
			}
		}
	}
	
	public void insertTabAt(int offset, int tabLength, int style) {
		insertTextAt(offset, getTabString(tabLength), style);
		recordTabPosition(offset, tabLength);
	}
	
	public void writeTabAt(int offset, int tabLength, int style) {
		writeTextAt(offset, getTabString(tabLength), style);
		recordTabPosition(offset, tabLength);
	}
	
	private void recordTabPosition(int offset, int tabLength) {
		if (tabRecords == null) {
			tabRecords = new byte[styles.length];
		}
		tabRecords[offset] = TAB_START;
		for (int i = 1; i < tabLength; i++) {
			tabRecords[offset + i] = TAB_CONTINUE;
		}
		// If we've overlapped an existing tab, move the old tab's start marker to just after the
		// end of the new tab.
		if (tabRecords.length > offset + tabLength && tabRecords[offset + tabLength] == TAB_CONTINUE) {
			tabRecords[offset + tabLength] = TAB_START;
		}
	}

	private String getTabString(int tabLength) {
		char[] spaces = new char[tabLength];
		Arrays.fill(spaces, ' ');
		return new String(spaces);
	}
	
	/** Inserts text at the given position, moving anything already there further to the right. */
	public void insertTextAt(int offset, String newText, int style) {
		ensureOffsetIsOK(offset);
		styles = insertBytesInto(styles, offset, newText.length(), (byte) style);
		if (tabRecords != null) {
			tabRecords = insertBytesInto(tabRecords, offset, newText.length(), TAB_NONE);
		}
		text = text.substring(0, offset) + newText + text.substring(offset);
	}
	
	/** Writes text at the given position, overwriting anything underneath. */
	public void writeTextAt(int offset, String newText, int style) {
		ensureOffsetIsOK(offset);
		if (offset + newText.length() < styles.length) {
			text = text.substring(0, offset) + newText + text.substring(offset + newText.length());
			Arrays.fill(styles, offset, offset + newText.length(), (byte) style);
			if (tabRecords != null) {
				Arrays.fill(tabRecords, offset, offset + newText.length(), TAB_NONE);
			}
		} else {
			text = text.substring(0, offset) + newText;
			styles = extendWithBytes(styles, offset, newText.length(), (byte) style);
			if (tabRecords != null) {
				tabRecords = extendWithBytes(tabRecords, offset, offset + newText.length(), TAB_NONE);
			}
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
		if (tabRecords != null) {
			tabRecords = insertBytesInto(tabRecords, tabRecords.length, count, TAB_NONE);
		}
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
