package terminator.view;

import java.awt.event.KeyEvent;

import e.gui.MenuItemProvider;

/**
 * Actions for a {@link JTerminalPane}.
 */
public interface TerminalPaneActions extends MenuItemProvider {

	/**
	 * On Mac OS, we have the screen menu bar to take care of
	 * all the keyboard equivalents. Elsewhere, we have to detect
	 * the events, and invoke actionPerformed on the relevant
	 * Action ourselves.
	 */
	void handleKeyboardEquivalent(KeyEvent event);
	
}
