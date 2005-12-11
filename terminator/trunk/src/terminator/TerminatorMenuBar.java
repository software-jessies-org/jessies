package terminator;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import e.gui.*;
import e.util.*;
import terminator.view.*;
import terminator.view.highlight.*;

/**
 * Provides a menu bar for Mac OS, and acts as a source of Action instances for
 * the pop-up menu on all platforms.
 */
public class TerminatorMenuBar extends JMenuBar {
	private Action[] customWindowMenuItems = new Action[] {
		new NextTabAction(),
		new PreviousTabAction()
	};
	
	public TerminatorMenuBar() {
		add(makeFileMenu());
		add(makeEditMenu());
		add(makeScrollbackMenu());
		//add(makeFontMenu());
		add(WindowMenu.getSharedInstance().makeJMenu(customWindowMenuItems));
		//addHelpMenu();
	}
	
	private JMenu makeFileMenu() {
		JMenu menu = new JMenu("File");
		menu.add(new JMenuItem(new NewShellAction()));
		menu.add(new JMenuItem(new NewTabAction()));
		//menu.add(makeAcceleratedItemEx(new NewCommandAction(), 'N', true));
		//menu.add(makeAcceleratedItemEx(new ConnectToServerAction(), 'K', true));
		
		menu.add(new JSeparator());
		menu.add(new JMenuItem(new CloseAction()));
		//menu.add(new JMenuItem(new SaveAsAction()));
		
		menu.add(new JSeparator());
		menu.add(new JMenuItem(new ShowInfoAction()));
		menu.add(new JMenuItem(new ResetAction()));
		
		/*
		if (GuiUtilities.isMacOs() == false) {
			menu.add(new JSeparator());
			menu.add(new JMenuItem(new ExitAction()));
		}
		*/
		return menu;
	}
	
	private JMenu makeEditMenu() {
		JMenu menu = new JMenu("Edit");
		menu.add(new JMenuItem(new CopyAction()));
		menu.add(new JMenuItem(new PasteAction()));
		menu.add(new JMenuItem(new SelectAllAction()));
		
		menu.add(new JSeparator());
		menu.add(new JMenuItem(new FindAction()));
		menu.add(new JMenuItem(new FindNextAction()));
		menu.add(new JMenuItem(new FindPreviousAction()));
		
		menu.add(new JSeparator());
		menu.add(new JMenuItem(new FindNextLinkAction()));
		menu.add(new JMenuItem(new FindPreviousLinkAction()));
		return menu;
	}
	
	private JMenu makeScrollbackMenu() {
		JMenu menu = new JMenu("Scrollback");
		
		menu.add(new JMenuItem(new ScrollToTopAction()));
		menu.add(new JMenuItem(new ScrollToBottomAction()));
		
		menu.add(new JSeparator());
		menu.add(new JMenuItem(new PageUpAction()));
		menu.add(new JMenuItem(new PageDownAction()));
		
		menu.add(new JSeparator());
		menu.add(new JMenuItem(new LineUpAction()));
		menu.add(new JMenuItem(new LineDownAction()));
		
		menu.add(new JSeparator());
		menu.add(new JMenuItem(new ClearScrollbackAction()));
		
		return menu;
	}
	
	private JMenu makeFontMenu() {
		JMenu menu = new JMenu("Font");
		
		//menu.add(new JMenuItem(new BiggerFontAction()));
		//menu.add(new JMenuItem(new SmallerFontAction()));
		
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
	
	public static KeyStroke makeShiftedKeyStroke(String key) {
		return GuiUtilities.makeKeyStrokeForModifier(KEYBOARD_EQUIVALENT_MODIFIER, key, true);
	}
	
	private static Component getFocusedComponent() {
		return KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
	}
	
	public static JTerminalPane getFocusedTerminalPane() {
		return (JTerminalPane) SwingUtilities.getAncestorOfClass(JTerminalPane.class, getFocusedComponent());
	}
	
	public static TerminatorFrame getFocusedTerminatorFrame() {
		return (TerminatorFrame) SwingUtilities.getAncestorOfClass(TerminatorFrame.class, getFocusedComponent());
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
	
	public static class NewTabAction extends AbstractAction {
		public NewTabAction() {
			super("New Tab");
			putValue(ACCELERATOR_KEY, makeKeyStroke("T"));
		}
		
		public void actionPerformed(ActionEvent e) {
			TerminatorFrame frame = getFocusedTerminatorFrame();
			if (frame != null) {
				frame.openNewTab();
			}
		}
	}
	
	class NewCommandAction extends AbstractAction {
		public NewCommandAction() {
			super("New Command...");
		}
		public void actionPerformed(ActionEvent e) {
			//pasteSystemClipboard();
		}
	}

	class ConnectToServerAction extends AbstractAction {
		public ConnectToServerAction() {
			super("Connect to Server...");
		}
		public void actionPerformed(ActionEvent e) {
			//pasteSystemClipboard();
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
	
	public static class ShowInfoAction extends AbstractAction {
		public ShowInfoAction() {
			super("Show Info");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("I"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				InfoDialog.getSharedInstance().showInfoDialogFor(terminal);
			}
		}
	}
	
	class SaveAsAction extends AbstractAction {
		public SaveAsAction() {
			super("Save As...");
		}
		public void actionPerformed(ActionEvent e) {
			//pasteSystemClipboard();
		}
	}
	
	public static class CopyAction extends AbstractAction {
		public CopyAction() {
			super("Copy");
			putValue(ACCELERATOR_KEY, makeKeyStroke("C"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				terminal.doCopyAction();
			}
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
	
	private static class SelectAllAction extends AbstractAction {
		public SelectAllAction() {
			super("Select All");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("A"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				terminal.selectAll();
			}
		}
	}
	
	public static class ResetAction extends AbstractAction {
		public ResetAction() {
			super("Reset");
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				terminal.reset();
			}
		}
	}
	
	public static class FindAction extends AbstractAction {
		public FindAction() {
			super("Find...");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("F"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				FindDialog.getSharedInstance().showFindDialogFor(terminal);
			}
		}
	}
	
	public abstract static class BindableAction extends AbstractAction {
		private JTerminalPane boundTerminalPane;
		
		public BindableAction(String name) {
			super(name);
		}
		
		public void bindTo(JTerminalPane terminalPane) {
			this.boundTerminalPane = terminalPane;
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = boundTerminalPane;
			if (terminal == null) {
				terminal = getFocusedTerminalPane();
			}
			if (terminal != null) {
				performOn(terminal);
			}
		}
		
		public abstract void performOn(JTerminalPane terminal);
	}
	
	public static class FindNextAction extends BindableAction {
		public FindNextAction() {
			super("Find Next");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("G"));
		}
		
		public void performOn(JTerminalPane terminal) {
			terminal.getTextPane().findNext(FindHighlighter.class);
		}
	}
	
	public static class FindNextLinkAction extends AbstractAction {
		public FindNextLinkAction() {
			super("Find Next Link");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeShiftedKeyStroke("G"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				terminal.getTextPane().findNext(HyperlinkHighlighter.class);
			}
		}
	}
	
	public static class FindPreviousAction extends BindableAction {
		public FindPreviousAction() {
			super("Find Previous");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("D"));
		}
		
		public void performOn(JTerminalPane terminal) {
			terminal.getTextPane().findPrevious(FindHighlighter.class);
		}
	}
	
	public static class FindPreviousLinkAction extends AbstractAction {
		public FindPreviousLinkAction() {
			super("Find Previous Link");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeShiftedKeyStroke("D"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				terminal.getTextPane().findPrevious(HyperlinkHighlighter.class);
			}
		}
	}
	
	public static class ScrollToTopAction extends AbstractAction {
		public ScrollToTopAction() {
			super("Scroll To Top");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("HOME"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				terminal.getTextPane().scrollToTop();
			}
		}
	}
	
	public static class ScrollToBottomAction extends AbstractAction {
		public ScrollToBottomAction() {
			super("Scroll To Bottom");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("END"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				terminal.getTextPane().scrollToBottom();
			}
		}
	}
	
	public static class PageUpAction extends AbstractAction {
		public PageUpAction() {
			super("Page Up");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("PAGE_UP"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				terminal.pageUp();
			}
		}
	}
	
	public static class PageDownAction extends AbstractAction {
		public PageDownAction() {
			super("Page Down");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("PAGE_DOWN"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				terminal.pageDown();
			}
		}
	}
	
	public static class LineUpAction extends AbstractAction {
		public LineUpAction() {
			super("Line Up");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("UP"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				terminal.lineUp();
			}
		}
	}
	
	public static class LineDownAction extends AbstractAction {
		public LineDownAction() {
			super("Line Down");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("DOWN"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminal = getFocusedTerminalPane();
			if (terminal != null) {
				terminal.lineDown();
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
				terminal.getTextPane().getModel().clearScrollBuffer();
			}
		}
	}
	
	public static class NextTabAction extends AbstractAction {
		public NextTabAction() {
			super("Select Next Tab");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke(GuiUtilities.isMacOs() ? "RIGHT" : "PAGE_DOWN"));
		}
		
		public void actionPerformed(ActionEvent e) {
			TerminatorFrame frame = getFocusedTerminatorFrame();
			if (frame != null) {
				frame.switchToNextTab();
			}
		}
		
		// FIXME: implement isEnabled.
	}
	
	public static class PreviousTabAction extends AbstractAction {
		public PreviousTabAction() {
			super("Select Previous Tab");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke(GuiUtilities.isMacOs() ? "LEFT" : "PAGE_UP"));
		}
		
		public void actionPerformed(ActionEvent e) {
			TerminatorFrame frame = getFocusedTerminatorFrame();
			if (frame != null) {
				frame.switchToPreviousTab();
			}
		}
		
		// FIXME: implement isEnabled.
	}
}
