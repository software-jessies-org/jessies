package terminatorn;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Logs terminal output to a file in ~/.terminal-logs. If that directory
 * doesn't exist, logs to /dev/null.
 */
public class LogWriter {
	private static DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ssZ");
	
	private FileWriter stream;
	
	public LogWriter(String prefix) throws IOException {
		prefix = java.net.URLEncoder.encode(prefix, "UTF-8");
		String timestamp = dateFormatter.format(new Date());
		String logsDirectoryName = System.getProperty("user.home") + File.separator + ".terminal-logs" + File.separator;
		File logsDirectory = new File(logsDirectoryName);
		if (logsDirectory.exists() == false) {
			stream = new FileWriter("/dev/null");
		} else {
			String filename =  prefix + '-' + timestamp + ".txt";
			stream = new FileWriter(logsDirectoryName + filename);
		}
	}
	
	public void append(char ch) throws IOException {
		stream.write(ch);
		if (ch == '\n') {
			stream.flush();
		}
	}
}
