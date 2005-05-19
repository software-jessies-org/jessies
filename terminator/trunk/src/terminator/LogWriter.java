package terminator;

import e.gui.*;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Logs terminal output to a file in ~/.terminal-logs. If that directory
 * doesn't exist, logs to /dev/null.
 */
public class LogWriter {
	private static DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HHmmssZ");
	
	private String filename;
	private BufferedWriter stream;
	private boolean suspended;
	
	public LogWriter(String prefix) {
		try {
			this.filename = makeLogFilename(prefix);
			this.stream = new BufferedWriter(new FileWriter(filename));
		} catch (Throwable th) {
			SimpleDialog.showDetails(null, "Couldn't Open Log File", th);
		} finally {
			this.suspended = (stream == null);
		}
	}
	
	private static String makeLogFilename(String prefix) throws IOException {
		prefix = java.net.URLEncoder.encode(prefix, "UTF-8");
		String timestamp = dateFormatter.format(new Date());
		String logsDirectoryName = System.getProperty("user.home") + File.separator + ".terminal-logs" + File.separator;
		File logsDirectory = new File(logsDirectoryName);
		return (logsDirectory.exists() ? (logsDirectoryName + prefix + '-' + timestamp + ".txt") : "/dev/null");
	}
	
	public void append(char ch) throws IOException {
		if (suspended) {
			return;
		}
		
		stream.write(ch);
		if (ch == '\n') {
			stream.flush();
		}
	}
	
	public String getFilename() {
		return filename;
	}
	
	public void setSuspended(boolean newState) {
		if (stream != null) {
			suspended = newState;
		}
	}
	
	public boolean isSuspended() {
		return suspended;
	}
}
