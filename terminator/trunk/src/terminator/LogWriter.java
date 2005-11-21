package terminator;

import e.gui.*;
import e.util.Log;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Logs terminal output to a file in ~/.terminal-logs. Logging can be
 * temporarily suspended. If the terminal logs directory does not exist
 * or we can't open the log file for some other reason, logging is
 * automatically suspended, and can't be un-suspended.
 */
public class LogWriter {
	private static DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HHmmssZ");
	
	private String info = "(not logging)";
	private BufferedWriter stream;
	private boolean suspended;
	
	public LogWriter(String prefix) {
		try {
			initLogging(prefix);
		} catch (Throwable th) {
			SimpleDialog.showDetails(null, "Couldn't Open Log File", th);
		} finally {
			this.suspended = (stream == null);
		}
	}
	
	private void initLogging(String prefix) throws IOException {
		prefix = java.net.URLEncoder.encode(prefix, "UTF-8");
		String timestamp = dateFormatter.format(new Date());
		String logsDirectoryName = System.getProperty("user.home") + File.separator + ".terminal-logs" + File.separator;
		File logsDirectory = new File(logsDirectoryName);
		if (logsDirectory.exists()) {
			String filename = logsDirectoryName + prefix + '-' + timestamp + ".txt";
			this.info = filename;
			this.stream = new BufferedWriter(new FileWriter(filename));
		} else {
			this.info = "(" + logsDirectoryName + " does not exist)";
		}
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
	
	public String getInfo() {
		return info;
	}
	
	public void setSuspended(boolean newState) {
		if (stream != null) {
			try {
				stream.flush();
			} catch (Throwable th) {
				Log.warn("Exception occurred flushing the log stream.", th);
			}
			suspended = newState;
		}
	}
	
	public boolean isSuspended() {
		return suspended;
	}
}
