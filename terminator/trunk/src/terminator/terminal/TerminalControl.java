package terminator.terminal;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import e.util.*;
import terminator.*;
import terminator.view.*;
import terminator.terminal.escape.*;

/**
Terminal stream control object - manages the interface between the rest of the Java code and the
low-level terminal protocol.

@author Phil Norman
@author Elliott Hughes
*/

public class TerminalControl implements Runnable {
	private static final boolean DEBUG = false;
	private static final boolean DEBUG_STEP_MODE = false;
	private static BufferedReader stepModeReader;

	/**
	 * On Mac OS, this seems to be an optimal size; any smaller and we end
	 * up doing more reads from programs that produce a lot of output, but
	 * going larger doesn't reduce the number. Maybe this corresponds to
	 * the stdio buffer size or something?
	 */
	private static final int INPUT_BUFFER_SIZE = 8 * 1024;

	private JTerminalPane pane;
	private TerminalListener listener;
	private Process process;
	private boolean processIsRunning = true;
	private InputStream in;
	private PtyOutputStream out;
	
	private boolean graphicalCharacterSet = false;
	
	private LogWriter logWriter;
	
	// Buffer of TerminalActions to perform.
	private ArrayList terminalActions = new ArrayList();
	
	public TerminalControl(JTerminalPane pane, TerminalListener listener, String command, Process process) throws IOException {
		this.pane = pane;
		this.listener = listener;
		this.process = process;
		this.in = process.getInputStream();
		this.out = new PtyOutputStream(process.getOutputStream());
		this.logWriter = new LogWriter(command);
	}
	
	public void destroyProcess() {
		if (processIsRunning) {
			try {
				process.destroy();
			} catch (Exception ex) {
				Log.warn("Failed to destroy process.", ex);
			}
		}
	}
	
	/** Starts the process listening once all the user interface stuff is set up. */
	public void start() {
		(new Thread(this, "Terminal connection listener")).start();
	}

	private StringBuffer lineBuffer = new StringBuffer();

	private EscapeParser escapeParser;
	
	public static final void doStep() {
		if (DEBUG_STEP_MODE) {
			try {
				if (stepModeReader == null) {
					stepModeReader = new BufferedReader(new InputStreamReader(System.in));
				}
				stepModeReader.readLine();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public void run() {
		try {
			byte[] buffer = new byte[INPUT_BUFFER_SIZE];
			int readCount;
			while ((readCount = in.read(buffer, 0, buffer.length)) != -1) {
				processBuffer(buffer, readCount);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			processIsRunning = false;
			try {
				int status = process.waitFor();
				if (status != 0 && Options.getSharedInstance().isErrorExitHolding()) {
					announceConnectionLost("[Process exited with status " + status + ".]");
				} else {
					pane.doCloseAction();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public void announceConnectionLost(String message) {
		try {
			final byte[] bytes = message.getBytes();
			processBuffer(bytes, bytes.length);
			pane.getTextPane().setCaretDisplay(false);
		} catch (Exception ex) {
			Log.warn("Couldn't say '" + message + "'.", ex);
		}
	}
	
	/** Must be called in the AWT dispatcher thread. */
	public void sizeChanged(final Dimension sizeInChars, final Dimension sizeInPixels) throws IOException {
		TerminalAction sizeChangeAction = new TerminalAction() {
			public void perform(TerminalListener listener) {
				listener.sizeChanged(sizeInChars);
			}
			
			public String toString() {
				return "TerminalAction[Size change to " + sizeInChars + "]";
			}
		};
		listener.processActions(new TerminalAction[] { sizeChangeAction });
		// Notify the pty that the size has changed.
		out.sendResizeNotification(sizeInChars, sizeInPixels);
	}
	
	/** Returns the number of bytes in buffer which remain unprocessed. */
	private synchronized void processBuffer(byte[] buffer, int size) throws IOException {
		for (int i = 0; i < size; i++) {
			int value = (buffer[i]) & 0xff;  // We don't handle negative bytes well.
			processChar((char) value);
		}
		flushLineBuffer();
		
		final TerminalAction[] actions = (TerminalAction[]) terminalActions.toArray(new TerminalAction[terminalActions.size()]);
		terminalActions.clear();
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					listener.processActions(actions);
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static final char ASCII_BEL = 0x07;
	private static final char ASCII_SO = 0x0e;
	private static final char ASCII_SI = 0x0f;
	private static final char ASCII_ESC = 0x1b;
	
	private void processChar(final char ch) throws IOException {
		logWriter.append(ch);
		if (ch == ASCII_ESC) {
			flushLineBuffer();
			// If the old escape sequence is interrupted; we start a new one.
			if (escapeParser != null) {
				Log.warn("Escape parser discarded with string \"" + escapeParser + "\".");
			}
			escapeParser = new EscapeParser();
			return;
		}
		if (escapeParser != null) {
			escapeParser.addChar(ch);
			if (escapeParser.isComplete()) {
				processEscape();
				escapeParser = null;
			}
		} else if (ch == '\n' || ch == '\r' || ch == KeyEvent.VK_BACK_SPACE || ch == '\t') {
			flushLineBuffer();
			doStep();
			processSpecialCharacter(ch);
		} else if (ch == ASCII_SO) {
			graphicalCharacterSet = true;
		} else if (ch == ASCII_SI) {
			graphicalCharacterSet = false;
		} else if (ch > ASCII_BEL) {
			// Most telnetd(1) implementations seem to have a bug whereby
			// they send the NUL byte at the end of the C strings they want to
			// output when you first connect. Since all Unixes are pretty much
			// copy and pasted from one another these days, this silly mistake
			// only needed to be made once.
			
			// Nothing below BEL is printable anyway.
			
			// And neither is BEL, really.
			
			if (graphicalCharacterSet) {
				lineBuffer.append(translateToGraphicalCharacterSet(ch));
			} else {
				lineBuffer.append(ch);
			}
		}
	}

	private synchronized void flushLineBuffer() {
		final String line = lineBuffer.toString();
		lineBuffer = new StringBuffer();
		
		// Conform to the stated claim that the listener's always called in the AWT dispatch thread.
		if (line.length() > 0) {
			doStep();
			terminalActions.add(new TerminalAction() {
				public void perform(TerminalListener listener) {
					if (DEBUG) {
						Log.warn("Processing line \"" + line + "\"");
					}
					listener.processLine(line);
				}
				
				public String toString() {
					return "TerminalAction[Process line: " + line + "]";
				}
			});
		}
	}
	
	public synchronized void processSpecialCharacter(final char ch) {
		terminalActions.add(new TerminalAction() {
			public void perform(TerminalListener listener) {
				if (DEBUG) {
					Log.warn("Processing special char \"" + getCharDesc(ch) + "\"");
				}
				listener.processSpecialCharacter(ch);
			}
			
			public String toString() {
				return "TerminalAction[Special char " + getCharDesc(ch) + "]";
			}
			
			private String getCharDesc(char ch) {
				switch (ch) {
					case '\n': return "LF";
					case '\r': return "CR";
					case '\t': return "TAB";
					case KeyEvent.VK_BACK_SPACE: return "BS";
					default: return "UK";
				}
			}
		});
	}
	
	public synchronized void processEscape() {
		String sequence = escapeParser.toString();
		
		// Invoke all escape sequence handling in the AWT dispatch thread - otherwise we'd have
		// to create billions upon billions of tiny little invokeLater(Runnable) things all over the place.
		doStep();
		TerminalAction action = escapeParser.getAction(this);
		if (action != null) {
			terminalActions.add(action);
		}
	}
	
	/**
	 * Translate ASCII to the Unicode box-drawing characters.
	 */
	private char translateToGraphicalCharacterSet(char ch) {
		switch (ch) {
			case 'q': return '\u2500';
			case 'x': return '\u2502';
			case 'm': return '\u2514';
			case 'j': return '\u2518';
			case 'l': return '\u250c';
			case 'k': return '\u2510';
			default:
				return ch;
		}
	}
	
	public void sendEscapeString(String str) {
		try {
			out.write((byte) ASCII_ESC);
			out.write(str.getBytes());
			out.flush();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void sendChar(char ch) {
		try {
			out.write((byte) ch);
			out.flush();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void sendString(String s) {
		try {
			out.write(s.getBytes());
			out.flush();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void sendLine(String line) {
		try {
			out.write(line.getBytes());
			out.write('\r');
			out.write('\n');
			out.flush();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public LogWriter getLogWriter() {
		return logWriter;
	}
}
