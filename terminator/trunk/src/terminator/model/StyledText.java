package terminator.model;

import java.awt.*;
import terminator.*;

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
	
	private static final String[] COLOR_DESCRIPTIONS = new String[] {
		"Black",
		"Red",
		"Green",
		"Yellow",
		"Blue",
		"Magenta",
		"Cyan",
		"White",
	};
	
	private static final int BACKGROUND_SHIFT = 3;
	private static final int FOREGROUND_MASK = 7;
	private static final int BACKGROUND_MASK = 7 << BACKGROUND_SHIFT;
	static final int IS_BOLD = 1 << 6;
	static final int IS_UNDERLINED = 1 << 7;

	private String text;
	private Style style;
	private boolean continueToEnd = false;
	
	public StyledText(String text, byte style) {
		this(text, getForegroundColor(style), getBackgroundColor(style), isBold(style), isUnderlined(style));
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
	
	public int length() {
		return text.length();
	}
	
	public Style getStyle() {
		return style;
	}
	
	public static Color getForegroundColor(int style) {
		return getColor(getForeground(style), isBold(style));
	}
	
	public static Color getBackgroundColor(int style) {
		return getColor(getBackground(style), false);  // Background is never considered to be bold.
	}
	
	public static String getForegroundColorName(int style) {
		return getColorName(getForeground(style), isBold(style));
	}
	
	public static String getBackgroundColorName(int style) {
		return getColorName(getBackground(style), isBold(style));
	}
	
	public static String getColorName(int colorIndex, boolean isBold) {
		return "color" + (colorIndex + (isBold ? 8 : 0));
	}
	
	public static String getForegroundColorDescription(int style) {
		return getColorDescription(getForeground(style), isBold(style));
	}
	
	public static String getBackgroundColorDescription(int style) {
		return getColorDescription(getBackground(style), isBold(style));
	}
	
	public static String getColorDescription(int colorIndex, boolean isBold) {
		if (isBold) {
			return COLOR_DESCRIPTIONS[colorIndex] + " (Bold)";
		} else {
			return COLOR_DESCRIPTIONS[colorIndex];
		}
	}
	
	public static Color getColor(int colorIndex, boolean isBold) {
		Color result = null;
		Options opts = Options.getSharedInstance();
		boolean isForeground = (colorIndex == BLACK);
		if (isBold) {
			if (isForeground) {
				result = opts.getColor("colorBD");
			} else if (colorIndex < 8) {
				result = opts.getColor("color" + (colorIndex + 8));
			}
		}
		if (result == null) {
			if (isForeground) {
				result = opts.getColor("foreground");
			} else if (colorIndex == WHITE) {
				result = opts.getColor("background");
			}
		}
		if (result == null) {
			result = opts.getColor("color" + colorIndex);
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
	
	public static int getNormalStyle(int color) {
		return color;
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
