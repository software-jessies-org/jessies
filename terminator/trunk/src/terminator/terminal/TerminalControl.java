package terminator.terminal;

import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import e.util.*;
import terminator.*;
import terminator.model.*;
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
	private static final boolean SHOW_ASCII_RENDITION = false;
	
	private static BufferedReader stepModeReader;

	/**
	 * On Mac OS, this seems to be an optimal size; any smaller and we end
	 * up doing more reads from programs that produce a lot of output, but
	 * going larger doesn't reduce the number. Maybe this corresponds to
	 * the stdio buffer size or something?
	 */
	private static final int INPUT_BUFFER_SIZE = 8 * 1024;

	private JTerminalPane pane;
	private TextBuffer listener;
	private PtyProcess ptyProcess;
	private boolean processIsRunning = true;
	private boolean processIsBeingDestroyed = false;
	private InputStream in;
	private OutputStream out;
	
	private int characterSet;
	private char[] g = new char[4];
	
	private boolean automaticNewline;
	
	private LogWriter logWriter;
	
	// Buffer of TerminalActions to perform.
	private ArrayList<TerminalAction> terminalActions = new ArrayList<TerminalAction>();
	
	public TerminalControl(JTerminalPane pane, TextBuffer listener, String command) throws IOException {
		reset();
		this.pane = pane;
		this.listener = listener;
		ptyProcess = new PtyProcess(command.split(" "));
		this.in = ptyProcess.getInputStream();
		this.out = ptyProcess.getOutputStream();
		this.logWriter = new LogWriter(command);
	}
	
	public void destroyProcess() {
		if (processIsRunning) {
			try {
				ptyProcess.destroy();
				processIsBeingDestroyed = true;
			} catch (Exception ex) {
				Log.warn("Failed to destroy process.", ex);
			}
		}
	}
	
	/** Starts the process listening once all the user interface stuff is set up. */
	public void start() {
		(new Thread(this, "Terminal connection listener")).start();
	}
	
	public void invokeCharacterSet(int index) {
		this.characterSet = index;
	}
	
	public void setAutomaticNewline(boolean automatic) {
		this.automaticNewline = automatic;
	}
	
	public boolean isAutomaticNewline() {
		return automaticNewline;
	}
	
	public void reset() {
		setAutomaticNewline(false);
		invokeCharacterSet(0);
		designateCharacterSet(0, 'B');
		designateCharacterSet(1, '0');
		designateCharacterSet(2, 'B');
		designateCharacterSet(3, 'B');
	}
	
	public void designateCharacterSet(int index, char set) {
		g[index] = set;
	}
	
	private StringBuilder lineBuffer = new StringBuilder();

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
				ptyProcess.waitFor();
				if (ptyProcess.didExitNormally()) {
					int status = ptyProcess.getExitStatus();
					if (status != 0 && Options.getSharedInstance().isErrorExitHolding()) {
						announceConnectionLost("\n\r[Process exited with status " + status + ".]");
						return;
					}
				} else if (ptyProcess.wasSignaled()) {
					int signal = ptyProcess.getTerminatingSignal();
					announceConnectionLost("\n\r[Process killed by signal " + signal + ".]");
					return;
				} else {
					// If it wasn't a pane close that caused us to get here, close
					// the pane.
					if (processIsBeingDestroyed == false) {
						pane.doCloseAction();
					}
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
			pane.getTextPane().setCursorVisible(false);
		} catch (Exception ex) {
			Log.warn("Couldn't say '" + message + "'.", ex);
		}
	}
	
	/** Must be called in the AWT dispatcher thread. */
	public void sizeChanged(final Dimension sizeInChars, final Dimension sizeInPixels) throws IOException {
		TerminalAction sizeChangeAction = new TerminalAction() {
			public void perform(TextBuffer listener) {
				listener.sizeChanged(sizeInChars);
			}
			
			public String toString() {
				return "TerminalAction[Size change to " + sizeInChars + "]";
			}
		};
		listener.processActions(new TerminalAction[] { sizeChangeAction });
		// Notify the pty that the size has changed.
		ptyProcess.sendResizeNotification(sizeInChars, sizeInPixels);
	}
	
	/** Returns the number of bytes in buffer which remain unprocessed. */
	private synchronized void processBuffer(byte[] buffer, int size) throws IOException {
		for (int i = 0; i < size; i++) {
			int value = (buffer[i]) & 0xff;  // We don't handle negative bytes well.
			processChar((char) value);
		}
		flushLineBuffer();
		flushTerminalActions();
	}
	
	private synchronized void flushTerminalActions() {
		if (terminalActions.size() == 0) {
			return;
		}
		
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
	
	/**
	 * According to vttest, these cursor movement characters are still
	 * treated as such, even when they occur within an escape sequence.
	 */
	private final boolean countsTowardsEscapeSequence(char ch) {
		return (ch != Ascii.BS && ch != Ascii.CR && ch != Ascii.VT);
	}
	
	private synchronized void processChar(final char ch) throws IOException {
		// Enable this if you're having trouble working out what we're being asked to interpret.
		if (SHOW_ASCII_RENDITION) {
			if (ch >= ' ' || ch == '\n') {
				System.out.print(ch);
			} else {
				System.out.print(".");
			}
		}
		
		logWriter.append(ch);
		if (ch == Ascii.ESC) {
			flushLineBuffer();
			// If the old escape sequence is interrupted; we start a new one.
			if (escapeParser != null) {
				Log.warn("Escape parser discarded with string \"" + escapeParser + "\".");
			}
			escapeParser = new EscapeParser();
			return;
		}
		if (escapeParser != null && countsTowardsEscapeSequence(ch)) {
			escapeParser.addChar(ch);
			if (escapeParser.isComplete()) {
				processEscape();
				escapeParser = null;
			}
		} else if (ch == Ascii.LF || ch == Ascii.CR || ch == Ascii.BS || ch == Ascii.HT || ch == Ascii.VT) {
			flushLineBuffer();
			doStep();
			processSpecialCharacter(ch);
		} else if (ch == Ascii.SO) {
			invokeCharacterSet(1);
		} else if (ch == Ascii.SI) {
			invokeCharacterSet(0);
		} else if (ch > Ascii.BEL) {
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

	private class PlainTextAction implements TerminalAction {
		private String line;
		
		private PlainTextAction(String line) {
			this.line = line;
		}
		
		public void perform(TextBuffer listener) {
			if (DEBUG) {
				Log.warn("Processing line \"" + line + "\"");
			}
			listener.processLine(line);
		}
		
		public String toString() {
			return "TerminalAction[Process line: " + line + "]";
		}
	}
	
	public String translate(String characters) {
		if (g[characterSet] == 'B') {
			return characters;
		}
		StringBuilder translation = new StringBuilder(characters.length());
		for (int i = 0; i < characters.length(); ++i) {
			translation.append(translateToCharacterSet(characters.charAt(i)));
		}
		return translation.toString();
	}
	
	private synchronized void flushLineBuffer() {
		final String line = lineBuffer.toString();
		lineBuffer = new StringBuilder();
		
		// Conform to the stated claim that the listener's always called in the AWT dispatch thread.
		if (line.length() > 0) {
			doStep();
			terminalActions.add(new PlainTextAction(line));
		}
		
		//pane.getOutputSpinner().animateOneFrame();
	}
	
	public synchronized void processSpecialCharacter(final char ch) {
		terminalActions.add(new TerminalAction() {
			public void perform(TextBuffer listener) {
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
					case Ascii.LF: return "LF";
					case Ascii.CR: return "CR";
					case Ascii.HT: return "HT";
					case Ascii.VT: return "VT";
					case Ascii.BS: return "BS";
					default: return "UK";
				}
			}
		});
	}
	
	public synchronized void processEscape() {
		String sequence = escapeParser.toString();
		
		if (DEBUG) {
			Log.warn("Processing escape sequence \"" + sequence + "\"");
		}
		
		// Invoke all escape sequence handling in the AWT dispatch thread - otherwise we'd have
		// to create billions upon billions of tiny little invokeLater(Runnable) things all over the place.
		doStep();
		TerminalAction action = escapeParser.getAction(this);
		if (action != null) {
			terminalActions.add(action);
		}
	}
	
	private char translateToCharacterSet(char ch) {
		switch (g[characterSet]) {
		case '0':
			return translateToGraphicalCharacterSet(ch);
		case 'A':
			return translateToUkCharacterSet(ch);
		default:
			return ch;
		}
	}
	
	/**
	 * Translate ASCII to the nearest Unicode characters to the special
	 * graphics and line drawing characters.
	 * 
	 * Run this in xterm(1) for reference:
	 * 
	 *   ruby -e 'cs="abcdefghijklmnopqrstuvwxyz"; puts(cs); \
	 *            print("\x1b(0\x1b)B\x0f");puts(cs);print("\x0e")'
	 * 
	 * Or try test 3 of vttest.
	 * 
	 * We use the Unicode box-drawing characters, but the characters
	 * extend out of the bottom of the font's bounding box, spoiling
	 * the effect. Bug parade #4896465.
	 */
	private char translateToGraphicalCharacterSet(char ch) {
		switch (ch) {
		case '`':
			return '\u2666'; // BLACK DIAMOND SUIT
		case 'a':
			return '\u2591'; // LIGHT SHADE
		case 'b':
			return '\u2409'; // SYMBOL FOR HORIZONTAL TABULATION
		case 'c':
			return '\u240c'; // SYMBOL FOR FORM FEED
		case 'd':
			return '\u240d'; // SYMBOL FOR CARRIAGE RETURN
		case 'e':
			return '\u240a'; // SYMBOL FOR LINE FEED
		case 'f':
			return '\u00b0'; // DEGREE SIGN
		case 'g':
			return '\u00b1'; // PLUS-MINUS SIGN
		case 'h':
			return '\u2424'; // SYMBOL FOR NEW LINE
		case 'i':
			return '\u240b'; // SYMBOL FOR VERTICAL TABULATION
		case 'j':
			return '\u2518'; // BOX DRAWINGS LIGHT UP AND LEFT
		case 'k':
			return '\u2510'; // BOX DRAWINGS LIGHT DOWN AND LEFT
		case 'l':
			return '\u250c'; // BOX DRAWINGS LIGHT DOWN AND RIGHT
		case 'm':
			return '\u2514'; // BOX DRAWINGS LIGHT UP AND RIGHT
		case 'n':
			return '\u253c'; // BOX DRAWINGS LIGHT VERTICAL AND HORIZONTAL
		case 'v':
			return '\u2534'; // BOX DRAWINGS LIGHT UP AND HORIZONTAL
		case 'w':
			return '\u252c'; // BOX DRAWINGS LIGHT DOWN AND HORIZONTAL
		case 'o':
		case 'p':
		case 'q':
		case 'r':
		case 's':
			// These should all be different characters,
			// but Unicode only offers one of them.
			return '\u2500'; // BOX DRAWINGS LIGHT HORIZONTAL
		case 't':
			return '\u251c'; // BOX DRAWINGS LIGHT VERTICAL AND RIGHT
		case 'u':
			return '\u2524'; // BOX DRAWINGS LIGHT VERTICAL AND LEFT
		case 'x':
			return '\u2502'; // BOX DRAWINGS LIGHT VERTICAL
		case 'y':
			return '\u2264'; // LESS-THAN OR EQUAL TO
		case 'z':
			return '\u2265'; // GREATER-THAN OR EQUAL TO
		case '{':
			return '\u03c0'; // GREEK SMALL LETTER PI
		case '|':
			return '\u2260'; // NOT EQUAL TO
		case '}':
			return '\u00a3'; // POUND SIGN
		case '~':
			return '\u00b7'; // MIDDLE DOT
		default:
			return ch;
		}
	}
	
	private char translateToUkCharacterSet(char ch) {
		return (ch == '#') ? '\u00a3' : ch;
	}
	
	public void sendEscapeString(String str) {
		try {
			if (processIsRunning) {
				out.write((byte) Ascii.ESC);
				out.write(str.getBytes());
				out.flush();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void sendChar(char ch) {
		try {
			if (processIsRunning) {
				out.write((byte) ch);
				out.flush();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void sendString(String s) {
		try {
			if (processIsRunning) {
				out.write(s.getBytes());
				out.flush();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void sendLine(String line) {
		try {
			if (processIsRunning) {
				out.write(line.getBytes());
				out.write('\r');
				out.write('\n');
				out.flush();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public LogWriter getLogWriter() {
		return logWriter;
	}
}
