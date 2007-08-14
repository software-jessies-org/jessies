package terminator.terminal.escape;

import e.util.*;
import terminator.model.*;
import terminator.terminal.*;

/**
Parses 'CSI' escape sequences.  Such sequences always have '[' as their first character,
and then are sometimes followed by a '?' character, then optionally a list of numbers
separated by ';' characters, followed by the final character which tells us what to do with
all that stuff.

@author Phil Norman
*/

public class CSIEscapeAction implements TerminalAction {
	private TerminalControl control;
	private String sequence;
	
	public CSIEscapeAction(TerminalControl control, String sequence) {
		this.control = control;
		this.sequence = sequence;
	}

	public void perform(TerminalModel model) {
		if (processSequence(model) == false) {
			Log.warn("Unimplemented escape sequence: \"" + sequence + "\"");
		}
	}
	
	private String getSequenceType(char lastChar) {
		switch (lastChar) {
		case 'A': return "Cursor up";
		case 'B': return "Cursor down";
		case 'C': return "Cursor right";
		case 'c': return "Device attributes request";
		case 'D': return "Cursor left";
		case 'd': return "Move cursor to row";
		case 'G':
		case '`': return "Move cursor column to";
		case 'f':
		case 'H': return "Move cursor to";
		case 'K': return "Kill line contents";
		case 'J': return "Kill lines";
		case 'L': return "Insert lines";
		case 'M': return "Delete lines";
		case 'P': return "Delete characters";
		case 'g': return "Clear tabs";
		case 'h': return "Set DEC private mode";
		case 'l': return "Clear DEC private mode";
		case 'm': return "Set font, color, etc";
		case 'n': return "Device status report";
		case 'r': return "Restore DEC private modes or set scrolling region";
		case 's': return "Save DEC private modes";
		default: return "Unknown";
		}
	}
	
	public String toString() {
		char lastChar = sequence.charAt(sequence.length() - 1);
		return "CSIEscapeAction[" + getSequenceType(lastChar) + "]";
	}
	
	private boolean processSequence(TerminalModel model) {
		char lastChar = sequence.charAt(sequence.length() - 1);
		String midSequence = sequence.substring(1, sequence.length() - 1);
		switch (lastChar) {
		case 'A':
			return moveCursor(model, midSequence, 0, -1);
		case 'B':
			return moveCursor(model, midSequence, 0, 1);
		case 'C':
			return moveCursor(model, midSequence, 1, 0);
		case 'c':
			return deviceAttributesRequest(model, midSequence);
		case 'D':
			return moveCursor(model, midSequence, -1, 0);
		case 'd':
			return moveCursorRowTo(model, midSequence);
		case 'G':
		case '`':
			return moveCursorColumnTo(model, midSequence);
		case 'f':
		case 'H':
			return moveCursorTo(model, midSequence);
		case 'K':
			return killLineContents(model, midSequence);
		case 'J':
			return killLines(model, midSequence);
		case 'L':
			return insertLines(model, midSequence);
		case 'M':
			return deleteLines(model, midSequence);
		case 'P':
			return deleteCharacters(model, midSequence);
		case 'g':
			return clearTabs(model, midSequence);
		case 'h':
			return setDecPrivateMode(model, midSequence, true);
		case 'l':
			return setDecPrivateMode(model, midSequence, false);
		case 'm':
			return processFontEscape(model, midSequence);
		case 'n':
			return processDeviceStatusReport(model, midSequence);
		case 'r':
			if (midSequence.startsWith("?")) {
				return restoreDecPrivateModes(model, midSequence);
			} else {
				return setScrollingRegion(model, midSequence);
			}
		case 's':
			return saveDecPrivateModes(model, midSequence);
		default:
			Log.warn("unknown CSI sequence " + sequence);
			return false;
		}
	}
	
	public boolean clearTabs(TerminalModel model, String seq) {
		int clearType = (seq.length() == 0) ? 0 : Integer.parseInt(seq);
		if (clearType == 0) {
			// Clear horizontal tab at current cursor position.
			model.removeTabAtCursor();
			return true;
		} else if (clearType == 3) {
			// Clear all horizontal tabs.
			model.removeAllTabs();
			return true;
		} else {
			Log.warn("Unknown clear tabs type: " + clearType);
			return false;
		}
	}
	
	public boolean deleteLines(TerminalModel model, String seq) {
		int count = (seq.length() == 0) ? 1 : Integer.parseInt(seq);
		for (int i = 0; i < count; i++) {
			model.deleteLine();
		}
		return true;
	}
	
	public boolean insertLines(TerminalModel model, String seq) {
		int count = (seq.length() == 0) ? 1 : Integer.parseInt(seq);
		model.insertLines(count);
		return true;
	}
	
	private boolean setDecPrivateMode(TerminalModel model, String seq, boolean value) {
		boolean isPrivateMode = seq.startsWith("?");
		String[] modes = (isPrivateMode ? seq.substring(1) : seq).split(";");
		for (String modeString : modes) {
			int mode = Integer.parseInt(modeString);
			if (isPrivateMode) {
				switch (mode) {
				case 25:
					model.setCursorVisible(value);
					break;
				case 47:
					model.useAlternateBuffer(value);
					break;
				default:
					Log.warn("Unknown private mode " + mode + " in [" + seq + (value ? 'h' : 'l'));
				}
			} else {
				switch (mode) {
				case 4:
					model.setInsertMode(value);
					break;
				case 20:
					control.setAutomaticNewline(value);
					break;
				default:
					Log.warn("Unknown mode " + mode + " in [" + seq + (value ? 'h' : 'l'));
				}
			}
		}
		return true;
	}
	
	public boolean restoreDecPrivateModes(TerminalModel model, String seq) {
		Log.warn("Restore DEC private mode values not implemented (CSI " + seq + ")");
		return false;
	}
	
	public boolean saveDecPrivateModes(TerminalModel model, String seq) {
		Log.warn("Save DEC private mode values not implemented (CSI " + seq + ")");
		return false;
	}
	
	public boolean setScrollingRegion(TerminalModel model, String seq) {
		int index = seq.indexOf(';');
		if (index == -1) {
			model.setScrollingRegion(-1, -1);
		} else {
			model.setScrollingRegion(Integer.parseInt(seq.substring(0, index)), Integer.parseInt(seq.substring(index + 1)));
		}
		return true;
	}
	
	public boolean deviceAttributesRequest(TerminalModel model, String seq) {
		if (seq.equals("") || seq.equals("0")) {
			sendDeviceAttributes(control);
			return true;
		} else {
			return false;
		}
	}
	
	public static void sendDeviceAttributes(TerminalControl control) {
		control.sendUtf8String(Ascii.ESC + "[?1;0c");
	}
	
	public boolean deleteCharacters(TerminalModel model, String seq) {
		int count = (seq.length() == 0) ? 1 : Integer.parseInt(seq);
		model.deleteCharacters(count);
		return true;
	}
	
	public boolean killLineContents(TerminalModel model, String seq) {
		int type = (seq.length() == 0) ? 0 : Integer.parseInt(seq);
		boolean fromStart = (type >= 1);
		boolean toEnd = (type != 1);
		model.killHorizontally(fromStart, toEnd);
		return true;
	}
	
	public boolean killLines(TerminalModel model, String seq) {
		int type = (seq.length() == 0) ? 0 : Integer.parseInt(seq);
		boolean fromTop = (type >= 1);
		boolean toBottom = (type != 1);
		model.killVertically(fromTop, toBottom);
		return true;
	}
	
	public boolean moveCursorRowTo(TerminalModel model, String seq) {
		model.setCursorPosition(-1, Integer.parseInt(seq));
		return true;
	}
	
	public boolean moveCursorColumnTo(TerminalModel model, String seq) {
		model.setCursorPosition(Integer.parseInt(seq), -1);
		return true;
	}
	
	public boolean moveCursorTo(TerminalModel model, String seq) {
		int x = 1;
		int y = 1;
		int splitIndex = seq.indexOf(';');
		if (splitIndex != -1) {
			y = Integer.parseInt(seq.substring(0, splitIndex));
			x = Integer.parseInt(seq.substring(splitIndex + 1));
		}
		model.setCursorPosition(x, y);
		return true;
	}
	
	public boolean moveCursor(TerminalModel model, String countString, int xDirection, int yDirection) {
		int count = (countString.length() == 0) ? 1 : Integer.parseInt(countString);
		if (xDirection != 0) {
			model.moveCursorHorizontally(xDirection * count);
		}
		if (yDirection != 0) {
			model.moveCursorVertically(yDirection * count);
		}
		return true;
	}
	
	private boolean processDeviceStatusReport(TerminalModel model, String sequence) {
		switch (Integer.parseInt(sequence)) {
		case 5:
			control.sendUtf8String(Ascii.ESC + "[0n");
			return true;
		case 6:
			Location location = model.getCursorPosition();
			int row = location.getLineIndex() - model.getFirstDisplayLine() + 1;
			int column = location.getCharOffset() + 1;
			control.sendUtf8String(Ascii.ESC + "[" + row + ";" + column + "R");
			return true;
		default:
			return false;
		}
	}
	
	public boolean processFontEscape(TerminalModel model, String sequence) {
		int oldStyle = model.getStyle();
		int foreground = StyledText.getForeground(oldStyle);
		int background = StyledText.getBackground(oldStyle);
		boolean isBold = StyledText.isBold(oldStyle);
		boolean isReverseVideo = StyledText.isReverseVideo(oldStyle);
		boolean isUnderlined = StyledText.isUnderlined(oldStyle);
		boolean hasForeground = StyledText.hasForeground(oldStyle);
		boolean hasBackground = StyledText.hasBackground(oldStyle);
		String[] chunks = sequence.split(";");
		for (String chunk : chunks) {
			int value = (chunk.length() == 0) ? 0 : Integer.parseInt(chunk);
			switch (value) {
			case 0:
				// Clear all attributes.
				hasForeground = false;
				hasBackground = false;
				isBold = false;
				isReverseVideo = false;
				isUnderlined = false;
				break;
			case 1:
				isBold = true;
				break;
			case 2:
				// ECMA-048 says "faint, decreased intensity or second colour".
				// gnome-terminal implements this as grey.
				// xterm does nothing.
				break;
			case 4:
				isUnderlined = true;
				break;
			case 5:
				// Blink on. Unsupported.
				break;
			case 7:
				isReverseVideo = true;
				break;
			case 21:
				// The xwsh man page suggests this should disable bold.
				// ECMA-048 says it turns on double-underlining.
				// xterm does nothing.
				// gnome-terminal treats this the same as 22.
				break;
			case 22:
				// ECMA-048 says "normal colour or normal intensity (neither bold nor faint)".
				// xterm clears the bold flag.
				// gnome-terminal clears the bold and half-intensity flags.
				isBold = false;
				break;
			case 24:
				isUnderlined = false;
				break;
			case 25:
				// Blink off. Unsupported.
				break;
			case 27:
				isReverseVideo = false;
				break;
			case 30:
			case 31:
			case 32:
			case 33:
			case 34:
			case 35:
			case 36:
			case 37:
				// Set foreground color.
				foreground = value - 30;
				hasForeground = true;
				break;
			case 39:
				// Use default foreground color.
				hasForeground = false;
				break;
			case 40:
			case 41:
			case 42:
			case 43:
			case 44:
			case 45:
			case 46:
			case 47:
				// Set background color.
				background = value - 40;
				hasBackground = true;
				break;
			case 49:
				// Use default background color.
				hasBackground = false;
				break;
			default:
				Log.warn("Unknown attribute " + value + " in [" + sequence);
				break;
			}
		}
		model.setStyle(StyledText.getStyle(foreground, hasForeground, background, hasBackground, isBold, isUnderlined, isReverseVideo));
		return true;
	}
}
