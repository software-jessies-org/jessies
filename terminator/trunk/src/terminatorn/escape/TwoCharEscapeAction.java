package terminatorn.escape;

import e.util.*;
import terminatorn.*;

/**

@author Phil Norman
*/

public class TwoCharEscapeAction implements TerminalAction {
	private String sequence;
	
	public TwoCharEscapeAction(String sequence) {
		this.sequence = sequence;
	}

	public void perform(TerminalListener listener) {
		switch (sequence.charAt(0)) {
			case '#':  // rxvt: if second char == '8', scr_E().
				unsupported();
				break;
			case '(':  // rxvt: scr_charset_set(0, second char).
				unsupported();
				break;
			case ')':  // rxvt: scr_charset_set(1, second char).
				unsupported();
				break;
			case '*':  // rxvt: scr_charset_set(2, second char).
				unsupported();
				break;
			case '+':  // rxvt: scr_charset_set(3, second char).
				unsupported();
				break;
			case '$':  // rxvt: scr_charset_set(-2, second char).
				unsupported();
				break;
			case '@':  // rxvt ignores this completely.
				unsupported();
				break;
			default:
				Log.warn("Unrecognised two-character escape \"" + sequence + "\".");
		}
	}
	
	private void unsupported() {
		Log.warn("Unsupported two-character escape \"" + sequence + "\".");
	}
}
