package terminator;

import java.awt.*;

import terminator.view.*;

/**
A TerminalPaneMaster is the master of a JTerminalPane, and allows the JTerminalPane to request its master
to do stuff, like opening new tabs, closing itself, or whatever.

@author Phil Norman
*/

public interface TerminalPaneMaster {
	public void closeTerminalPane(JTerminalPane victim);
	
	public void openNewWindow();
	
	public void openShellPane(boolean focusOnNewTab);
	
	public void openCommandPane(String command, boolean focusOnNewTab);
	
	public void showFindDialogFor(JTextBuffer textToFindIn);
	
	public void setTerminalSize(Dimension size);
	
	public void terminalNameChanged(JTerminalPane terminal);
}
