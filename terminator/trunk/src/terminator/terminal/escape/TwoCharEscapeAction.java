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

	public void perform(TextBuffer listener) {
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
	
	private void unsupported() {
		Log.warn("Unsupported two-character escape \"" + sequence + "\".");
	}
}
