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
		if (isBold) {
			// FIXME: disabled for now until I work out how to cope with
			// the foreground/background thing we see below. How do
			// we know the user's chosen a light foreground and so we
			// should use a lighter color rather than a muddy gray?
			//result = opts.getColor("color" + (colourIndex + 8));
		}
		if (result == null) {
			if (colourIndex == BLACK) {
				result = opts.getColor("foreground");
			} else if (colourIndex == WHITE) {
				result = opts.getColor("background");
			}
		}
		if (result == null) {
			result = opts.getColor("color" + colourIndex);
		}
		if (result == null) {
			switch (colourIndex) {
				case BLACK: return Color.BLACK;
				case RED: return Color.RED;
				case GREEN: return Color.GREEN;
				case YELLOW: return Color.YELLOW;
				case BLUE: return Color.BLUE;
				case MAGENTA: return Color.MAGENTA;
				case CYAN: return Color.CYAN;
				case WHITE: return Color.WHITE;
				default: return Color.BLACK;
			}
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
