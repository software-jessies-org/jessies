package terminatorn;

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
	
	class NewShellAction extends AbstractAction {
		public NewShellAction() {
			super("New Shell");
		}
		public void actionPerformed(ActionEvent e) {
			//paste();
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
