package terminator;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import e.util.*;

public class TerminatorMenuBar extends JMenuBar {
	public TerminatorMenuBar() {
		add(makeFileMenu());
		add(makeEditMenu());
		//addWindowMenu();
		//addHelpMenu();
	}
	
	public JMenu makeFileMenu() {
		JMenu menu = new JMenu("File");
		menu.add(makeAcceleratedItem(new NewShellAction(), 'N'));
		menu.add(makeAcceleratedItemEx(new NewCommandAction(), 'N', true));
		menu.add(makeAcceleratedItemEx(new ConnectToServerAction(), 'K', true));
		
		menu.add(new JSeparator());
		menu.add(makeAcceleratedItem(new CloseWindowAction(), 'W'));
		menu.add(makeAcceleratedItemEx(new SaveAsAction(), 'S', true));
		
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
		menu.add(makeAcceleratedItem(new CopyAction(), 'C'));
		menu.add(makeAcceleratedItem(new PasteAction(), 'V'));
		menu.add(makeAcceleratedItem(new SelectAllAction(), 'A'));
		
		menu.add(new JSeparator());
		menu.add(makeAcceleratedItem(new FindAction(), 'F'));
		
		menu.add(new JSeparator());
		menu.add(makeAcceleratedItem(new ClearScrollbackAction(), 'K'));

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

	class CloseWindowAction extends AbstractAction {
		public CloseWindowAction() {
			super("Close Window");
		}
		public void actionPerformed(ActionEvent e) {
			//paste();
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
	
	class CopyAction extends AbstractAction {
		public CopyAction() {
			super("Copy");
		}
		public void actionPerformed(ActionEvent e) {
			// FIXME: we should probably have an "explicit copy" mode.
		}
	}
	
	class PasteAction extends AbstractAction {
		public PasteAction() {
			super("Paste");
		}
		public void actionPerformed(ActionEvent e) {
			//paste();
		}
	}
	
	class SelectAllAction extends AbstractAction {
		public SelectAllAction() {
			super("Select All");
		}
		public void actionPerformed(ActionEvent e) {
			//paste();
		}
	}
	
	class FindAction extends AbstractAction {
		public FindAction() {
			super("Find");
		}
		public void actionPerformed(ActionEvent e) {
			//paste();
		}
	}
	
	class ClearScrollbackAction extends AbstractAction {
		public ClearScrollbackAction() {
			super("Clear Scrollback");
		}
		public void actionPerformed(ActionEvent e) {
			//paste();
		}
	}
	
	public JMenuItem makeAcceleratedItem(Action action, char character) {
		return makeAcceleratedItemEx(action, character, false);
	}
	
	public JMenuItem makeAcceleratedItemEx(Action action, char character, boolean shifted) {
		JMenuItem item = new JMenuItem(action);
		int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		if (shifted) modifiers |= InputEvent.SHIFT_MASK;
		String keycodeName = "VK_" + character;
		int keycode;
		try {
			keycode = KeyEvent.class.getField(keycodeName).getInt(KeyEvent.class);
			KeyStroke keyStroke = KeyStroke.getKeyStroke(keycode, modifiers);
			item.setAccelerator(keyStroke);
		} catch (Exception e) {
			Log.warn("Couldn't find virtual keycode for '" + character);
		}
		return item;
	}

}
