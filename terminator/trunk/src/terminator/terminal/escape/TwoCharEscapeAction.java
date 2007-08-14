package terminator.terminal.escape;

import e.util.*;
import terminator.model.*;
import terminator.terminal.*;

public class TwoCharEscapeAction implements TerminalAction {
	private TerminalControl control;
	private String sequence;
	
	public TwoCharEscapeAction(TerminalControl control, String sequence) {
		this.control = control;
		this.sequence = sequence;
	}

	public void perform(TerminalModel model) {
		switch (sequence.charAt(0)) {
			case '#':  // rxvt: if second char == '8', scr_E().
				unsupported();
				break;
			case '(':
				control.designateCharacterSet(0, sequence.charAt(1));
				break;
			case ')':
				control.designateCharacterSet(1, sequence.charAt(1));
				break;
			case '*':
				control.designateCharacterSet(2, sequence.charAt(1));
				break;
			case '+':
				control.designateCharacterSet(3, sequence.charAt(1));
				break;
			case '$':  // rxvt: scr_charset_set(-2, second char).
				unsupported();
				break;
			case '@':  // rxvt ignores this completely.
				unsupported();
				break;
			default:
				Log.warn("Unrecognized two-character escape \"" + sequence + "\".");
		}
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer("TwoCharEscapeAction[");
		int charSet = "()*+".indexOf(sequence.charAt(0));
		if (charSet == -1) {
			result.append("Unsupported");
		} else {
			result.append("Set char set to ").append(charSet);
		}
		result.append("]");
		return result.toString();
	}
	
	private void unsupported() {
		Log.warn("Unsupported two-character escape \"" + sequence + "\".");
	}
}
