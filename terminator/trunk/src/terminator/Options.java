package terminator;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import e.util.*;

/**
 * Reads in settings from the file system and makes them conveniently
 * available.
 * 
 * There's a grand tradition amongst Unix terminal emulators of pretending
 * to be xterm, and we aren't any different. We go one step further by
 * pretending to be an X11 application and reading .Xdefaults and .Xresources.
 * Strictly, most X11 applications won't do this, but rxvt does, rather than
 * using the bloated library routines.
 * 
 * We don't pass .Xdefaults and .Xresources through m4, as X11 does, but I've
 * not seen that used since people had monochrome as well as color displays and
 * needed different settings on each. We do understand X11's idiosyncratic
 * comment style, and that should be enough for most resource files seen in
 * the real world.
 * 
 * All settings should have a default, so that users can use "--help" to see
 * every available option.
 */
public class Options {
	private static final Options INSTANCE = new Options();
	
	private static final String ANTI_ALIAS = "antiAlias";
	private static final String FONT_NAME = "fontName";
	private static final String FONT_SIZE = "fontSize";
	private static final String INITIAL_COLUMN_COUNT = "initialColumnCount";
	private static final String INITIAL_ROW_COUNT = "initialRowCount";
	private static final String INTERNAL_BORDER = "internalBorder";
	private static final String LOGIN_SHELL = "loginShell";
	private static final String SCROLL_KEY = "scrollKey";
	private static final String SCROLL_TTY_OUTPUT = "scrollTtyOutput";
	private static final String TITLE = "title";
	private static final String USE_MENU_BAR = "useMenuBar";
	
	private final Pattern resourcePattern = Pattern.compile("(?:(?:XTerm|Rxvt|Terminator)(?:\\*|\\.))?(\\S+):\\s*(.+)");
	
	private HashMap options = new HashMap();
	private HashMap descriptions = new HashMap();
	private HashMap rgbColors = null;
	
	public static Options getSharedInstance() {
		return INSTANCE;
	}
	
	public void showOptions(PrintStream out) {
		Object[] keys = options.keySet().toArray();
		Arrays.sort(keys);
		for (int i = 0; i < keys.length; ++i) {
			String key = (String) keys[i];
			String description = (String) descriptions.get(key);
			if (description != null) {
				out.println("\n# " + description);
			}
			out.println("Terminator*" + key + ": " + options.get(key));
		}
	}
	
	/**
	 * Whether or not the shells we start should be login shells.
	 */
	public boolean isLoginShell() {
		return booleanResource(LOGIN_SHELL);
	}
	
	/**
	 * Whether or not pressing a key should cause the the scrollbar to go
	 * to the bottom of the scrolling region.
	 */
	public boolean isScrollKey() {
		return booleanResource(SCROLL_KEY);
	}
	
	/**
	 * Whether or not output to the terminal should cause the scrollbar to
	 * go to the bottom of the scrolling region.
	 */
	public boolean isScrollTtyOutput() {
		return booleanResource(SCROLL_TTY_OUTPUT);
	}
	
	/**
	 * Whether or not to anti-alias text.
	 */
	public boolean isAntiAliased() {
		return booleanResource(ANTI_ALIAS);
	}
	
	/**
	 * Whether or not to use a menu bar.
	 */
	public boolean shouldUseMenuBar() {
		return booleanResource(USE_MENU_BAR);
	}
	
	/**
	 * Returns a string suitable for the window manager to use as a window
	 * title.
	 */
	public String getTitle() {
		return stringResource(TITLE);
	}
	
	public int getInternalBorder() {
		return integerResource(INTERNAL_BORDER);
	}
	
	/**
	 * How many rows a new window should have.
	 */
	public int getInitialRowCount() {
		return integerResource(INITIAL_ROW_COUNT);
	}
	
	/**
	 * How many columns a new window should have.
	 */
	public int getInitialColumnCount() {
		return integerResource(INITIAL_COLUMN_COUNT);
	}
	
	private int integerResource(String name) {
		return Integer.parseInt(stringResource(name));
	}
	
	private String stringResource(String name) {
		return (String) options.get(name);
	}
	
	private boolean booleanResource(String name) {
		return parseBoolean(stringResource(name));
	}
	
	private boolean parseBoolean(String s) {
		return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("on");
	}
	
	/**
	 * Returns a color, if explicitly configured by the user.
	 * We understand colors specified in the #rrggbb form,
	 * or those parsed from rgb.txt.
	 * 
	 * Color names supported by xterm (defaults in parentheses) include:
	 * 
	 *  background (white)
	 *  foreground (black)
	 *  cursorColor (black; we use green)
	 *  highlightColor (reverse video)
	 *  pointerColor
	 *  pointerBackgroundColor
	 * 
	 * xterm also offers complete control over all the ECMA colors.
	 */
	public Color getColor(String name) {
		String description = stringResource(name);
		if (description == null) {
			return null;
		} else if (description.startsWith("#")) {
			return Color.decode("0x" + description.substring(1));
		} else {
			return getRgbColor(description);
		}
	}
	
	/**
	 * Returns a suitable fixed font (ignoring all X11 configuration,
	 * because we're unlikely to understand those font specifications).
	 * So we don't get into trouble with Xterm's font resource, and
	 * to work around Font.decode's weaknesses, we use two resources:
	 * "fontName" and "fontSize".
	 */
	public Font getFont() {
		return new Font(stringResource(FONT_NAME), Font.PLAIN, integerResource(FONT_SIZE));
	}
	
	private Options() {
		initDefaults();
		initDefaultColors();
		readOptionsFrom(".Xdefaults");
		readOptionsFrom(".Xresources");
		aliasColorBD();
	}
	
	/**
	 * Parses "-xrm <resource-string>" options from an array of
	 * command-line arguments, returning the remaining arguments as
	 * a List.
	 */
	public List parseCommandLine(String[] arguments) {
		ArrayList otherArguments = new ArrayList();
		for (int i = 0; i < arguments.length; ++i) {
			if (arguments[i].equals("-xrm")) {
				String resourceString = arguments[++i];
				processResourceString(resourceString);
			} else {
				otherArguments.add(arguments[i]);
			}
		}
		return otherArguments;
	}
	
	private void addDefault(String name, String value, String description) {
		options.put(name, value);
		descriptions.put(name, description);
	}
	
	/**
	 * Sets the defaults for non-color options.
	 */
	private void initDefaults() {
		addDefault(ANTI_ALIAS, "false", "Whether or not to use anti-aliased text");
		addDefault(FONT_NAME, GuiUtilities.isMacOs() ? "Monaco" : "Monospaced", "The name of the font to use (not an X11 font)");
		addDefault(FONT_SIZE, "12", "The size of text, in points");
		addDefault(INITIAL_COLUMN_COUNT, "80", "The number of columns in a new terminal");
		addDefault(INITIAL_ROW_COUNT, "24", "The number of rows in a new terminal");
		addDefault(INTERNAL_BORDER, "2", "The number of pixels spacing between the text and the edge of the window");
		addDefault(LOGIN_SHELL, "true", "Whether or not the shell will be started with the '-l' argument");
		addDefault(SCROLL_KEY, "true", "Whether or not pressing a key should move the scrollbar to the bottom");
		addDefault(SCROLL_TTY_OUTPUT, "false", "Whether or not output to the terminal should move the scrollbar to the bottom");
		addDefault(TITLE, "Terminator", "The default title string for new terminals");
		addDefault(USE_MENU_BAR, Boolean.toString(GuiUtilities.isMacOs()), "Whether or not to use a menu bar");
	}
	
	/**
	 * color0 to color7 are the normal colors (black, red3, green3,
	 * yellow3, blue3, magenta3, cyan3, and gray90).
	 *
	 * color8 to color15 are the bold colors (gray30, red, green, yellow,
	 * blue, magenta, cyan, and white).
	 */
	private void initDefaultColors() {
		options.put("color0", "#000000"); // black
		options.put("color1", "#cd0000"); // red3
		options.put("color2", "#00cd00"); // green3
		options.put("color3", "#cdcd00"); // yellow3
		options.put("color4", "#0000cd"); // blue3
		options.put("color5", "#cd00cd"); // magenta3
		options.put("color6", "#00cdcd"); // cyan3
		options.put("color7", "#e5e5e5"); // grey90
		
		options.put("color8", "#4d4d4d"); // gray30
		options.put("color9", "#ff0000"); // red
		options.put("color10", "#00ff00"); // green
		options.put("color11", "#ffff00"); // yellow
		options.put("color12", "#0000ff"); // blue
		options.put("color13", "#ff00ff"); // magenta
		options.put("color14", "#00ffff"); // cyan
		options.put("color15", "#ffffff"); // white
		
		// Defaults reminiscent of SGI's xwsh(1).
		options.put("background", "#000045"); // dark blue
		options.put("colorBD", "#ffffff"); // white
		options.put("cursorColor", "#00ff00"); // green
		options.put("foreground", "#e7e7e7"); // off-white
		options.put("linkColor", "#00ffff"); // cyan
		options.put("selectionColor", "#1c2bff"); // light blue
	}
	
	/**
	 * Tries to get a good bold foreground color. If the user has set their
	 * own, or they haven't set their own foreground, we don't need to do
	 * anything. Otherwise, we look through the normal (low intensity)
	 * colors to see if we can find a match. If we do, take the appropriate
	 * bold (high intensity) color for the bold foreground color.
	 */
	private void aliasColorBD() {
		if (getColor("colorBD") != null || getColor("foreground") == null) {
			return;
		}
		Color foreground = getColor("foreground");
		for (int i = 0; i < 8; ++i) {
			Color color = getColor("color" + i);
			if (foreground.equals(color)) {
				options.put("colorBD", options.get("color" + (i + 8)));
				return;
			}
		}
	}
	
	private void readRGBFile() {
		rgbColors = new HashMap();
		String[] lines = StringUtilities.readLinesFromFile("/usr/X11R6/lib/X11/rgb.txt");
		for (int i = 0; i < lines.length; ++i) {
			String line = lines[i];
			if (line.startsWith("!")) {
				continue;
			}
			int r = channelAt(line, 0);
			int g = channelAt(line, 4);
			int b = channelAt(line, 8);
			line = line.substring(12).trim();
			rgbColors.put(line.toLowerCase(), new Color(r, g, b));
		}
	}
	
	private Color getRgbColor(String description) {
		if (rgbColors == null) {
			try {
				readRGBFile();
			} catch (Exception ex) {
				Log.warn("Problem reading X11 colors", ex);
			}
		}
		return (Color) rgbColors.get(description.toLowerCase());
	}
	
	private int channelAt(String line, int offset) {
		return Integer.parseInt(line.substring(offset, offset + 3).trim());
	}
	
	private void readOptionsFrom(String filename) {
		File file = new File(System.getProperty("user.home"), filename);
		if (file.exists() == false) {
			return;
		}
		try {
			readOptionsFrom(file);
		} catch (Exception ex) {
			Log.warn("Problem reading options from " + filename, ex);
		}
	}
	
	private void readOptionsFrom(File file) {
		String[] lines = StringUtilities.readLinesFromFile(file.toString());
		for (int i = 0; i < lines.length; ++i) {
			String line = lines[i].trim();
			if (line.length() == 0 || line.startsWith("!")) {
				continue;
			}
			processResourceString(line);
		}
	}
	
	private void processResourceString(String resourceString) {
		Matcher matcher = resourcePattern.matcher(resourceString);
		if (matcher.find()) {
			String key = matcher.group(1);
			String value = matcher.group(2);
			options.put(key, value);
		}
	}
}
