package terminatorn;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Options {
	private static final Options INSTANCE = new Options();
	
	private HashMap options = new HashMap();
	
	public static Options getSharedInstance() {
		return INSTANCE;
	}
	
	public Color getColor(String name) {
		String description = (String) options.get(name);
		if (description == null || description.startsWith("#") == false) {
			return null;
		}
		return Color.decode("0x" + description.substring(1));
	}
	
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
