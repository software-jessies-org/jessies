package terminatorn.escape;

import e.util.*;
import terminatorn.*;

/**
An XTermEscapeAction performs the action associated with an XTerm-style escape sequence.
XTerm escape sequences always start with a ']' character, followed by a number.  Optionally,
following the number is a ';' character, which if present, denotes that following must be some
string of characters terminated by a BEL (0x07) character.  We strip off the initial ']' and the
BEL terminator, if present, in the constructor, since it contains no information.

@author Phil Norman
*/

public class XTermEscapeAction implements TerminalAction {
	private String sequence;
	
	public XTermEscapeAction(String sequence) {
		// Trim off the terminating BEL character if present.
		if (sequence.charAt(sequence.length() - 1) == '\007') {
			this.sequence = sequence.substring(1, sequence.length() - 1);
		} else {
			this.sequence = sequence.substring(1);
		}
	}

	/**
	 * Handles the special escape sequence from xterm, called OSC by ECMA.
	 * From rxvt:
	 *
	 * XTerm escape sequences: ESC ] Ps;Pt BEL
	 *       0 = change iconName/title
	 *       1 = change iconName
	 *       2 = change title
	 *      46 = change log file (not implemented)
	 *      50 = change font
	 *
	 * rxvt extensions:
	 *      10 = menu
	 *      20 = bg pixmap
	 *      39 = change default fg color
	 *      49 = change default bg color
	 */
	public void perform(TerminalListener listener) {
		if (sequence.startsWith("2;") || sequence.startsWith("0;")) {
			String newWindowTitle = sequence.substring(2);
			listener.setWindowTitle(newWindowTitle);
		} else {
			Log.warn("Unsupported XTerm escape sequence \"" + sequence + "\".");
		}
	}
}
