package terminator;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;

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
	private static final String BLOCK_CURSOR = "blockCursor";
	private static final String CURSOR_BLINK = "cursorBlink";
	private static final String ERROR_EXIT_HOLDING = "errorExitHolding";
	private static final String FANCY_BELL = "fancyBell";
	private static final String FONT_NAME = "fontName";
	private static final String FONT_SIZE = "fontSize";
	private static final String INITIAL_COLUMN_COUNT = "initialColumnCount";
	private static final String INITIAL_ROW_COUNT = "initialRowCount";
	private static final String INTERNAL_BORDER = "internalBorder";
	private static final String LOGIN_SHELL = "loginShell";
	private static final String SCROLL_KEY = "scrollKey";
	private static final String SCROLL_TTY_OUTPUT = "scrollTtyOutput";
	private static final String USE_MENU_BAR = "useMenuBar";
	
	private final Pattern resourcePattern = Pattern.compile("(?:(?:XTerm|Rxvt|Terminator)(?:\\*|\\.))?(\\S+):\\s*(.+)");
	
	private HashMap<String, Object> options = new HashMap<String, Object>();
	private HashMap<String, String> descriptions = new HashMap<String, String>();
	private HashMap<String, Color> rgbColors = null;
	
	public static Options getSharedInstance() {
		return INSTANCE;
	}
	
	public void showOptions(PrintWriter out) {
		String[] keys = options.keySet().toArray(new String[options.size()]);
		Arrays.sort(keys);
		for (String key : keys) {
			String description = descriptions.get(key);
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
	 * Whether to use a block cursor instead of an underline cursor.
	 */
	public boolean isBlockCursor() {
		return booleanResource(BLOCK_CURSOR);
	}
	
	/**
	 * Whether or not to keep the window up if the child exits with an
	 * error status code.
	 */
	public boolean isErrorExitHolding() {
		return booleanResource(ERROR_EXIT_HOLDING);
	}
	
	/**
	 * Whether or not to use a nicer-looking but more expensive visual
	 * bell rendition. If there were a way to detect a remote X11 display --
	 * the only place where we really need to disable this -- we could
	 * remove this option, but I don't know of one.
	 */
	public boolean isFancyBell() {
		return booleanResource(FANCY_BELL);
	}
	
	/**
	 * Whether or not to blink the cursor.
	 */
	public boolean shouldCursorBlink() {
		return booleanResource(CURSOR_BLINK);
	}
	
	/**
	 * Whether or not to use a menu bar.
	 */
	public boolean shouldUseMenuBar() {
		return booleanResource(USE_MENU_BAR);
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
		return options.get(name).toString();
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
		return (Color) options.get(name);
	}
	
	private Color colorFromString(String description) {
		if (description.startsWith("#")) {
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
		return new Font(Font.class.cast(options.get(FONT_NAME)).getFamily(), Font.PLAIN, integerResource(FONT_SIZE));
	}
	
	private Options() {
		initDefaults();
		initDefaultColors();
		readOptionsFrom(".Xdefaults");
		readOptionsFrom(".Xresources");
		readAppleTerminalOptions();
		// FIXME: read options from our own private stash, which we can safely rewrite.
		aliasColorBD();
	}
	
	private void readAppleTerminalOptions() {
		// FIXME: read Mac OS Terminal.app settings if running on Mac OS.
		// We can't parse the XML ourselves because Apple switched to binary plists in 10.4.
		// `defaults read com.apple.Terminal NSFixedPitchFont` == "Monaco\n"
		// Obvious:
		//   NSFixedPitchFont (Monaco)
		//   NSFixedPitchFontSize (12)
		//   FontAntialiasing (NO)
		//   Columns (80)
		//   BlinkCursor (YES)
		//   CursorShape (1="underline"; we don't support vertical bar, so anything else should be "block")
		// Less obvious:
		//   ShellExitAction (1)
		//   TextColors (0.905 0.905 0.905 0.000 0.000 0.270 1.000 1.000 1.000 1.000 1.000 1.000 0.000 0.000 0.270 0.905 0.905 0.905 0.111 0.167 1.000 0.333 1.000 0.333)
	}
	
	/**
	 * Parses "-xrm <resource-string>" options from an array of
	 * command-line arguments, returning the remaining arguments as
	 * a List.
	 */
	public List<String> parseCommandLine(String[] arguments) {
		ArrayList<String> otherArguments = new ArrayList<String>();
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
	
	private void addDefault(String name, Object value, String description) {
		options.put(name, value);
		descriptions.put(name, description);
	}
	
	private Font makePrototypeFont(String familyName) {
		return new Font(familyName, Font.PLAIN, 1);
	}
	
	/**
	 * Sets the defaults for non-color options.
	 */
	private void initDefaults() {
		addDefault(ANTI_ALIAS, Boolean.FALSE, "Anti-alias text?");
		addDefault(BLOCK_CURSOR, Boolean.FALSE, "Use block cursor?");
		addDefault(CURSOR_BLINK, Boolean.TRUE, "Blink cursor?");
		addDefault(ERROR_EXIT_HOLDING, Boolean.TRUE, "Keep terminal if child exits with error status?");
		addDefault(FANCY_BELL, Boolean.TRUE, "High-quality rendering of the visual bell?");
		addDefault(FONT_NAME, makePrototypeFont(GuiUtilities.getMonospacedFontName()), "Font family");
		addDefault(FONT_SIZE, Integer.valueOf(12), "Font size (points)");
		addDefault(INITIAL_COLUMN_COUNT, Integer.valueOf(80), "New terminal width");
		addDefault(INITIAL_ROW_COUNT, Integer.valueOf(24), "New terminal height");
		addDefault(INTERNAL_BORDER, Integer.valueOf(2), "Terminal border width");
		addDefault(LOGIN_SHELL, Boolean.TRUE, "Start the child with a '-l' argument?");
		addDefault(SCROLL_KEY, Boolean.TRUE, "Scroll to bottom on key press?");
		addDefault(SCROLL_TTY_OUTPUT, Boolean.FALSE, "Scroll to bottom on output?");
		
		if (GuiUtilities.isMacOs()) {
			// Mac users don't get a choice about this, though if they're insane they can override this in their resources.
			// FIXME: don't allow this to be overridden on Mac OS.
			options.put(USE_MENU_BAR, Boolean.TRUE);
		} else {
			addDefault(USE_MENU_BAR, Boolean.FALSE, "Use a menu bar?");
		}
	}
	
	public void showPreferencesDialog() {
		// FIXME: the info dialog keeps the Terminator menu bar; is that because we give it an owner?
		FormBuilder form = new FormBuilder(null, "Preferences");
		FormPanel formPanel = form.getFormPanel();
		
		String[] keys = options.keySet().toArray(new String[options.size()]);
		Arrays.sort(keys);
		for (String key : keys) {
			String description = descriptions.get(key);
			if (description != null) {
				Object value = options.get(key);
				if (value instanceof Boolean) {
					formPanel.addRow("", new BooleanPreferenceAction(key).makeUi());
				} else if (value instanceof Integer) {
					formPanel.addRow(description + ":", new IntegerPreferenceAction(key).makeUi());
				} else if (value instanceof Font) {
					formPanel.addRow(description + ":", new FontPreferenceAction(key).makeUi());
				} else if (value instanceof Color) {
					// Ignore colors.
					// FIXME: there are some colors we should probably handle; the ones with names.
				} else {
					// FIXME: we should probably handle String.
					// FIXME: the Final Solution should use a HashMap<Class, ActionAndUi>.
					continue;
				}
			}
		}
		
		form.showNonModal();
		// FIXME: write options to our own private stash.
	}
	
	private class BooleanPreferenceAction extends AbstractAction {
		private String key;
		
		public BooleanPreferenceAction(String key) {
			this.key = key;
			putValue(NAME, descriptions.get(key));
		}
		
		public void actionPerformed(ActionEvent e) {
			JCheckBox checkBox = JCheckBox.class.cast(e.getSource());
			options.put(key, checkBox.isSelected());
		}
		
		public JComponent makeUi() {
			JCheckBox checkBox = new JCheckBox(this);
			checkBox.setSelected(Boolean.class.cast(options.get(key)).booleanValue());
			return checkBox;
		}
	}
	
	private class FontPreferenceAction extends AbstractAction {
		private String key;
		
		public FontPreferenceAction(String key) {
			this.key = key;
			putValue(NAME, descriptions.get(key));
		}
		
		public void actionPerformed(ActionEvent e) {
			JComboBox comboBox = JComboBox.class.cast(e.getSource());
			options.put(key, makePrototypeFont(comboBox.getSelectedItem().toString()));
		}
		
		public JComponent makeUi() {
			JComboBox comboBox = new JComboBox();
			// FIXME: filter out unsuitable fonts. "Zapf Dingbats", for example.
			// FIXME: pull fixed fonts to the top of the list?
			for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
				comboBox.addItem(name);
			}
			comboBox.setSelectedItem(Font.class.cast(options.get(key)).getFamily());
			comboBox.addActionListener(this);
			// FIXME: add a custom renderer so you can see the fonts.
			return comboBox;
		}
	}
	
	private class IntegerPreferenceAction extends AbstractAction {
		private String key;
		
		public IntegerPreferenceAction(String key) {
			this.key = key;
			putValue(NAME, descriptions.get(key));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTextField textField = JTextField.class.cast(e.getSource());
			boolean okay = false;
			try {
				String text = textField.getText();
				int newValue = Integer.parseInt(text);
				// FIXME: really, an integer preference should have an explicit range.
				if (newValue > 0) {
					options.put(key, newValue);
					okay = true;
				}
			} catch (NumberFormatException ex) {
			}
			textField.setForeground(okay ? UIManager.getColor("TextField.foreground") : Color.RED);
		}
		
		public JComponent makeUi() {
			final ETextField textField = new ETextField(options.get(key).toString()) {
				@Override
				public void textChanged() {
					actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
				}
			};
			return textField;
		}
	}
	
	/**
	 * color0 to color7 are the normal colors (black, red3, green3,
	 * yellow3, blue3, magenta3, cyan3, and gray90).
	 *
	 * color8 to color15 are the bold colors (gray30, red, green, yellow,
	 * blue, magenta, cyan, and white).
	 */
	private void initDefaultColors() {
		addDefault("color0", colorFromString("#000000"), "Color 0: black");
		addDefault("color1", colorFromString("#cd0000"), "Color 1: red3");
		addDefault("color2", colorFromString("#00cd00"), "Color 2: green3");
		addDefault("color3", colorFromString("#cdcd00"), "Color 3: yellow3");
		addDefault("color4", colorFromString("#0000cd"), "Color 4: blue3");
		addDefault("color5", colorFromString("#cd00cd"), "Color 5: magenta3");
		addDefault("color6", colorFromString("#00cdcd"), "Color 6: cyan3");
		addDefault("color7", colorFromString("#e5e5e5"), "Color 7: grey90");
		
		addDefault("color8", colorFromString("#4d4d4d"), "Color 8: gray30");
		addDefault("color9", colorFromString("#ff0000"), "Color 9: red");
		addDefault("color10", colorFromString("#00ff00"), "Color 10: green");
		addDefault("color11", colorFromString("#ffff00"), "Color 11: yellow");
		addDefault("color12", colorFromString("#0000ff"), "Color 12: blue");
		addDefault("color13", colorFromString("#ff00ff"), "Color 13: magenta");
		addDefault("color14", colorFromString("#00ffff"), "Color 14: cyan");
		addDefault("color15", colorFromString("#ffffff"), "Color 15: white");
		
		// Defaults reminiscent of SGI's xwsh(1).
		addDefault("background", colorFromString("#000045"), "Background"); // dark blue
		addDefault("colorBD", colorFromString("#ffffff"), "Bold color"); // white
		addDefault("cursorColor", colorFromString("#00ff00"), "Cursor color"); // green
		addDefault("foreground", colorFromString("#e7e7e7"), "Foreground"); // off-white
		addDefault("linkColor", colorFromString("#00ffff"), "Link color"); // cyan
		addDefault("selectionColor", colorFromString("#1c2bff"), "Selection color"); // light blue
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
		// FIXME: with Sun's JVM, there's a class in XAWT that has a Color constant for each color in "rgb.txt".
		rgbColors = new HashMap<String, Color>();
		String[] lines = StringUtilities.readLinesFromFile("/usr/X11R6/lib/X11/rgb.txt");
		for (String line : lines) {
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
		return rgbColors.get(description.toLowerCase());
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
		for (String line : lines) {
			line = line.trim();
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
			Object currentValue = options.get(key);
			if (currentValue == null) {
				// Silently ignore resources we don't recognize.
				// FIXME: we should probably distinguish between XTerm or RXvt resources (where this is expected) and Terminator resources, where this shouldn't happen.
				return;
			}
			Class currentClass = currentValue.getClass();
			if (currentClass == Boolean.class) {
				options.put(key, Boolean.valueOf(value));
			} else if (currentClass == Font.class) {
				options.put(key, makePrototypeFont(value));
			} else if (currentClass == Integer.class) {
				options.put(key, Integer.valueOf(value));
			} else if (currentClass == Color.class) {
				options.put(key, colorFromString(value));
			} else {
				throw new RuntimeException("Resource '" + key + "' had default value " + currentValue + " of class " + currentClass);
			}
		}
	}
}
