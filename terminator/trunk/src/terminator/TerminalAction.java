package terminatorn;

/**
A TerminalAction does something to affect a TerminalListener.

@author Phil Norman
*/

public interface TerminalAction {
	public void perform(TerminalListener listener);
}
