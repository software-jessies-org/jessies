package terminatorn;

import java.awt.*;

/**
A Controller is the master of a JTelnetPane, and allows the JTelnetPane to request its master
to do stuff, like opening new tabs, closing itself, or whatever.

@author Phil Norman
*/

public interface Controller {
	public void closeTelnetPane(JTelnetPane victim);
	
	public void openShellPane(boolean focusOnNewTab);
	
	public void openCommandPane(String command, boolean focusOnNewTab);
	
	public void showFindDialogFor(JTextBuffer textToFindIn);
	
	public void setTerminalSize(Dimension size);
	
	public void terminalNameChanged(JTelnetPane terminal);
}
