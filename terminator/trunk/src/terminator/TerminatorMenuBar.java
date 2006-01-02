package terminator;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import terminator.view.*;
import terminator.view.highlight.*;

/**
 * Provides a menu bar for Mac OS, and acts as a source of Action instances for
 * the pop-up menu on all platforms.
 */
public class TerminatorMenuBar extends EMenuBar {
	private Action[] customWindowMenuItems = new Action[] {
		new NextTabAction(),
		new PreviousTabAction()
	};
	
	public TerminatorMenuBar() {
		add(makeFileMenu());
		add(makeEditMenu());
		add(makeScrollbackMenu());
		add(WindowMenu.getSharedInstance().makeJMenu(customWindowMenuItems));
		add(makeHelpMenu());
	}
	
	private JMenu makeFileMenu() {
		JMenu menu = new JMenu("File");
		menu.add(new NewShellAction());
		menu.add(new NewCommandAction());
		
		menu.addSeparator();
		menu.add(new NewTabAction());
		menu.add(new NewCommandTabAction());
		menu.add(new DetachTabAction());
		
		menu.addSeparator();
		menu.add(new CloseAction());
		
		menu.addSeparator();
		menu.add(new ShowInfoAction());
		menu.add(new ResetAction());
		
		/*
		if (GuiUtilities.isMacOs() == false) {
			menu.addSeparator();
			menu.add(new ExitAction());
		}
		*/
		return menu;
	}
	
	private JMenu makeEditMenu() {
		JMenu menu = new JMenu("Edit");
		menu.add(new CopyAction());
		menu.add(new PasteAction());
		menu.add(new SelectAllAction());
		
		menu.addSeparator();
		menu.add(new FindAction());
		menu.add(new FindNextAction());
		menu.add(new FindPreviousAction());
		
		menu.addSeparator();
		menu.add(new FindNextLinkAction());
		menu.add(new FindPreviousLinkAction());
		return menu;
	}
	
	private JMenu makeScrollbackMenu() {
		JMenu menu = new JMenu("Scrollback");
		
		menu.add(new ScrollToTopAction());
		menu.add(new ScrollToBottomAction());
		
		menu.addSeparator();
		menu.add(new PageUpAction());
		menu.add(new PageDownAction());
		
		menu.addSeparator();
		menu.add(new LineUpAction());
		menu.add(new LineDownAction());
		
		menu.addSeparator();
		menu.add(new ClearScrollbackAction());
		
		return menu;
	}
	
	private JMenu makeHelpMenu() {
		HelpMenu helpMenu = new HelpMenu("Terminator");
		helpMenu.setWebsite("http://software.jessies.org/terminator/");
		helpMenu.setChangeLog("http://software.jessies.org/terminator/ChangeLog.html");
		return helpMenu.makeJMenu();
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
	
	private static KeyStroke makeKeyStroke(String key) {
		return GuiUtilities.makeKeyStrokeForModifier(KEYBOARD_EQUIVALENT_MODIFIER, key, false);
	}
	
	private static KeyStroke makeShiftedKeyStroke(String key) {
		return GuiUtilities.makeKeyStrokeForModifier(KEYBOARD_EQUIVALENT_MODIFIER, key, true);
	}
	
	private static Component getFocusedComponent() {
		return KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
	}
	
	private static JTerminalPane getFocusedTerminalPane() {
		return (JTerminalPane) SwingUtilities.getAncestorOfClass(JTerminalPane.class, getFocusedComponent());
	}
	
	private static TerminatorFrame getFocusedTerminatorFrame() {
		return (TerminatorFrame) SwingUtilities.getAncestorOfClass(TerminatorFrame.class, getFocusedComponent());
	}
	
	private static boolean focusedFrameHasMultipleTabs() {
		TerminatorFrame frame = getFocusedTerminatorFrame();
		return frame != null && frame.hasMultipleTabs();
	}
	
	//
	// Any new Action should probably subclass one of these abstract
	// classes. Only if your action requires neither a frame nor a
	// terminal pane (i.e. acts upon the application as a whole) should
	// you subclass AbstractAction directly.
	//
	
	/**
	 * Superclass for actions that just need a TerminatorFrame.
	 */
	private abstract static class AbstractFrameAction extends AbstractAction {
		public AbstractFrameAction(String name) {
			super(name);
		}
		
		public void actionPerformed(ActionEvent e) {
			TerminatorFrame frame = getFocusedTerminatorFrame();
			if (frame != null) {
				performFrameAction(frame);
			}
		}
		
		protected abstract void performFrameAction(TerminatorFrame frame);
		
		@Override
		public boolean isEnabled() {
			return (getFocusedTerminatorFrame() != null);
		}
	}
	
	/**
	 * Superclass for actions that need a JTerminalPane (that may or may
	 * not have a frame to itself).
	 */
	private abstract static class AbstractPaneAction extends AbstractAction {
		public AbstractPaneAction(String name) {
			super(name);
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminalPane = getFocusedTerminalPane();
			if (terminalPane != null) {
				performPaneAction(terminalPane);
			}
		}
		
		protected abstract void performPaneAction(JTerminalPane terminalPane);
		
		@Override
		public boolean isEnabled() {
			return (getFocusedTerminatorFrame() != null);
		}
	}
	
	/**
	 * Superclass for actions that need a JTerminalPane that must be on a
	 * tab rather than in a frame by itself.
	 */
	private abstract static class AbstractTabAction extends AbstractFrameAction {
		public AbstractTabAction(String name) {
			super(name);
		}
		
		@Override
		public boolean isEnabled() {
			return focusedFrameHasMultipleTabs();
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
			JTerminalPane terminalPane = boundTerminalPane;
			if (terminalPane == null) {
				terminalPane = getFocusedTerminalPane();
			}
			if (terminalPane != null) {
				performOn(terminalPane);
			}
		}
		
		public abstract void performOn(JTerminalPane terminalPane);
		
		
		@Override
		public boolean isEnabled() {
			return (boundTerminalPane != null || getFocusedTerminalPane() != null);
		}
	}
	
	//
	// Terminator's Actions.
	//
	
	public static class NewShellAction extends AbstractAction {
		public NewShellAction() {
			super("New Shell");
			putValue(ACCELERATOR_KEY, makeKeyStroke("N"));
		}
		
		public void actionPerformed(ActionEvent e) {
			Terminator.getSharedInstance().openFrame(JTerminalPane.newShell());
		}
	}
	
	public static class NewTabAction extends AbstractFrameAction {
		public NewTabAction() {
			super("New Shell Tab");
			putValue(ACCELERATOR_KEY, makeKeyStroke("T"));
		}
		
		@Override
		protected void performFrameAction(TerminatorFrame frame) {
			frame.addTab(JTerminalPane.newShell());
		}
	}
	
	private static JTerminalPane runCommand(JFrame frame) {
		JTextField commandField = new JTextField(40);
		
		JFrame parent = (frame != null) ? frame : Terminator.getSharedInstance().getSuitableParentFrameForForms();
		FormBuilder form = new FormBuilder(parent, "Run Command");
		FormPanel formPanel = form.getFormPanel();
		formPanel.addRow("Command:", commandField);
		form.getFormDialog().setRememberBounds(false);
		
		boolean shouldRun = form.show("Run");
		if (shouldRun == false) {
			return null;
		}
		return JTerminalPane.newCommandWithName(commandField.getText(), null);
	}
	
	public static class NewCommandAction extends AbstractAction {
		public NewCommandAction() {
			super("New Command...");
			putValue(ACCELERATOR_KEY, makeShiftedKeyStroke("N"));
		}
		
		public void actionPerformed(ActionEvent e) {
			JTerminalPane terminalPane = runCommand(null);
			if (terminalPane != null) {
				Terminator.getSharedInstance().openFrame(terminalPane);
			}
		}
	}
	
	public static class NewCommandTabAction extends AbstractFrameAction {
		public NewCommandTabAction() {
			super("New Command Tab...");
			putValue(ACCELERATOR_KEY, makeShiftedKeyStroke("T"));
		}
		
		@Override
		protected void performFrameAction(TerminatorFrame frame) {
			JTerminalPane terminalPane = runCommand(frame);
			if (terminalPane != null) {
				frame.addTab(terminalPane);
			}
		}
	}
	
	public static class CloseAction extends AbstractPaneAction {
		public CloseAction() {
			super("Close");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("W"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.doCloseAction();
		}
	}
	
	public static class DetachTabAction extends AbstractTabAction {
		public DetachTabAction() {
			super("Detach Tab");
		}
		
		public void performFrameAction(TerminatorFrame frame) {
			frame.detachCurrentTab();
		}
	}
	
	public static class ShowInfoAction extends AbstractPaneAction {
		public ShowInfoAction() {
			super("Show Info");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("I"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			InfoDialog.getSharedInstance().showInfoDialogFor(terminalPane);
		}
	}
	
	public static class CopyAction extends AbstractPaneAction {
		public CopyAction() {
			super("Copy");
			putValue(ACCELERATOR_KEY, makeKeyStroke("C"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.doCopyAction();
		}
	}
	
	public static class PasteAction extends AbstractPaneAction {
		public PasteAction() {
			super("Paste");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("V"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.doPasteAction();
		}
	}
	
	private static class SelectAllAction extends AbstractPaneAction {
		public SelectAllAction() {
			super("Select All");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("A"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.selectAll();
		}
	}
	
	public static class ResetAction extends AbstractPaneAction {
		public ResetAction() {
			super("Reset");
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.reset();
		}
	}
	
	public static class FindAction extends AbstractPaneAction {
		public FindAction() {
			super("Find...");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("F"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			FindDialog.getSharedInstance().showFindDialogFor(terminalPane);
		}
	}
	
	public static class FindNextAction extends BindableAction {
		public FindNextAction() {
			super("Find Next");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("G"));
		}
		
		public void performOn(JTerminalPane terminalPane) {
			terminalPane.getTextPane().findNext(FindHighlighter.class);
		}
	}
	
	public static class FindNextLinkAction extends AbstractPaneAction {
		public FindNextLinkAction() {
			super("Find Next Link");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeShiftedKeyStroke("G"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.getTextPane().findNext(HyperlinkHighlighter.class);
		}
	}
	
	public static class FindPreviousAction extends BindableAction {
		public FindPreviousAction() {
			super("Find Previous");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("D"));
		}
		
		public void performOn(JTerminalPane terminalPane) {
			terminalPane.getTextPane().findPrevious(FindHighlighter.class);
		}
	}
	
	public static class FindPreviousLinkAction extends AbstractPaneAction {
		public FindPreviousLinkAction() {
			super("Find Previous Link");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeShiftedKeyStroke("D"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.getTextPane().findPrevious(HyperlinkHighlighter.class);
		}
	}
	
	public static class ScrollToTopAction extends AbstractPaneAction {
		public ScrollToTopAction() {
			super("Scroll To Top");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("HOME"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.getTextPane().scrollToTop();
		}
	}
	
	public static class ScrollToBottomAction extends AbstractPaneAction {
		public ScrollToBottomAction() {
			super("Scroll To Bottom");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("END"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.getTextPane().scrollToBottom();
		}
	}
	
	public static class PageUpAction extends AbstractPaneAction {
		public PageUpAction() {
			super("Page Up");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("PAGE_UP"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.pageUp();
		}
	}
	
	public static class PageDownAction extends AbstractPaneAction {
		public PageDownAction() {
			super("Page Down");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("PAGE_DOWN"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.pageDown();
		}
	}
	
	public static class LineUpAction extends AbstractPaneAction {
		public LineUpAction() {
			super("Line Up");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("UP"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.lineUp();
		}
	}
	
	public static class LineDownAction extends AbstractPaneAction {
		public LineDownAction() {
			super("Line Down");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("DOWN"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.lineDown();
		}
	}
	
	public static class ClearScrollbackAction extends AbstractPaneAction {
		public ClearScrollbackAction() {
			super("Clear Scrollback");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("K"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.getTextPane().getModel().clearScrollBuffer();
		}
	}
	
	public static class NextTabAction extends AbstractTabAction {
		public NextTabAction() {
			super("Select Next Tab");
			if (GuiUtilities.isMacOs()) {
				// Sun bug 6350813 prevents this from looking right on Mac OS, but at least it's the same keystroke as Safari.
				putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke("CLOSE_BRACKET", true));
			} else {
				putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("PAGE_DOWN"));
			}
		}
		
		@Override
		protected void performFrameAction(TerminatorFrame frame) {
			frame.switchToNextTab();
		}
	}
	
	public static class PreviousTabAction extends AbstractTabAction {
		public PreviousTabAction() {
			super("Select Previous Tab");
			if (GuiUtilities.isMacOs()) {
				// Sun bug 6350813 prevents this from looking right on Mac OS, but at least it's the same keystroke as Safari.
				putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke("OPEN_BRACKET", true));
			} else {
				putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("PAGE_UP"));
			}
		}
		
		@Override
		protected void performFrameAction(TerminatorFrame frame) {
			frame.switchToPreviousTab();
		}
	}
}
