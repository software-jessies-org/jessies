package terminatorn;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import e.util.*;

/**
 * Reads in settings from the file system and makes the conveniently
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
 */
public class Options {
	private static final Options INSTANCE = new Options();
	
	private final Pattern resourcePattern = Pattern.compile("(?:(?:XTerm|Rxvt)(?:\\*|\\.))?(\\S+):\\s*(.+)");
	
	private HashMap options = new HashMap();
	private HashMap rgbColours = new HashMap();
	
	private HashMap propertySets = new HashMap();
	
	public static Options getSharedInstance() {
		return INSTANCE;
	}
	
	/**
	 * Whether or not the shells we start should be login shells. The
	 * default is false.
	 */
	public boolean isLoginShell() {
		return defaultedBooleanResource("loginShell", false);
	}
	
	/**
	 * Whether or not pressing a key should cause the the scrollbar to go
	 * to the bottom of the scrolling region. The default is true.
	 */
	public boolean isScrollKey() {
		return defaultedBooleanResource("scrollKey", true);
	}
	
	/**
	 * Whether or not output to the terminal should cause the scrollbar to
	 * go to the bottom of the scrolling region. The default is false.
	 */
	public boolean isScrollTtyOutput() {
		return defaultedBooleanResource("scrollTtyOutput", false);
	}
	
	/**
	 * Whether or not to anti-alias text.
	 */
	public boolean isAntiAliased() {
		return defaultedBooleanResource("antiAlias", false);
	}
	
	private boolean defaultedBooleanResource(String name, boolean defaultValue) {
		String value = (String) options.get(name);
		if (value != null) {
			return parseBoolean(value);
		}
		return defaultValue;
	}
	
	private boolean parseBoolean(String s) {
		return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes");
	}
	
	/**
	 * Returns a string suitable for the window manager to use as a window
	 * title. The default is "Terminator".
	 */
	public String getTitle() {
		String title = (String) options.get("title");
		return (title != null) ? title : "Terminator";
	}
	
	public int getInternalBorder() {
		return defaultedIntegerResource("internalBorder", 2);
	}
	
	/**
	 * How many rows a new window should have.
	 */
	public int getInitialRowCount() {
		return defaultedIntegerResource("initialRowCount", 24);
	}
	
	/**
	 * How many columns a new window should have.
	 */
	public int getInitialColumnCount() {
		return defaultedIntegerResource("initialColumnCount", 80);
	}
	
	private int defaultedIntegerResource(String name, int defaultValue) {
		String value = (String) options.get(name);
		if (value != null) {
			return Integer.parseInt(value);
		}
		return defaultValue;
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
	 *  cursorColor (black)
	 *  highlightColor (reverse video)
	 *  pointerColor
	 *  pointerBackgroundColor
	 * 
	 * xterm also offers complete control over all the ECMA colors.
	 */
	public Color getColor(String name) {
		String description = (String) options.get(name);
		if (description == null) {
			return null;
		} else if (description.startsWith("#")) {
			return Color.decode("0x" + description.substring(1));
		} else {
			return (Color) rgbColours.get(description.toLowerCase());
		}
	}
	
	/**
	 * Returns a suitable fixed font (ignoring all X11 configuration,
	 * because we're unlikely to understand those font specifications).
	 * So we don't get into trouble with Xterm's font resource, we
	 * call ours "javaFont".
	 */
	public Font getFont() {
		String fontDescription = (String) options.get("javaFont");
		if (fontDescription == null) {
			fontDescription = GuiUtilities.isMacOs() ? "Monaco" : "Monospaced";
		}
		return Font.decode(fontDescription);
	}
	
	private Options() {
		readRGBFile();
		initDefaultColors();
		readOptionsFrom(".Xdefaults");
		readOptionsFrom(".Xresources");
		File terminatorOptionsFile = new File(System.getProperty("user.home"), ".terminator");
		aliasColorBD();
		try {
			readTerminatorOptions(terminatorOptionsFile);
		} catch (Exception ex) {
			Log.warn("Problem reading options from " + terminatorOptionsFile.getPath(), ex);
		}
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
	
	/**
	 * color0 to color7 are the normal colors (black, red3, green3,
	 * yellow3, blue3, magenta3, cyan3, and gray90).
	 *
	 * color8 to color15 are the bold colors (gray30, red, green, yellow,
	 * blue, magenta, cyan, and white).
	 */
	private void initDefaultColors() {
		options.put("color0", "black");
		options.put("color1", "red3");
		options.put("color2", "green3");
		options.put("color3", "yellow3");
		options.put("color4", "blue3");
		options.put("color5", "magenta3");
		options.put("color6", "cyan3");
		options.put("color7", "gray90");
		
		options.put("color8", "gray30");
		options.put("color9", "red");
		options.put("color10", "green");
		options.put("color11", "yellow");
		options.put("color12", "blue");
		options.put("color13", "magenta");
		options.put("color14", "cyan");
		options.put("color15", "white");
		
		options.put("cursorColor", "black");
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
			rgbColours.put(line.toLowerCase(), new Color(r, g, b));
		}
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
	
	public String[] getPropertySetNames(String type) {
		HashMap typeSets = (HashMap) propertySets.get(type);
		if (typeSets == null) {
			return new String[0];
		} else {
			return (String[]) typeSets.keySet().toArray(new String[0]);
		}
	}
	
	public Properties getPropertySet(String type, String name) {
		HashMap typeSets = (HashMap) propertySets.get(type);
		if (typeSets == null) {
			return null;
		} else {
			HashMap map = (HashMap) typeSets.get(name);
			if (map == null) {
				return null;
			} else {
				Properties result = new Properties();
				result.putAll(map);
				return result;
			}
		}
	}
	
	private void readTerminatorOptions(File file) throws IOException {
		if (file.exists() == false) {
			Log.warn("Create a file called " + file.getPath() + " with your Terminator setup.");
			return;
		}
		LineNumberReader in = null;
		try {
			in = new LineNumberReader(new FileReader(file));
			Pattern propertySetOpener = Pattern.compile("^(\\w+)\\s+(\\w+)\\s+\\{");
			String line;
			HashMap propertySet = null;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")) {
					continue;
				}
				if (propertySet == null) {
					Matcher matcher = propertySetOpener.matcher(line);
					if (matcher.matches()) {
						String type = matcher.group(1);
						String name = matcher.group(2);
						propertySet = new HashMap();
						HashMap typeMap = (HashMap) propertySets.get(type);
						if (typeMap == null) {
							typeMap = new HashMap();
							propertySets.put(type, typeMap);
						}
						typeMap.put(name, propertySet);
					}
				} else {
					int equalsIndex = line.indexOf('=');
					if (line.equals("}")) {
						propertySet = null;
					} else if (equalsIndex != -1) {
						String key = line.substring(0, equalsIndex).trim();
						String value = line.substring(equalsIndex + 1).trim();
						if (value.startsWith("\"") && value.endsWith("\"")) {
							value = value.substring(1, value.length() - 1);
						}
						propertySet.put(key, value);
					}
				}
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ex) {
					Log.warn("Couldn't close stream.", ex);
				}
			}
		}
	}
}
