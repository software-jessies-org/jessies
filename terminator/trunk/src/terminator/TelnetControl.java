package terminatorn;

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

// TODO: Our way of cutting the input into little chunks to be executed separately, each in their
// own little Runnable on the AWT dispatch thread is leading to enormous performance problems.
// We need to sort this out, using some kind of timer and event buffer so we can set one single
// Runnable going which will execute each command which needs doing, keeping track of all
// screen areas which have changed, and then finally cause a redraw/size update on the GUI
// once all the work in that parcel is done.  This should help make us fast like we should be.

public class TelnetControl implements Runnable {
	private static final boolean DEBUG = false;
	private static final boolean DEBUG_STEP_MODE = false;
	private static BufferedReader stepModeReader;

	private static final int INPUT_BUFFER_SIZE = 4096;

	private TelnetListener listener;
	private InputStream in;
	private OutputStream out;
	
	// Buffer of TelnetActions to perform.
	private ArrayList telnetActions = new ArrayList();
	
	public TelnetControl(TelnetListener listener, InputStream in, OutputStream out) throws IOException {
		this.listener = listener;
		this.in = in;
		this.out = out;
		(new Thread(this, "Telnet connection listener")).start();
	}
	
	private boolean localEcho = false;
	private StringBuffer lineBuffer = new StringBuffer();
	private boolean readingOSC = false;
	
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
//				System.err.println("Processing buffer of " + readCount + " bytes.");
				remainCount = processBuffer(buffer, readCount);
				System.arraycopy(buffer, readCount - remainCount, buffer, 0, remainCount);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			Log.warn("Returning from TelnetSocket.run()");
		}
	}
	
	private boolean nextByteIsCommand = false;
	
	/** Returns the number of bytes in buffer which remain unprocessed. */
	private int processBuffer(byte[] buffer, int size) throws IOException {
		int i;
		for (i = 0; i < size; i++) {
			int value = (buffer[i]) & 0xff;  // We don't handle negative bytes well.
			if (nextByteIsCommand) {
				if (value == IAC) {
					processChar((char) value);
				} else {
					int extraIACByteCount = getExtraIACByteCount(value);
					if (size - i > extraIACByteCount) {
						int[] extraBytes = new int[extraIACByteCount];
						for (int j = 0; j < extraBytes.length; j++) {
							extraBytes[j] = ((int) buffer[++i]) & 0xff;
						}
						interpretAsCommand(value, extraBytes);
					} else {
						break;
					}
				}
				nextByteIsCommand = false;
			} else {
				if (value == IAC) {
					nextByteIsCommand = true;
				} else {
					processChar((char) value);
				}
			}
		}
		flushLineBuffer();
		final TelnetAction[] actions = (TelnetAction[]) telnetActions.toArray(new TelnetAction[telnetActions.size()]);
		telnetActions.clear();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				listener.processActions(actions);
			}
		});
		return size - i;
	}
	
	public void processChar(final char ch) {
		if (readingOSC) {
			if (ch == '\\' || ch == 0x7) {
				readingOSC = false;
			}
			return;
		}
		if (ch <= 0x7) {
			// Most telnetd(1) implementations seem to have a bug whereby
			// they send the NUL byte at the end of the C strings they want to
			// output when you first connect. Since all Unixes are pretty much
			// copy and pasted from one another these days, this silly mistake
			// only needed to be made once.
			
			// Nothing below BEL is printable anyway.
			
			// And neither is BEL, really.
			return;
		}
		if (ch == ESC) {
			flushLineBuffer();
			inEscape = true;
			escapeBuffer.setLength(0);  // Old escape sequence is interrupted; we start a new one.
			return;
		}
		if (inEscape) {
			escapeBuffer.append(ch);
			//FIXME: does anything else terminate an ANSI escape sequence?
			if (Character.isLetter(ch) || ch == '>') {
				processEscape();
			}
		} else if (ch == '\n' || ch == '\r' || ch == KeyEvent.VK_BACK_SPACE) {
			flushLineBuffer();
			doStep();
			processSpecialCharacter(ch);
		} else {
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
						System.err.println("Processing line \"" + line + "\"");
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
						case KeyEvent.VK_BACK_SPACE: charDesc = "BS"; break;
					}
					System.err.println("Processing special char \"" + charDesc + "\"");
				}
				listener.processSpecialCharacter(ch);
			}
		});
	}

	public static final int ESC = 0x1b;
	
	public boolean inEscape = false;
	private StringBuffer escapeBuffer = new StringBuffer();
	
	public void processEscape() {
		inEscape = false;
		final String sequence = escapeBuffer.toString();
		escapeBuffer.setLength(0);
		if (sequence.startsWith("]")) {  // This one affects the stream, so we must deal with it now.
			readingOSC = true;
		}
		
		// Invoke all escape sequence handling in the AWT dispatch thread - otherwise we'd have
		// to create billions upon billions of tiny little invokeLater(Runnable) things all over the place.
		doStep();
		telnetActions.add(new TelnetAction() {
			public void perform(TelnetListener listener) {
				if (DEBUG) {
					Log.warn("Escape sequence \"" + sequence + "\"");
				}
				if (sequence.startsWith("[")) {
					if (processVT100BracketSequence(sequence) == false) {
						Log.warn("Unknown escape sequence: \"" + sequence + "\"");
					}
				} else if (sequence.equals("c")) {
					// Full reset (RIS).
					listener.fullReset();
				} else if (sequence.equals("D")) {
					listener.scrollDisplayDown();
				} else if (sequence.equals("M")) {
					listener.scrollDisplayUp();
				} else {
					Log.warn("Unknown escape sequence: \"" + sequence + "\"");
				}
			}
		});
	}
	
	public boolean processVT100BracketSequence(String sequence) {
		char lastChar = sequence.charAt(sequence.length() - 1);
		String midSequence = sequence.substring(1, sequence.length() - 1);
		switch (lastChar) {
			case 'A': return moveCursor(midSequence, 0, -1);
			case 'B': return moveCursor(midSequence, 0, 1);
			case 'C': return moveCursor(midSequence, 1, 0);
			case 'c': return deviceAttributesRequest(midSequence);
			case 'D': return moveCursor(midSequence, -1, 0);
			case 'f': case 'H': return moveCursorTo(midSequence);
			case 'K': return killLineContents(midSequence);
			case 'J': return killLines(midSequence);
			case 'm': return processFontEscape(midSequence);
			case 'r': return setScrollScreen(midSequence);
			default: return false;
		}
	}
	
	public boolean setScrollScreen(String seq) {
		int index = seq.indexOf(';');
		if (index == -1) {
			listener.setScrollScreen(-1, -1);
		} else {
			listener.setScrollScreen(Integer.parseInt(seq.substring(0, index)), Integer.parseInt(seq.substring(index + 1)));
		}
		return true;
	}
	
	public boolean deviceAttributesRequest(String seq) {
		if (seq.equals("") || seq.equals("0")) {
			sendEscapeString("[?1;0c");
			return true;
		} else {
			return false;
		}
	}
	
	public boolean killLineContents(String seq) {
		int type = (seq.length() == 0) ? 0 : Integer.parseInt(seq);
		boolean fromStart = (type >= 1);
		boolean toEnd = (type != 1);
		listener.killHorizontally(fromStart, toEnd);
		return true;
	}
	
	public boolean killLines(String seq) {
		int type = (seq.length() == 0) ? 0 : Integer.parseInt(seq);
		boolean fromTop = (type >= 1);
		boolean toBottom = (type != 1);
		listener.killVertically(fromTop, toBottom);
		return true;
	}
	
	public boolean moveCursorTo(String seq) {
		int x = 1;
		int y = 1;
		int splitIndex = seq.indexOf(';');
		if (splitIndex != -1) {
			y = Integer.parseInt(seq.substring(0, splitIndex));
			x = Integer.parseInt(seq.substring(splitIndex + 1));
		}
		listener.setCursorPosition(x, y);
		return true;
	}
	
	public boolean moveCursor(String countString, int xDirection, int yDirection) {
		int count = (countString.length() == 0) ? 1 : Integer.parseInt(countString);
		if (xDirection != 0) {
			listener.moveCursorHorizontally(xDirection * count);
		}
		if (yDirection != 0) {
			listener.moveCursorVertically(yDirection * count);
		}
		return true;
	}
	
	public boolean processFontEscape(String sequence) {
		int oldStyle = listener.getStyle();
		int foreground = StyledText.getForeground(oldStyle);
		int background = StyledText.getBackground(oldStyle);
		boolean isBold = StyledText.isBold(oldStyle);
		boolean isUnderlined = StyledText.isUnderlined(oldStyle);
		String[] bits = sequence.split(";");
		for (int i = 0; i < bits.length; i++) {
			int value = (bits[i].length() == 0) ? 0 : Integer.parseInt(bits[i]);
			if (valueInRange(value, 0, 8)) {
				switch (value) {
					case 0:
						foreground = StyledText.BLACK;
						background = StyledText.WHITE;
						isBold = false;
						isUnderlined = false;
						break;
					case 1: isBold = true; break;
					case 4: isUnderlined = true; break;
					case 7:
						int temp = foreground;
						foreground = background;
						background = temp;
						break;
				}
			} else if (valueInRange(value, 30, 37)) {
				foreground = value - 30;
			} else if (valueInRange(value, 40, 47)) {
				background = value - 40;
			}
		}
		listener.setStyle(StyledText.getStyle(foreground, background, isBold, isUnderlined));
		return true;
	}
	
	public boolean valueInRange(int value, int min, int max) {
		return (value >= min) && (value <= max);
	}
	
	public void sendEscapeString(String str) {
		try {
			out.write((byte) ESC);
			out.write(str.getBytes());
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
	
	public void sendInterrupt() {
		try {
			sendCommand(INTERRUPT_PROCESS);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void sendCommand(int type) throws IOException {
		out.write(new byte[] { (byte) IAC, (byte) type });
	}
	
	public void sendCommand(int type, int detail) throws IOException {
		out.write(new byte[] { (byte) IAC, (byte) type, (byte) detail });
	}

	public void sendDo(int what) throws IOException {
		sendCommand(DO, what);
	}
	
	public void sendDoNot(int what) throws IOException {
		sendCommand(DO_NOT, what);
	}
	
	public void sendWill(int what) throws IOException {
		sendCommand(WILL, what);
	}
	
	public void sendWillNot(int what) throws IOException {
		sendCommand(WILL_NOT, what);
	}
	
	public int getExtraIACByteCount(int command) {
		switch (command) {
			case WILL:
			case WILL_NOT:
			case DO:
			case DO_NOT:
			case SB:
				return 1;
			
			case SE:
			default:
				return 0;
		}
	}
	
	public void interpretAsCommand(int b, int[] extraBytes) throws IOException {
		if (b == WILL) {
			int what = extraBytes[0];
			if (what == TelnetOption.ECHO || what == TelnetOption.SUPPRESS_GO_AHEAD) {
				sendDo(what);
			} else if (what == TelnetOption.STATUS) {
				sendDoNot(what);
			} else {
				Log.warn("Got WILL  " + what);
			}
		} else if (b == WILL_NOT) {
			int what = extraBytes[0];
			Log.warn("Got WON'T " + what);
		} else if (b == DO) {
			int what = extraBytes[0];
			if (what == TelnetOption.ECHO) {
				sendWill(TelnetOption.ECHO);
				localEcho = true;
				Log.warn("Enabling local echo...");
			} else if (what == TelnetOption.TERMINAL_TYPE) {
				sendWill(what);
			} else if (what == TelnetOption.TERMINAL_SPEED || what == TelnetOption.NAWS || what == TelnetOption.NEW_ENVIRON || what == TelnetOption.X_DISPLAY_LOCATION || what == TelnetOption.TOGGLE_FLOW_CONTROL) {
				sendWillNot(what);
			} else {
				Log.warn("Got DO	" + what + "; refusing.");
				sendWillNot(what);
			}
		} else if (b == DO_NOT) {
			int what = extraBytes[0];
			if (what == TelnetOption.ECHO) {
				Log.warn("Disabling local echo...");
				localEcho = false;
				sendWillNot(TelnetOption.ECHO);
			} else {
				Log.warn("Got DON'T " + what + "; don't understand!");
			}
		} else if (b == SB) {
			int what = extraBytes[0];
			Log.warn("Start sub-option negotiation for " + what);
			if (what == TelnetOption.TERMINAL_TYPE) {
				out.write(new byte[] { (byte) IAC, (byte) SB, (byte) TelnetOption.TERMINAL_TYPE, (byte) IS });
				out.write("vt100".getBytes());
				sendCommand(SE);
			}
			// FIXME: should we skip bytes until we see IAC SE?
		} else if (b == SE) {
			// Subnegotiation is over.
			Log.warn("End sub-option negotiation");
		} else {
			Log.warn("Got unknown command " + Integer.toHexString(b));
		}
	}
	
	/** TELNET: End of subnegotiation parameters. */
	private static final int SE = 240;
	
	/** TELNET: No Operation. */
	private static final int NOP = 241;
	
	/**
	 * TELNET: The data stream portion of a Synch.
	 * This should always be accompanied by a TCP Urgent notification.
	 */
	private static final int DATA_MARK = 242;
	
	private static final int BREAK = 243;
	private static final int INTERRUPT_PROCESS = 244;
	private static final int ABORT_OUTPUT = 245;
	private static final int ARE_YOU_THERE = 246;
	private static final int ERASE_CHARACTER = 247;
	private static final int ERASE_LINE = 248;
	private static final int GO_AHEAD = 249;
	private static final int SB = 250; // Indicates that what follows is subnegotiation of the indicated option
	private static final int IS = 0;
	
	private static final int WILL = 251; // Indicates the desire to begin performing, or confirmation that you are now performing, the indicated option
	private static final int WILL_NOT = 252; // Indicates the refusal to perform, or continue performing, the indicated option.
	private static final int DO = 253; // Indicates the request that the other party perform, or confirmation that you are expecting the other party to perform, the indicated option.
	private static final int DO_NOT = 254; // Indicates the demand that the other party stop performing, or confirmation that you are no longer expecting the other party to perform, the indicated option.
	
	/** TELNET IAC: Interpret As Command. */
	private static final int IAC = 255;
	
	public class TelnetOption {
		public static final int ECHO = 1;
		public static final int SUPPRESS_GO_AHEAD = 3;
		public static final int STATUS = 5;
		public static final int TERMINAL_TYPE = 24;
		/** Negotiate About Window Size. */
		public static final int NAWS = 31;
		public static final int TERMINAL_SPEED = 32;
		public static final int TOGGLE_FLOW_CONTROL = 33;
		public static final int X_DISPLAY_LOCATION = 35;
		public static final int OLD_ENVIRON = 36;
		public static final int NEW_ENVIRON = 39;

		private TelnetOption() { }
	}
}
