package terminator.escape;

import e.util.*;
import terminator.*;

/**

@author Phil Norman
*/

public class SingleCharEscapeAction implements TerminalAction {
	private char escChar;
	
	public SingleCharEscapeAction(char escChar) {
		this.escChar = escChar;
	}

	public void perform(TerminalListener listener) {
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
			case 'Z':  // rxvt: Print ESCZ_ANSWER
				unsupported("Print ESCZ_ANSWER");
				break;
			case 'c':  // Power on (full reset).
				listener.fullReset();
				break;
			case 'n':  // rxvt: scr_charset_choose(2)
				unsupported("scr_charset_choose(2)");
				break;
			case 'o':  // rxvt: scr_charset_choose(3)
				unsupported("scr_charset_choose(3)");
				break;
			default:
				Log.warn("Unrecognised single-character escape \"" + escChar + "\".");
		}
	}
	
	private void unsupported(String description) {
		Log.warn("Unsupported single-character escape \"" + escChar + "\" (" + description + ").");
	}
}
