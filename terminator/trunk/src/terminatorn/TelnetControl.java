package terminatorn;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

/**
Telnet stream control object - manages the interface between the rest of the Java code and the
low-level telnet protocol (RFC 854).

@author Phil Norman
@author Elliott Hughes
*/

public class TelnetControl implements Runnable {
	private static final boolean DEBUG = false;
	private static final boolean DEBUG_STEP_MODE = false;
	private static BufferedReader stepModeReader;

	private static final int INPUT_BUFFER_SIZE = 4096;

	private JTelnetPane pane;
	private TelnetListener listener;
	private Process process;
	private boolean processIsRunning = true;
	private InputStream in;
	private OutputStream out;
	
	private LogWriter logWriter;
	
	// Buffer of TelnetActions to perform.
	private ArrayList telnetActions = new ArrayList();
	
	public TelnetControl(JTelnetPane pane, TelnetListener listener, String command, Process process) throws IOException {
		this.pane = pane;
		this.listener = listener;
		this.process = process;
		this.in = process.getInputStream();
		this.out = process.getOutputStream();
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
		(new Thread(this, "Telnet connection listener")).start();
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
				if (status == 0) {
					pane.getController().closeTelnetPane(pane);
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
	public void sizeChanged(final Dimension sizeInChars) {
		TelnetAction sizeChangeAction = new TelnetAction() {
			public void perform(TelnetListener listener) {
				listener.sizeChanged(sizeInChars);
			}
		};
		listener.processActions(new TelnetAction[] { sizeChangeAction });
		// Notify the pty that the size has changed.
	}
	
	/** Returns the number of bytes in buffer which remain unprocessed. */
	private int processBuffer(byte[] buffer, int size) throws IOException {
		int i;
		for (i = 0; i < size; i++) {
			int value = (buffer[i]) & 0xff;  // We don't handle negative bytes well.
			processChar((char) value);
		}
		flushLineBuffer();
		final TelnetAction[] actions = (TelnetAction[]) telnetActions.toArray(new TelnetAction[telnetActions.size()]);
		telnetActions.clear();
		synchronized (this) {
			telnetActionsProcessed = false;
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					listener.processActions(actions);
				} finally {
					synchronized (TelnetControl.this) {
						telnetActionsProcessed = true;
						TelnetControl.this.notifyAll();
					}
				}
			}
		});
		synchronized (this) {
			if (telnetActionsProcessed == false) {
				try {
					wait();
				} catch (InterruptedException ex) {
					Log.warn("Go away.", ex);
				}
			}
		}
		return size - i;
	}
	
	private boolean telnetActionsProcessed = true;
	
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
			telnetActions.add(new TelnetAction() {
				public void perform(TelnetListener listener) {
					if (DEBUG) {
						Log.warn("Processing line \"" + line + "\"");
					}
					listener.processLine(line);
				}
			});
		}
	}
	
	public void processSpecialCharacter(final char ch) {
		telnetActions.add(new TelnetAction() {
			public void perform(TelnetListener listener) {
				if (DEBUG) {
					String charDesc = "UK";
					switch (ch) {
						case '\n': charDesc = "LF"; break;
						case '\r': charDesc = "CR"; break;
						case '\t': charDesc = "TAB"; break;
						case KeyEvent.VK_BACK_SPACE: charDesc = "BS"; break;
					}
					Log.warn("Processing special char \"" + charDesc + "\"");
				}
				listener.processSpecialCharacter(ch);
			}
		});
	}
	
	public void processEscape() {
		String sequence = escapeParser.toString();
		
		// Invoke all escape sequence handling in the AWT dispatch thread - otherwise we'd have
		// to create billions upon billions of tiny little invokeLater(Runnable) things all over the place.
		doStep();
		TelnetAction action = escapeParser.getAction(this);
		if (action != null) {
			telnetActions.add(action);
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
