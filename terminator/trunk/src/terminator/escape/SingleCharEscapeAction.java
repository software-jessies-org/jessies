package terminatorn.escape;

import terminatorn.*;

/**

@author Phil Norman
*/

public class SingleCharEscapeAction implements TelnetAction {
	private char escChar;
	
	public SingleCharEscapeAction(char escChar) {
		this.escChar = escChar;
	}

	public void perform(TelnetListener listener) {
		switch (escChar) {
			case '6':  // rxvt: scr_backindex
				unsupported();
				break;
			case '7':  // Save cursor.  rxvt saves position, current style, and charset.
				listener.saveCursor();
				break;
			case '8':  // Restore cursor.
				listener.restoreCursor();
				break;
			case '9':  // rxvt: scr_forwardindex
				unsupported();
				break;
			case '=':  // rxvt: set private mode PrivMode_aplKP (keypad madness).
				unsupported();
				break;
			case '>':  // rxvt: unset private mode PrivMode_aplKP (keypad madness).
				unsupported();
				break;
			case 'D':  // Scroll display down.
				listener.scrollDisplayDown();
				break;
			case 'E':  // rxvt: scr_add_lines containing '\n\r', 1, 2
				unsupported();
				break;
			case 'H':  // rxvt: scr_set_tab(1)
				unsupported();
				break;
			case 'M':  // Scroll display up.
				listener.scrollDisplayUp();
				break;
			case 'Z':  // rxvt: Print ESCZ_ANSWER
				unsupported();
				break;
			case 'c':  // Power on (full reset).
				listener.fullReset();
				break;
			case 'n':  // rxvt: scr_charset_choose(2)
				unsupported();
				break;
			case 'o':  // rxvt: scr_charset_choose(3)
				unsupported();
				break;
			default:
				Log.warn("Unrecognised single-character escape \"" + escChar + "\".");
		}
	}
	
	private void unsupported() {
		Log.warn("Unsupported single-character escape \"" + escChar + "\".");
	}
}
