package terminatorn;

/**
A TelnetAction does something to affect a TelnetListener.

@author Phil Norman
*/

public interface TelnetAction {
	public void perform(TelnetListener listener);
}
