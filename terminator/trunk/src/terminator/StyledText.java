package terminatorn;

import java.awt.*;

/**
A StyledText represents a string of text with one particular style applied.  It also defines in
its namespace the set of allowed styles, and provides static methods for accessing them.

@author Phil Norman
*/

public class StyledText {
	public static final int BLACK = 0;
	public static final int RED = 1;
	public static final int GREEN = 2;
	public static final int YELLOW = 3;
	public static final int BLUE = 4;
	public static final int MAGENTA = 5;
	public static final int CYAN = 6;
	public static final int WHITE = 7;
	
	private static final int BACKGROUND_SHIFT = 3;
	private static final int FOREGROUND_MASK = 7;
	private static final int BACKGROUND_MASK = 7 << BACKGROUND_SHIFT;
	static final int IS_BOLD = 1 << 6;
	static final int IS_UNDERLINED = 1 << 7;

	private String text;
	private Style style;
	private boolean continueToEnd = false;
	
	public StyledText(String text, byte style) {
		this(text, getForegroundColour(style), getBackgroundColour(style), isBold(style), isUnderlined(style));
	}
	
	public StyledText(String text, Color foreground, Color background, boolean isBold, boolean isUnderlined) {
		this(text, new Style(foreground, background, isBold, isUnderlined));
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
	
	public Style getStyle() {
		return style;
	}
	
	public static Color getForegroundColour(int style) {
		return getColour(getForeground(style), isBold(style));
	}
	
	public static Color getBackgroundColour(int style) {
		return getColour(getBackground(style), false);  // Background is never considered to be bold.
	}
	
	public static Color getColour(int colourIndex, boolean isBold) {
		Color result = null;
		Options opts = Options.getSharedInstance();
		boolean isForeground = (colourIndex == BLACK);
		if (isBold) {
			if (isForeground) {
				result = opts.getColor("colorBD");
			} else if (colourIndex < 8) {
				result = opts.getColor("color" + (colourIndex + 8));
			}
		}
		if (result == null) {
			if (isForeground) {
				result = opts.getColor("foreground");
			} else if (colourIndex == WHITE) {
				result = opts.getColor("background");
			}
		}
		if (result == null) {
			result = opts.getColor("color" + colourIndex);
		}
		return result;
	}
	
	public static int getForeground(int style) {
		return style & FOREGROUND_MASK;
	}
	
	public static int getBackground(int style) {
		return (style & BACKGROUND_MASK) >> BACKGROUND_SHIFT;
	}
	
	public static boolean isBold(int style) {
		return (style & IS_BOLD) != 0;
	}
	
	public static boolean isUnderlined(int style) {
		return (style & IS_UNDERLINED) != 0;
	}
	
	public static int getNormalStyle(int colour) {
		return colour;
	}
	
	public static int getDefaultStyle() {
		return getStyle(BLACK, WHITE, false, false);
	}
	
	public static int getStyle(int foreground, int background, boolean isBold, boolean isUnderlined) {
		return foreground | (background << BACKGROUND_SHIFT) |
				(isBold ? IS_BOLD : 0) | (isUnderlined ? IS_UNDERLINED : 0);
	}
	
	public String getDescription() {
		return "FG " + style.getForeground() + ", BG " + style.getBackground() +
				", B=" + style.isBold() + ", U=" + style.isUnderlined();
	}
}
