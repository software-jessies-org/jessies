package terminator;

import e.gui.*;
import e.util.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.Timer;

/**
 * Logs terminal output to a file.
 * Logging can be temporarily suspended.
 * If the terminal logs directory does not exist or we can't open the log file for some other reason, logging is automatically suspended, and can't be un-suspended.
 */
public class LogWriter {
	private static DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HHmmssZ");
	
	private String info = "(not logging)";
	private Writer writer;
	private Writer suspendedWriter;
	private Timer flushTimer;
	
	public LogWriter(String[] command) {
		// Establish the invariant that writer != null.
		// suspendedWriter is still null - when we're not suspended.
		this.writer = NullWriter.INSTANCE;
		this.flushTimer = new Timer(1000, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				flush();
			}
		});
		flushTimer.setRepeats(false);
		try {
			String prefix = StringUtilities.join(command, " ");
			initLogging(prefix);
		} catch (Throwable th) {
			SimpleDialog.showDetails(null, "Couldn't Open Log File", th);
		}
	}
	
	private void initLogging(String prefix) throws IOException {
		prefix = java.net.URLEncoder.encode(prefix, "UTF-8");
		String timestamp = dateFormatter.format(new Date());
		String logsDirectoryName = System.getProperty("org.jessies.terminator.logDirectory");
		File logsDirectory = new File(logsDirectoryName);
		if (logsDirectory.exists()) {
			// FIXME: Do something about "File name too long".
			File logFile = new File(logsDirectory, prefix + '-' + timestamp + ".txt");
			try {
				this.info = logFile.toString();
				this.writer = new BufferedWriter(new FileWriter(logFile));
			} catch (IOException ex) {
				this.info = "(\"" + logFile + "\" could not be opened for writing)";
				if (logsDirectory.canWrite()) {
					Log.warn("Exception occurred creating log writer \"" + logFile + "\".", ex);
				} else {
					this.info = "(\"" + logsDirectoryName + "\" is not writable)";
				}
			}
		} else {
			this.info = "(\"" + logsDirectoryName + "\" does not exist)";
		}
	}
	
	public void append(char[] chars, int charCount, boolean sawNewline) throws IOException {
		writer.write(chars, 0, charCount);
		if (sawNewline) {
			flushTimer.restart();
		}
	}
	
	public void flush() {
		try {
			writer.flush();
		} catch (Throwable th) {
			Log.warn("Exception occurred flushing log writer \"" + info + "\".", th);
		}
	}
	
	public void close() {
		try {
			suspend(false);
			writer.close();
			writer = NullWriter.INSTANCE;
		} catch (Throwable th) {
			Log.warn("Exception occurred closing log writer \"" + info + "\".", th);
		}
	}
	
	public String getInfo() {
		return info;
	}
	
	public void suspend(boolean shouldSuspend) {
		flush();
		if (shouldSuspend == isSuspended()) {
			return;
		}
		if (shouldSuspend) {
			suspendedWriter = writer;
			writer = NullWriter.INSTANCE;
		} else {
			writer = suspendedWriter;
			suspendedWriter = null;
		}
	}
	
	public boolean isSuspended() {
		return (suspendedWriter != null);
	}
	
	public static class NullWriter extends Writer {
		public static final Writer INSTANCE = new NullWriter();
		
		private NullWriter() {
		}
		
		public void close() {
		}
		
		public void flush() {
		}
		
		public void write(int c) {
		}
		
		public void write(char[] buffer, int offset, int byteCount) {
		}
	}
}
