package terminator.terminal;

import terminator.model.*;

/**
A TerminalAction does something to affect a TextBuffer.

@author Phil Norman
*/

public interface TerminalAction {
	public void perform(TextBuffer listener);
}
