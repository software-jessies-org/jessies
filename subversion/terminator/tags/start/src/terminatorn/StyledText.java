package terminatorn;

/**
A StyledText represents a string of text with one particular style applied.  It also defines in
its namespace the set of allowed styles, and provides static methods for accessing them.

@author Phil Norman
*/

public class StyledText {
	public static final int BLACK = 0;
	public static final int RED = 1;
	public static final int GREEN = 2;
	public static final int ORANGE = 3;
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
	private int style;
	
	public StyledText(String text, byte style) {
		this.text = text;
		this.style = (int) style;
	}
	
	public String getText() {
		return text;
	}
	
	public int getForeground() {
		return style & FOREGROUND_MASK;
	}
	
	public int getBackground() {
		return (style & BACKGROUND_MASK) >> BACKGROUND_SHIFT;
	}
	
	public boolean isBold() {
		return (style & IS_BOLD) != 0;
	}
	
	public boolean isUnderlined() {
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
}
