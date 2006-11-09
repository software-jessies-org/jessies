package terminator.terminal.escape;

import e.util.*;
import terminator.model.*;
import terminator.terminal.*;

/**
 * Recognizes escape sequences consisting of ASCII ESC followed by a single character.
 * Note that most of these are mainly of historical interest, even though some of them look similar to more common sequences.
 */
public class SingleCharEscapeAction implements TerminalAction {
	private TerminalControl control;
	private char escChar;
	
	public SingleCharEscapeAction(TerminalControl control, char escChar) {
		this.control = control;
		this.escChar = escChar;
	}

	public void perform(TextBuffer listener) {
		switch (escChar) {
			case '6':  // rxvt: scr_backindex
				unsupported("scr_backindex");
				break;
			case '7':  // Save cursor.  rxvt saves position, current style, and charset.
				listener.saveCursor();
				break;
			case '8':  // Restore cursor.
				listener.restoreCursor();
				break;
			case '9':  // rxvt: scr_forwardindex
				unsupported("scr_forwardindex");
				break;
			case '=':  // rxvt: set private mode PrivMode_aplKP (application keypad).
				unsupported("set private mode PrivMode_aplKP (application keypad).");
				break;
			case '>':  // rxvt: unset private mode PrivMode_aplKP (application keypad).
				unsupported("unset private mode PrivMode_aplKP (application keypad).");
				break;
			case 'D':  // Move the cursor down one line, scrolling if it reaches the bottom of scroll region.  Effectively NL.
				listener.processSpecialCharacter('\n');
				break;
			case 'E':  // Move cursor to start of next line, scrolling if required.  Effectively CR,NL
				listener.processSpecialCharacter('\r');
				listener.processSpecialCharacter('\n');
				break;
			case 'H':  // rxvt: scr_set_tab(1)  Set a horizontal tab marker at the current cursor position.
				listener.setTabAtCursor();
				break;
			case 'M':  // Move cursor up one line, scrolling if it reaches the top of scroll region.  Opposite of NL.
				listener.scrollDisplayUp();
				break;
			case 'Z':
				// An obsolete form of ESC [ c (send device attributes).
				CSIEscapeAction.sendDeviceAttributes(control);
				break;
			case 'c':  // Power on (full reset).
				listener.fullReset();
				break;
				
			// Change character set.
			// Note that these are different to the related ^N and ^O sequences, which select character sets 1 and 0 and are handled elsewhere.
			// These sequences ("^[n" and "^[o") are even less common than their relatives.
			case 'n':
				control.invokeCharacterSet(2);
				break;
			case 'o':
				control.invokeCharacterSet(3);
				break;
				
			case '|':
			case '}':
			case '~':
				// Invoke the G3, G2, and G1 character sets as
				// GR. Has no visible effect.
				break;
			default:
				Log.warn("Unrecognized single-character escape \"" + escChar + "\".");
		}
	}
	
	private void unsupported(String description) {
		Log.warn("Unsupported single-character escape \"" + escChar + "\" (" + description + ").");
	}
}
