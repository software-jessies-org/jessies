package terminator;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Logs terminal output to a file in ~/.terminal-logs. If that directory
 * doesn't exist, logs to /dev/null.
 */
public class LogWriter {
	private static DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ssZ");
	
	private String filename;
	private FileWriter stream;
	
	public LogWriter(String prefix) throws IOException {
		prefix = java.net.URLEncoder.encode(prefix, "UTF-8");
		String timestamp = dateFormatter.format(new Date());
		String logsDirectoryName = System.getProperty("user.home") + File.separator + ".terminal-logs" + File.separator;
		File logsDirectory = new File(logsDirectoryName);
		this.filename = (logsDirectory.exists() ? (logsDirectoryName + prefix + '-' + timestamp + ".txt") : "/dev/null");
		this.stream = new FileWriter(filename);
	}
	
	public void append(char ch) throws IOException {
		stream.write(ch);
		if (ch == '\n') {
			stream.flush();
		}
	}
	
	public String getFilename() {
		return filename;
	}
}
