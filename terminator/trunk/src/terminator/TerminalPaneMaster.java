package terminator;

import java.awt.*;

/**
A TerminalPaneMaster is the master of a JTerminalPane, and allows the JTerminalPane to request its master
to do stuff, like opening new tabs, or whatever.

@author Phil Norman
*/
public interface TerminalPaneMaster {
	public void openShellPane(boolean focusOnNewTab);
	
	public void openCommandPane(String command, boolean focusOnNewTab);
	
	public void setTerminalSize(Dimension size);
}
