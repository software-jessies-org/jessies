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
	// We can't use ':' to separate the hours, minutes, and seconds because it's not allowed on all file systems.
	private static final DateFormat FILENAME_TIMESTAMP_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss.SSSZ");
	
	private String info = "(not logging)";
	private Writer writer;
	private Writer suspendedWriter;
	private Timer flushTimer;
	
	public LogWriter(String[] commandWords, String ptyName) {
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
			initLogging(commandWords, ptyName);
		} catch (Throwable th) {
			SimpleDialog.showDetails(null, "Couldn't Open Log File", th);
		}
	}
	
	private static File makeLogFilename(File logsDirectory, String commandLine, int truncationLength) throws UnsupportedEncodingException {
		String mostInterestingPartOfCommandLine = commandLine.substring(0, truncationLength);
		String suffix = java.net.URLEncoder.encode(mostInterestingPartOfCommandLine, "UTF-8");
		String timestamp = FILENAME_TIMESTAMP_FORMATTER.format(new Date());
		String leafname = timestamp + "-" + suffix + ".txt";
		return new File(logsDirectory, leafname);
	}
	
	private void initLogging(String[] commandWords, String ptyName) throws IOException {
		String logsDirectoryName = System.getProperty("org.jessies.terminator.logDirectory");
		File logsDirectory = new File(logsDirectoryName);
		if (logsDirectory.exists() == false) {
			this.info = "(\"" + logsDirectoryName + "\" does not exist)";
			return;
		}
		
		// Try to create a log file.
		// We'll keep truncating the name until we either succeed or there's no name left.
		// This avoids assumptions about maximum filename or path lengths.
		String commandLine = StringUtilities.join(commandWords, " ");
		int truncationLength = commandLine.length() + 1;
		while (truncationLength > 0) {
			--truncationLength;
			File logFile = makeLogFilename(logsDirectory, commandLine, truncationLength);
			try {
				this.info = "(\"" + logFile + "\" could not be opened for writing)";
				this.writer = new BufferedWriter(new FileWriter(logFile));
				this.info = logFile.toString();
				Log.warn("Logging \"" + ptyName + "\" to \"" + this.info + "\"");
				return;
			} catch (IOException ex) {
				if (truncationLength == 0 && logsDirectory.canWrite()) {
					throw ex;
				}
			}
		}
		this.info = "(\"" + logsDirectoryName + "\" is not writable)";
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
