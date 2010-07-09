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
public class TerminalLogWriter {
	// We can't use ':' to separate the hours, minutes, and seconds because it's not allowed on all file systems.
	private static final DateFormat FILENAME_TIMESTAMP_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss.SSSZ");
	
	private String info = "(not logging)";
	private Writer writer;
	private Writer suspendedWriter;
	private Timer flushTimer;
	
	public TerminalLogWriter(List<String> command) {
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
			initLogging(StringUtilities.join(command, " "));
		} catch (Throwable th) {
			SimpleDialog.showDetails(null, "Couldn't Open Log File", th);
		}
	}
	
	private synchronized static File makeLogFilename(File logsDirectory, String commandLine, int truncationLength) {
		String mostInterestingPartOfCommandLine = commandLine.substring(0, truncationLength);
		String suffix = StringUtilities.urlEncode(mostInterestingPartOfCommandLine);
		String timestamp = FILENAME_TIMESTAMP_FORMATTER.format(new Date());
		String leafname = timestamp + "-" + suffix + ".txt";
		return new File(logsDirectory, leafname);
	}
	
	private void initLogging(String commandLine) throws IOException {
		String logsDirectoryName = System.getProperty("org.jessies.terminator.logDirectory");
		File logsDirectory = new File(logsDirectoryName);
		if (logsDirectory.exists() == false) {
			this.info = "(\"" + logsDirectoryName + "\" does not exist)";
			return;
		}
		try {
			// Multiple Terminator processes may contend for the same log directory,
			// so just creating a file called "sentinel" would be liable to occasional failure.
			File sentinel = File.createTempFile("sentinel", null, logsDirectory);
			sentinel.delete();
		} catch (IOException ex) {
			// What other reason could there be?
			this.info = "(\"" + logsDirectoryName + "\" is not writable)";
			return;
		}
		
		// Try to create a log file.
		// We'll keep truncating the name until we either succeed or there's no name left.
		// This avoids assumptions about maximum filename or path lengths.
		for (int truncationLength = commandLine.length(); truncationLength >= 0; --truncationLength) {
			File logFile = makeLogFilename(logsDirectory, commandLine, truncationLength);
			try {
				this.info = "(\"" + logFile + "\" could not be opened for writing)";
				this.writer = new BufferedWriter(new FileWriter(logFile));
				this.info = logFile.toString();
				return;
			} catch (IOException ex) {
				if (truncationLength == 0) {
					// That's it. We can't retry with a shorter filename.
					throw ex;
				}
			}
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
		
		@Override public void close() {
		}
		
		@Override public void flush() {
		}
		
		@Override public void write(int c) {
		}
		
		@Override public void write(char[] buffer, int offset, int byteCount) {
		}
	}
}
