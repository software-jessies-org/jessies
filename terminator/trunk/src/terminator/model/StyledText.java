package terminator.model;

import java.awt.*;
import terminator.*;

/**
A StyledText represents a string of text with one particular style applied.  It also defines in
its namespace the set of allowed styles, and provides static methods for accessing them.

@author Phil Norman
*/

public class StyledText {
	private static final int BACKGROUND_SHIFT = 3;
	private static final int FOREGROUND_MASK = 7;
	private static final int BACKGROUND_MASK = 7 << BACKGROUND_SHIFT;
	
	private static final int IS_BOLD = 1 << 6;
	private static final int IS_UNDERLINED = 1 << 7;
	private static final int IS_REVERSE_VIDEO = 1 << 8;
	
	private static final int HAS_FOREGROUND = 1 << 14;
	private static final int HAS_BACKGROUND = 1 << 15;
	
	private String text;
	private Style style;
	private boolean continueToEnd = false;
	
	public StyledText(String text, short style) {
		this(text, new Style(getForegroundColor(style), getBackgroundColor(style), Boolean.valueOf(isBold(style)), Boolean.valueOf(isUnderlined(style)), isReverseVideo(style)));
	}
	
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
	
	public static Color getForegroundColor(int style) {
		return getColor(getForeground(style), isBold(style), true);
	}
	
	public static Color getBackgroundColor(int style) {
		return getColor(getBackground(style), false, false);  // Background is never considered to be bold.
	}
	
	private static Color getColor(int colorIndex, boolean isBold, boolean isForeground) {
		Color result = null;
		Options opts = Options.getSharedInstance();
		if (isBold && isForeground) {
			if (colorIndex == -1 && isForeground) {
				result = opts.getColor("colorBD");
			} else if (colorIndex < 8) {
				result = opts.getColor("color" + (colorIndex + 8));
			}
		}
		if (result == null && colorIndex == -1) {
			result = opts.getColor(isForeground ? "foreground" : "background");
		}
		if (result == null) {
			result = opts.getColor("color" + colorIndex);
		}
		return result;
	}
	
	public static boolean hasBackground(int style) {
		return ((style & HAS_BACKGROUND) != 0);
	}
	
	public static boolean hasForeground(int style) {
		return ((style & HAS_FOREGROUND) != 0);
	}
	
	public static int getForeground(int style) {
		return hasForeground(style) ? (style & FOREGROUND_MASK) : -1;
	}
	
	public static int getBackground(int style) {
		return hasBackground(style) ? ((style & BACKGROUND_MASK) >> BACKGROUND_SHIFT) : -1;
	}
	
	public static boolean isBold(int style) {
		return (style & IS_BOLD) != 0;
	}
	
	public static boolean isUnderlined(int style) {
		return (style & IS_UNDERLINED) != 0;
	}
	
	public static boolean isReverseVideo(int style) {
		return (style & IS_REVERSE_VIDEO) != 0;
	}
	
	public static short getDefaultStyle() {
		return getStyle(-1, false, -1, false, false, false, false);
	}
	
	public static short getStyle(int foreground, boolean hasForeground, int background, boolean hasBackground, boolean isBold, boolean isUnderlined, boolean isReverseVideo) {
		return (short) ((foreground & FOREGROUND_MASK) | ((background << BACKGROUND_SHIFT) & BACKGROUND_MASK) | (isBold ? IS_BOLD : 0) | (isUnderlined ? IS_UNDERLINED : 0) | (hasForeground ? HAS_FOREGROUND : 0) | (hasBackground ? HAS_BACKGROUND : 0) | (isReverseVideo ? IS_REVERSE_VIDEO : 0));
	}
	
	public String getDescription() {
		return "StyledText[foreground=" + style.getForeground() + ",background=" + style.getBackground() + ",bold=" + style.isBold() + ",underlined=" + style.isUnderlined() + ",reverse=" + style.isReverseVideo() + "]";
	}
}
