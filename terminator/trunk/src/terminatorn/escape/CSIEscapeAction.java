package terminatorn.escape;

import terminatorn.*;

/**
Parses 'CSI' escape sequences.  Such sequences always have '[' as their first character,
and then are sometimes followed by a '?' character, then optionally a list of numbers
separated by ';' characters, followed by the final character which tells us what to do with
all that stuff.

@author Phil Norman
*/

public class CSIEscapeAction implements TelnetAction {
	private TelnetControl control;
	private String sequence;
	
	public CSIEscapeAction(TelnetControl control, String sequence) {
		this.control = control;
		this.sequence = sequence;
	}

	public void perform(TelnetListener listener) {
		if (processSequence(listener) == false) {
			Log.warn("Unimplemented escape sequence: \"" + sequence + "\"");
		}
	}
	
	private boolean processSequence(TelnetListener listener) {
		char lastChar = sequence.charAt(sequence.length() - 1);
		String midSequence = sequence.substring(1, sequence.length() - 1);
		switch (lastChar) {
			case 'A': return moveCursor(listener, midSequence, 0, -1);
			case 'B': return moveCursor(listener, midSequence, 0, 1);
			case 'C': return moveCursor(listener, midSequence, 1, 0);
			case 'c': return deviceAttributesRequest(listener, midSequence);
			case 'D': return moveCursor(listener, midSequence, -1, 0);
			case 'f': case 'H': return moveCursorTo(listener, midSequence);
			case 'K': return killLineContents(listener, midSequence);
			case 'J': return killLines(listener, midSequence);
			case 'L': return insertLines(listener, midSequence);
			case 'M': return scrollDisplayUp(listener, midSequence);
			case 'P': return deleteCharacters(listener, midSequence);
			case 'h': return setMode(listener, midSequence, true);
			case 'l': return setMode(listener, midSequence, false);
			case 'm': return processFontEscape(listener, midSequence);
			case 'r': return setScrollScreen(listener, midSequence);
			default: return false;
		}
	}
	
	public boolean scrollDisplayUp(TelnetListener listener, String seq) {
		int count = (seq.length() == 0) ? 1 : Integer.parseInt(seq);
		for (int i = 0; i < count; i++) {
			listener.scrollDisplayDown();
		}
		return true;
	}
	
	public boolean insertLines(TelnetListener listener, String seq) {
		int count = (seq.length() == 0) ? 1 : Integer.parseInt(seq);
		listener.insertLines(count);
		return true;
	}
	
	public boolean setMode(TelnetListener listener, String seq, boolean value) {
		if (seq.startsWith("?")) {
			String[] modes = seq.substring(1).split(";");
			for (int i = 0; i < modes.length; i++) {
				switch (Integer.parseInt(modes[i])) {
					case 25: listener.setCaretDisplay(value); break;
					case 47: listener.useAlternativeBuffer(value); break;
					default: Log.warn("Unknown mode " + modes[i] + " in [" + seq + (value ? 'h' : 'l'));
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean setScrollScreen(TelnetListener listener, String seq) {
		int index = seq.indexOf(';');
		if (index == -1) {
			listener.setScrollScreen(-1, -1);
		} else {
			listener.setScrollScreen(Integer.parseInt(seq.substring(0, index)), Integer.parseInt(seq.substring(index + 1)));
		}
		return true;
	}
	
	public boolean deviceAttributesRequest(TelnetListener listener, String seq) {
		if (seq.equals("") || seq.equals("0")) {
			control.sendEscapeString("[?1;0c");
			return true;
		} else {
			return false;
		}
	}
	
	public boolean deleteCharacters(TelnetListener listener, String seq) {
		int count = (seq.length() == 0) ? 1 : Integer.parseInt(seq);
		listener.deleteCharacters(count);
		return true;
	}
	
	public boolean killLineContents(TelnetListener listener, String seq) {
		int type = (seq.length() == 0) ? 0 : Integer.parseInt(seq);
		boolean fromStart = (type >= 1);
		boolean toEnd = (type != 1);
		listener.killHorizontally(fromStart, toEnd);
		return true;
	}
	
	public boolean killLines(TelnetListener listener, String seq) {
		int type = (seq.length() == 0) ? 0 : Integer.parseInt(seq);
		boolean fromTop = (type >= 1);
		boolean toBottom = (type != 1);
		listener.killVertically(fromTop, toBottom);
		return true;
	}
	
	public boolean moveCursorTo(TelnetListener listener, String seq) {
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
	
	public boolean moveCursor(TelnetListener listener, String countString, int xDirection, int yDirection) {
		int count = (countString.length() == 0) ? 1 : Integer.parseInt(countString);
		if (xDirection != 0) {
			listener.moveCursorHorizontally(xDirection * count);
		}
		if (yDirection != 0) {
			listener.moveCursorVertically(yDirection * count);
		}
		return true;
	}
	
	public boolean processFontEscape(TelnetListener listener, String sequence) {
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
}
