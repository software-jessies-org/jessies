package terminator.view;

import e.gui.*;
import java.awt.*;

/**
 * Contains JTerminatorPanes and provides their host environment.
 * 
 * Not tied to the rest of Terminator to facilitate embedding.
 */
public interface TerminalPaneHost {
	public void cycleTab(int delta);
	public void moveCurrentTab(int direction);
	public void setSelectedTabIndex(int index);
	
	public boolean confirmClose(String processesUsingTty);
	public void closeTerminalPane(JTerminalPane terminalPane);
	
	public void updateFrameTitle();
	public void terminalNameChanged(JTerminalPane terminalPane);
	
	public void setTerminalSize(Dimension size);
	
	public MenuItemProvider createMenuItemProvider(JTerminalPane terminalPane);
}
