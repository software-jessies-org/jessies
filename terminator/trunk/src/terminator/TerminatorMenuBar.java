package terminator;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import e.gui.*;
import e.util.*;
import terminator.view.*;

/**
 * Provides a menu bar for Mac OS, and acts as a source of Action instances for
 * the pop-up menu on all platforms.
 */
public class TerminatorMenuBar extends JMenuBar {
	public TerminatorMenuBar() {
		add(makeFileMenu());
		add(makeEditMenu());
		add(WindowMenu.getSharedInstance().makeJMenu());
		//addHelpMenu();
	}
	
	public JMenu makeFileMenu() {
		JMenu menu = new JMenu("File");
		menu.add(new JMenuItem(new NewShellAction()));
		//menu.add(makeAcceleratedItemEx(new NewCommandAction(), 'N', true));
		//menu.add(makeAcceleratedItemEx(new ConnectToServerAction(), 'K', true));
		
		menu.add(new JSeparator());
		menu.add(new JMenuItem(new CloseAction()));
		//menu.add(new JMenuItem(new SaveAsAction()));
		
		/*
		if (GuiUtilities.isMacOs() == false) {
			menu.add(new JSeparator());
			menu.add(new JMenuItem(new ExitAction()));
		}
		*/
		return menu;
	}
	
	public JMenu makeEditMenu() {
		JMenu menu = new JMenu("Edit");
		menu.add(new JMenuItem(new CopyAction()));
		menu.add(new JMenuItem(new PasteAction()));
		//menu.add(new JMenuItem(new SelectAllAction()));
		
		menu.add(new JSeparator());
		menu.add(new JMenuItem(new FindAction()));
		menu.add(new JMenuItem(new FindNextAction()));
		menu.add(new JMenuItem(new FindPreviousAction()));
		
		menu.add(new JSeparator());
		menu.add(new JMenuItem(new ClearScrollbackAction()));

		return menu;
	}
	
	/**
	 * On the Mac, the Command key (called 'meta' by Java) is always
	 * used for keyboard equivalents. On other systems, Control tends to
	 * be used, but in the special case of terminal emulators this
	 * conflicts with the ability to type control characters. The
	 * traditional work-around has always been to use Alt, which --
	 * conveniently for Mac users -- is in the same place on a PC
	 * keyboard as Command on a Mac keyboard.
	 */
	private static final int KEYBOARD_EQUIVALENT_MODIFIER = GuiUtilities.isMacOs() ? KeyEvent.META_MASK : KeyEvent.ALT_MASK;
	
	/**
	 * Tests whether the given event corresponds to a keyboard
	 * equivalent.
	 */
	public static boolean isKeyboardEquivalent(KeyEvent event) {
		return ((event.getModifiers() & KEYBOARD_EQUIVALENT_MODIFIER) == KEYBOARD_EQUIVALENT_MODIFIER);
	}
	
	public static KeyStroke makeKeyStroke(String key) {
		return GuiUtilities.makeKeyStrokeForModifier(KEYBOARD_EQUIVALENT_MODIFIER, key, false);
	}
	
	public static JTerminalPane getFocusedTerminalPane() {
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		return (JTerminalPane) SwingUtilities.getAncestorOfClass(JTerminalPane.class, focusOwner);
	}
	
	public static class NewShellAction extends AbstractAction {
		public NewShellAction() {
			super("New Shell");
			putValue(ACCELERATOR_KEY, makeKeyStroke("N"));
		}
		
		public void actionPerformed(ActionEvent e) {
			Terminator.getSharedInstance().openFrame();
		}
	}

	class NewCommandAction extends AbstractAction {
		public NewCommandAction() {
			super("New Command...");
		}
		public void actionPerformed(ActionEvent e) {
			//paste();
		}
	}

	class ConnectToServerAction extends AbstractAction {
		public ConnectToServerAction() {
			super("Connect to Server...");
		}
		public void actionPerformed(ActionEvent e) {
			//paste();
		}
	}

	public static class CloseAction extends AbstractAction {
		public CloseAction() {
			super("Close");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("W"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				terminal.doCloseAction();
			}
		}
	}

	class SaveAsAction extends AbstractAction {
		public SaveAsAction() {
			super("Save As...");
		}
		public void actionPerformed(ActionEvent e) {
			//paste();
		}
	}
	
	public static class CopyAction extends AbstractAction {
		public CopyAction() {
			super("Copy");
			putValue(ACCELERATOR_KEY, makeKeyStroke("C"));
		}
		
		public void actionPerformed(ActionEvent e) {
			// FIXME: we should probably have an "explicit copy" mode.
		}
	}
	
	public static class PasteAction extends AbstractAction {
		public PasteAction() {
			super("Paste");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("V"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				terminal.doPasteAction();
			}
		}
	}
	
	/*
	private static class SelectAllAction extends AbstractAction {
		public SelectAllAction() {
			super("Select All");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("A"));
		}
		
		public void actionPerformed(ActionEvent e) {
			terminal.doSelectAllAction();
		}
	}
	*/
	
	public static class FindAction extends AbstractAction {
		public FindAction() {
			super("Find...");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("F"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				FindDialog.getSharedInstance().showFindDialogFor(terminal.getTextPane());
			}
		}
	}
	
	public static class FindNextAction extends AbstractAction {
		public FindNextAction() {
			super("Find Next");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("G"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				FindDialog.getSharedInstance().findNextIn(terminal.getTextPane());
			}
		}
	}
	
	public static class FindPreviousAction extends AbstractAction {
		public FindPreviousAction() {
			super("Find Previous");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("D"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				FindDialog.getSharedInstance().findPreviousIn(terminal.getTextPane());
			}
		}
	}
	
	public static class ClearScrollbackAction extends AbstractAction {
		public ClearScrollbackAction() {
			super("Clear Scrollback");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("K"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				terminal.doClearScrollbackAction();
			}
		}
	}
}
