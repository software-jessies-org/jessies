package terminator.view;

import java.awt.Dimension;

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

	boolean isShowingMenu();
	
	TerminalPaneActions createActions(JTerminalPane terminalPane);

}
