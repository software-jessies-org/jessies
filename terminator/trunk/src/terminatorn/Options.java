package terminatorn;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

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
	
	private HashMap options = new HashMap();
	private HashMap rgbColours = new HashMap();
	
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
	
	public int getInternalBorder() {
		String internalBorder = (String) options.get("internalBorder");
		if (internalBorder != null) {
			return Integer.parseInt(internalBorder);
		}
		return 2;
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
	 * xterm also offers complete control over all the ECMA colors:
	 * 
	 *  color0 to color7 are the normal colors (black, red3, green3,
	 *  yellow3, blue3, magenta3, cyan3, and gray90).
	 * 
	 *  color8 to color15 are the bold colors (gray30, red, green, yellow,
	 *  blue, magenta, cyan, and white).
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
	 */
	public Font getFont() {
		boolean isMacOS = (System.getProperty("os.name").indexOf("Mac") != -1);
		return Font.decode(isMacOS ? "Monaco" : "Monospaced");
	}
	
	private Options() {
		readRGBFile();
		readOptionsFrom(".Xdefaults");
		readOptionsFrom(".Xresources");
	}
	
	private void readRGBFile() {
		BufferedReader in = null;
		try {
			File rgbFile = new File("/usr/X11R6/lib/X11/rgb.txt");
			if (rgbFile.exists()) {
				in = new BufferedReader(new FileReader(rgbFile));
				String line;
				while ((line = in.readLine()) != null) {
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
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ex) { }
			}
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
		} catch (IOException ex) {
			Log.warn("Problem reading options from " + filename, ex);
		}
	}
	
	private void readOptionsFrom(File file) throws IOException {
//		Pattern pattern = Pattern.compile("(?:XTerm|Rxvt)(?:\\*|\\.)(\\S+):\\s*(\\S+)");
// The colour name should be any character, since some entries in rgb.txt contain spaces.
		Pattern pattern = Pattern.compile("(?:XTerm|Rxvt)(?:\\*|\\.)(\\S+):\\s*(.+)");
		LineNumberReader in = new LineNumberReader(new FileReader(file));
		String line;
		while ((line = in.readLine()) != null) {
			line = line.trim();
			if (line.length() == 0 || line.startsWith("!")) {
				continue;
			}
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				String key = matcher.group(1);
				String value = matcher.group(2);
				options.put(key, value);
			}
		}
	}
}
