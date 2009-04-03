package terminator.view;

import e.gui.*;
import java.awt.*;

/**
 * Contains JTerminatorPanes and provides their host environment.
 * 
 * Not tied to the rest of Terminator to facilitate embedding.
 */
public interface TerminalPaneHost {

	void cycleTab(int delta);
	void moveCurrentTab(int direction);
	void setSelectedTabIndex(int index);
	
	boolean confirmClose(String processesUsingTty);
	void closeTerminalPane(JTerminalPane terminalPane);
	
	void updateFrameTitle();
	void terminalNameChanged(JTerminalPane terminalPane);
	void setTerminalSize(Dimension size);
	
	MenuItemProvider createMenuItemProvider(JTerminalPane terminalPane);
}
