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

	public void perform(TerminalListener listener) {
		if (processSequence(listener) == false) {
			Log.warn("Unimplemented escape sequence: \"" + sequence + "\"");
		}
	}
	
	private boolean processSequence(TerminalListener listener) {
		char lastChar = sequence.charAt(sequence.length() - 1);
		String midSequence = sequence.substring(1, sequence.length() - 1);
		switch (lastChar) {
			case 'A': return moveCursor(listener, midSequence, 0, -1);
			case 'B': return moveCursor(listener, midSequence, 0, 1);
			case 'C': return moveCursor(listener, midSequence, 1, 0);
			case 'c': return deviceAttributesRequest(listener, midSequence);
			case 'D': return moveCursor(listener, midSequence, -1, 0);
			case 'd': return moveCursorRowTo(listener, midSequence);
			case 'G': case '`': return moveCursorColumnTo(listener, midSequence);
			case 'f': case 'H': return moveCursorTo(listener, midSequence);
			case 'K': return killLineContents(listener, midSequence);
			case 'J': return killLines(listener, midSequence);
			case 'L': return insertLines(listener, midSequence);
			case 'M': return scrollDisplayUp(listener, midSequence);
			case 'P': return deleteCharacters(listener, midSequence);
			case 'g': return clearTabs(listener, midSequence);
			case 'h': return setMode(listener, midSequence, true);
			case 'l': return setMode(listener, midSequence, false);
			case 'm': return processFontEscape(listener, midSequence);
			case 'r': return setScrollScreen(listener, midSequence);
			default: return false;
		}
	}
	
	public boolean clearTabs(TerminalListener listener, String seq) {
		int clearType = (seq.length() == 0) ? 0 : Integer.parseInt(seq);
		if (clearType == 0) {
			// Clear horizontal tab at current cursor position.
			listener.removeTabAtCursor();
			return true;
		} else if (clearType == 3) {
			// Clear all horizontal tabs.
			listener.removeAllTabs();
			return true;
		} else {
			Log.warn("Unknown clear tabs type: " + clearType);
			return false;
		}
	}
	
	public boolean scrollDisplayUp(TerminalListener listener, String seq) {
		int count = (seq.length() == 0) ? 1 : Integer.parseInt(seq);
		for (int i = 0; i < count; i++) {
			listener.scrollDisplayDown();
		}
		return true;
	}
	
	public boolean insertLines(TerminalListener listener, String seq) {
		int count = (seq.length() == 0) ? 1 : Integer.parseInt(seq);
		listener.insertLines(count);
		return true;
	}
	
	public boolean setMode(TerminalListener listener, String seq, boolean value) {
		boolean isQuestionMode = seq.startsWith("?");
		String[] modes = (isQuestionMode ? seq.substring(1) : seq).split(";");
		for (int i = 0; i < modes.length; i++) {
			int mode = Integer.parseInt(modes[i]);
			if (isQuestionMode) {
				switch (Integer.parseInt(modes[i])) {
					case 25: listener.setCaretDisplay(value); break;
					case 47: listener.useAlternativeBuffer(value); break;
					default: Log.warn("Unknown mode " + modes[i] + " in [" + seq + (value ? 'h' : 'l'));
				}
			} else {
				switch (Integer.parseInt(modes[i])) {
					case 4: listener.setInsertMode(value); break;
					default: Log.warn("Unknown mode " + modes[i] + " in [" + seq + (value ? 'h' : 'l'));
				}
			}
		}
		return true;
	}
	
	public boolean setScrollScreen(TerminalListener listener, String seq) {
		int index = seq.indexOf(';');
		if (index == -1) {
			listener.setScrollScreen(-1, -1);
		} else {
			listener.setScrollScreen(Integer.parseInt(seq.substring(0, index)), Integer.parseInt(seq.substring(index + 1)));
		}
		return true;
	}
	
	public boolean deviceAttributesRequest(TerminalListener listener, String seq) {
		if (seq.equals("") || seq.equals("0")) {
			control.sendEscapeString("[?1;0c");
			return true;
		} else {
			return false;
		}
	}
	
	public boolean deleteCharacters(TerminalListener listener, String seq) {
		int count = (seq.length() == 0) ? 1 : Integer.parseInt(seq);
		listener.deleteCharacters(count);
		return true;
	}
	
	public boolean killLineContents(TerminalListener listener, String seq) {
		int type = (seq.length() == 0) ? 0 : Integer.parseInt(seq);
		boolean fromStart = (type >= 1);
		boolean toEnd = (type != 1);
		listener.killHorizontally(fromStart, toEnd);
		return true;
	}
	
	public boolean killLines(TerminalListener listener, String seq) {
		int type = (seq.length() == 0) ? 0 : Integer.parseInt(seq);
		boolean fromTop = (type >= 1);
		boolean toBottom = (type != 1);
		listener.killVertically(fromTop, toBottom);
		return true;
	}
	
	public boolean moveCursorRowTo(TerminalListener listener, String seq) {
		listener.setCursorPosition(-1, Integer.parseInt(seq));
		return true;
	}
	
	public boolean moveCursorColumnTo(TerminalListener listener, String seq) {
		listener.setCursorPosition(Integer.parseInt(seq), -1);
		return true;
	}
	
	public boolean moveCursorTo(TerminalListener listener, String seq) {
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
	
	public boolean moveCursor(TerminalListener listener, String countString, int xDirection, int yDirection) {
		int count = (countString.length() == 0) ? 1 : Integer.parseInt(countString);
		if (xDirection != 0) {
			listener.moveCursorHorizontally(xDirection * count);
		}
		if (yDirection != 0) {
			listener.moveCursorVertically(yDirection * count);
		}
		return true;
	}
	
	public boolean processFontEscape(TerminalListener listener, String sequence) {
		int oldStyle = listener.getStyle();
		int foreground = StyledText.getForeground(oldStyle);
		int background = StyledText.getBackground(oldStyle);
		boolean isBold = StyledText.isBold(oldStyle);
		boolean isReverseVideo = StyledText.isReverseVideo(oldStyle);
		boolean isUnderlined = StyledText.isUnderlined(oldStyle);
		boolean hasForeground = StyledText.hasForeground(oldStyle);
		boolean hasBackground = StyledText.hasBackground(oldStyle);
		String[] chunks = sequence.split(";");
		for (int i = 0; i < chunks.length; i++) {
			int value = (chunks[i].length() == 0) ? 0 : Integer.parseInt(chunks[i]);
			if (valueInRange(value, 0, 29)) {
				switch (value) {
					case 0:
						hasForeground = false;
						hasBackground = false;
						isBold = false;
						isReverseVideo = false;
						isUnderlined = false;
						break;
					case 1:
						isBold = true;
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
					case 22:
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
					default:
						Log.warn("Unknown attribute " + value + " in [" + sequence);
						break;
				}
			} else if (valueInRange(value, 30, 37)) {
				foreground = value - 30;
				hasForeground = true;
			} else if (valueInRange(value, 40, 47)) {
				background = value - 40;
				hasBackground = true;
			} else if (value == 39) {
				hasForeground = false;
			} else if (value == 49) {
				hasBackground = false;
			} else {
				Log.warn("Unknown attribute " + value + " in [" + sequence);
			}
		}
		listener.setStyle(StyledText.getStyle(foreground, hasForeground, background, hasBackground, isBold, isUnderlined, isReverseVideo));
		return true;
	}
	
	public boolean valueInRange(int value, int min, int max) {
		return (value >= min) && (value <= max);
	}
}
