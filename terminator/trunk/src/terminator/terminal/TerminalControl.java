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

	private static final int INPUT_BUFFER_SIZE = 4096;

	private JTerminalPane pane;
	private TerminalListener listener;
	private Process process;
	private boolean ignoreExitStatus;
	private boolean processIsRunning = true;
	private InputStream in;
	private PtyOutputStream out;
	
	private LogWriter logWriter;
	
	// Buffer of TerminalActions to perform.
	private ArrayList terminalActions = new ArrayList();
	
	public TerminalControl(JTerminalPane pane, TerminalListener listener, String command, Process process, boolean ignoreExitStatus) throws IOException {
		this.pane = pane;
		this.listener = listener;
		this.process = process;
		this.ignoreExitStatus = ignoreExitStatus;
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

	private boolean localEcho = false;
	private StringBuffer lineBuffer = new StringBuffer();

	public static final int ESC = 0x1b;
	private EscapeParser escapeParser;
	
	public static void doStep() {
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
			int remainCount = 0;
			while ((readCount = in.read(buffer, remainCount, buffer.length - remainCount)) != -1) {
				// Process all unprocessed bytes in buffer, and then move any which remain
				// processed to the front of the buffer, noting how many there are.
				readCount += remainCount;
				remainCount = processBuffer(buffer, readCount);
				System.arraycopy(buffer, readCount - remainCount, buffer, 0, remainCount);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			processIsRunning = false;
			try {
				int status = process.waitFor();
				if (status == 0 || ignoreExitStatus) {
					pane.getTerminalPaneMaster().closeTerminalPane(pane);
				} else {
					announceConnectionLost("[Process exited with status " + status + ".]");
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
	
	private boolean nextByteIsCommand = false;
	
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
	private int processBuffer(byte[] buffer, int size) throws IOException {
		int i;
		for (i = 0; i < size; i++) {
			int value = (buffer[i]) & 0xff;  // We don't handle negative bytes well.
			processChar((char) value);
		}
		flushLineBuffer();
		final TerminalAction[] actions = (TerminalAction[]) terminalActions.toArray(new TerminalAction[terminalActions.size()]);
		terminalActions.clear();
		synchronized (this) {
			terminalActionsProcessed = false;
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					listener.processActions(actions);
				} finally {
					synchronized (TerminalControl.this) {
						terminalActionsProcessed = true;
						TerminalControl.this.notifyAll();
					}
				}
			}
		});
		synchronized (this) {
			if (terminalActionsProcessed == false) {
				try {
					wait();
				} catch (InterruptedException ex) {
					Log.warn("Go away.", ex);
				}
			}
		}
		return size - i;
	}
	
	private boolean terminalActionsProcessed = true;
	
	public void processChar(final char ch) throws IOException {
		logWriter.append(ch);
		if (ch == ESC) {
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
		} else if (ch > 0x7) {
			// Most telnetd(1) implementations seem to have a bug whereby
			// they send the NUL byte at the end of the C strings they want to
			// output when you first connect. Since all Unixes are pretty much
			// copy and pasted from one another these days, this silly mistake
			// only needed to be made once.
			
			// Nothing below BEL is printable anyway.
			
			// And neither is BEL, really.
			lineBuffer.append(ch);
		}
	}

	public void flushLineBuffer() {
		final String line = lineBuffer.toString();
		lineBuffer.setLength(0);
		
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
	
	public void processSpecialCharacter(final char ch) {
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
	
	public void processEscape() {
		String sequence = escapeParser.toString();
		
		// Invoke all escape sequence handling in the AWT dispatch thread - otherwise we'd have
		// to create billions upon billions of tiny little invokeLater(Runnable) things all over the place.
		doStep();
		TerminalAction action = escapeParser.getAction(this);
		if (action != null) {
			terminalActions.add(action);
		}
	}
	
	public void sendEscapeString(String str) {
		try {
			out.write((byte) ESC);
			out.write(str.getBytes());
			out.flush();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Send a fake resize notification so the application gets a SIGWINCH
	 * and redraws itself.
	 */
	public void sendRedrawScreen() {
		try {
			out.sendResizeNotification(pane.getTextPane().getModel().getCurrentSizeInChars(), new Dimension());
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
}
