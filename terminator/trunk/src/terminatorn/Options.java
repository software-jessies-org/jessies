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
	
	public static Options getSharedInstance() {
		return INSTANCE;
	}
	
	/**
	 * Returns a color, if explicitly configured by the user.
	 * We only understand colors specified in the #rrggbb form,
	 * and make no effort to find and parse rgb.txt (though we
	 * could).
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
		if (description == null || description.startsWith("#") == false) {
			return null;
		}
		return Color.decode("0x" + description.substring(1));
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
		readOptionsFrom(".Xdefaults");
		readOptionsFrom(".Xresources");
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
		Pattern pattern = Pattern.compile("(?:XTerm|Rxvt)(?:\\*|\\.)(\\S+):\\s*(\\S+)");
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
