package terminator;

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
	private static int defaultKeyStrokeModifiers = GuiUtilities.getDefaultKeyStrokeModifier();
	
	private Action[] customWindowMenuItems = new Action[] {
		new MoveTabAction(+1),
		new MoveTabAction(-1),
		new CycleTabAction(+1),
		new CycleTabAction(-1)
	};
	
	public TerminatorMenuBar() {
		add(makeFileMenu());
		add(makeEditMenu());
		add(makeScrollbackMenu());
		if (GuiUtilities.isMacOs()) {
			add(WindowMenu.getSharedInstance().makeJMenu(customWindowMenuItems));
		} else {
			add(makeTabsMenu());
		}
		add(makeHelpMenu());
	}
	
	private JMenu makeFileMenu() {
		JMenu menu = GuiUtilities.makeMenu("File", 'F');
		// Alt-F for Find is more useful.
		if (Terminator.getPreferences().getBoolean(TerminatorPreferences.USE_ALT_AS_META) == false) {
			menu.setMnemonic(0);
		}
		menu.add(new NewShellAction());
		menu.add(new NewCommandAction());
		
		menu.addSeparator();
		menu.add(new NewTabAction());
		menu.add(new NewCommandTabAction());
		
		menu.addSeparator();
		menu.add(new CloseAction());
		
		menu.addSeparator();
		menu.add(new ShowInfoAction());
		menu.add(new ResetAction());
		
		return menu;
	}
	
	private JMenu makeEditMenu() {
		JMenu menu = GuiUtilities.makeMenu("Edit", 'E');
		menu.add(new CopyAction());
		menu.add(new PasteAction());
		menu.add(new SelectAllAction());
		
		menu.addSeparator();
		menu.add(new FindAction());
		menu.add(new FindNextAction());
		menu.add(new FindPreviousAction());
		menu.add(new CancelFindAction());
		
		Terminator.getPreferences().initPreferencesMenuItem(menu);
		
		return menu;
	}
	
	private JMenu makeScrollbackMenu() {
		JMenu menu = GuiUtilities.makeMenu("Scrollback", 'S');
		
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
	
	private JMenu makeTabsMenu() {
		final JMenu menu = GuiUtilities.makeMenu("Tabs", 'T');
		menu.add(new DetachTabAction());
		for (Action action : customWindowMenuItems) {
			menu.add(action);
		}
		return menu;
	}
	
	private JMenu makeHelpMenu() {
		HelpMenu helpMenu = new HelpMenu();
		return helpMenu.makeJMenu();
	}
	
	/**
	 * Tests whether the given event corresponds to a keyboard equivalent.
	 */
	public static boolean isKeyboardEquivalent(KeyEvent event) {
		// Windows seems to use ALT_MASK|CTRL_MASK instead of ALT_GRAPH_MASK.
		// We don't want those events, despite the lax comparison later.
		final int fakeWindowsAltGraph = InputEvent.ALT_MASK | InputEvent.CTRL_MASK;
		if ((event.getModifiers() & fakeWindowsAltGraph) == fakeWindowsAltGraph) {
			return false;
		}
		// This comparison is more inclusive than you might expect.
		// If the default modifier is alt, say, we still want to accept alt+shift.
		final int expectedModifiers = defaultKeyStrokeModifiers;
		return ((event.getModifiers() & expectedModifiers) == expectedModifiers);
	}
	
	/**
	 * Returns the appropriate keystroke modifiers for terminal-related actions.
	 */
	public static int getDefaultKeyStrokeModifiers() {
		return defaultKeyStrokeModifiers;
	}
	
	/**
	 * Sets the appropriate keystroke modifiers for terminal-related actions.
	 * On Mac OS, the command key is spare anyway, but on Linux and Windows we'd normally use control.
	 * That's no good in a terminal emulator, because we need to be able to pass things like control-c through.
	 */ 
	public static void setDefaultKeyStrokeModifiers(int modifiers) {
		defaultKeyStrokeModifiers = modifiers;
	}
	
	// Semi-duplicated from GuiUtilities so we can apply our custom modifiers if needed.
	// Use the GuiUtilities version for actions that aren't terminal-related, to get the system-wide defaults.
	private static KeyStroke makeKeyStroke(String key) {
		return GuiUtilities.makeKeyStrokeWithModifiers(defaultKeyStrokeModifiers, key);
	}
	
	// Semi-duplicated from GuiUtilities so we can apply our custom modifiers if needed.
	// Use the GuiUtilities version for actions that aren't terminal-related, to get the system-wide defaults.
	private static KeyStroke makeShiftedKeyStroke(String key) {
		return GuiUtilities.makeKeyStrokeWithModifiers(defaultKeyStrokeModifiers | InputEvent.SHIFT_MASK, key);
	}
	
	private static Component getFocusedComponent() {
		return KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
	}
	
	private static JTerminalPane getFocusedTerminalPane() {
		return (JTerminalPane) SwingUtilities.getAncestorOfClass(JTerminalPane.class, getFocusedComponent());
	}
	
	public static TerminatorFrame getFocusedTerminatorFrame() {
		return (TerminatorFrame) SwingUtilities.getAncestorOfClass(TerminatorFrame.class, getFocusedComponent());
	}
	
	private static boolean focusedFrameHasMultipleTabs() {
		TerminatorFrame frame = getFocusedTerminatorFrame();
		return (frame != null) && (frame.getTerminalPaneCount() > 1);
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
			newShell();
		}
		
		public static void newShell() {
			Terminator.getSharedInstance().openFrame(JTerminalPane.newShell());
		}
	}
	
	public static class NewTabAction extends AbstractAction {
		public NewTabAction() {
			super("New Shell Tab");
			putValue(ACCELERATOR_KEY, makeKeyStroke("T"));
		}
		
		public void actionPerformed(ActionEvent e) {
			TerminatorFrame frame = getFocusedTerminatorFrame();
			
			// On Mac OS, if the user hits C-T multiple times in quick succession while we've got no window up, we end up here with no focused frame.
			// Without this hack, that means that (on my machine) for three C-Ts, I get two in one new window and a third in a second new window.
			// With this hack, the focus even seems to work itself out in the end.
			// I don't know of any cleaner way to do this.
			Frames frames = Terminator.getSharedInstance().getFrames();
			if (frame == null && frames.isEmpty() == false) {
				frame = (TerminatorFrame) frames.getFrame();
			}
			
			if (frame != null) {
				frame.addTab(JTerminalPane.newShell());
			} else {
				// There's no existing frame, so interpret "New Shell Tab..." as "New Shell...".
				NewShellAction.newShell();
			}
		}
	}
	
	public static class NewCommandAction extends AbstractAction {
		public NewCommandAction() {
			super("New Command...");
			putValue(ACCELERATOR_KEY, makeShiftedKeyStroke("N"));
		}
		
		public void actionPerformed(ActionEvent e) {
			newCommand();
		}
		
		public static void newCommand() {
			final JTerminalPane terminalPane = new CommandDialog().askForCommandToRun();
			if (terminalPane != null) {
				// We need to invokeLater to avoid a race condition where (I think) the VK_ENTER hasn't finished processing and gets dispatched again to the new terminal.
				// Chris Reece saw this on Mac OS if he did shift-command T, tab, return with no other windows open.
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						Terminator.getSharedInstance().openFrame(terminalPane);
					}
				});
			}
		}
	}
	
	public static class NewCommandTabAction extends AbstractAction {
		public NewCommandTabAction() {
			super("New Command Tab...");
			putValue(ACCELERATOR_KEY, makeShiftedKeyStroke("T"));
		}
		
		public void actionPerformed(ActionEvent e) {
			TerminatorFrame frame = getFocusedTerminatorFrame();
			if (frame != null) {
				JTerminalPane terminalPane = new CommandDialog().askForCommandToRun();
				if (terminalPane != null) {
					frame.addTab(terminalPane);
				}
			} else {
				// There's no existing frame, so interpret "New Command Tab..." as "New Command...".
				NewCommandAction.newCommand();
			}
		}
	}
	
	public static class CloseAction extends AbstractPaneAction {
		public CloseAction() {
			super("Close");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("W"));
			GnomeStockIcon.configureAction(this);
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.doCheckedCloseAction();
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
			GnomeStockIcon.configureAction(this);
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
			GnomeStockIcon.configureAction(this);
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
			GnomeStockIcon.configureAction(this);
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
			GnomeStockIcon.configureAction(this);
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
			GnomeStockIcon.configureAction(this);
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
			terminalPane.getTerminalView().findNext(FindHighlighter.class);
		}
	}
	
	public static class FindPreviousAction extends BindableAction {
		public FindPreviousAction() {
			super("Find Previous");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("D"));
		}
		
		public void performOn(JTerminalPane terminalPane) {
			terminalPane.getTerminalView().findPrevious(FindHighlighter.class);
		}
	}
	
	public static class CancelFindAction extends AbstractPaneAction {
		public CancelFindAction() {
			super("Cancel Find");
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			TerminalView view = terminalPane.getTerminalView();
			view.getHighlighterOfClass(FindHighlighter.class).forgetPattern(view);
		}
	}
	
	public static class ScrollToTopAction extends AbstractPaneAction {
		public ScrollToTopAction() {
			super("Scroll To Top");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("HOME"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.getTerminalView().scrollToTop();
		}
	}
	
	public static class ScrollToBottomAction extends AbstractPaneAction {
		public ScrollToBottomAction() {
			super("Scroll To Bottom");
			putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke("END"));
		}
		
		@Override
		protected void performPaneAction(JTerminalPane terminalPane) {
			terminalPane.getTerminalView().scrollToEnd();
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
			terminalPane.getTerminalView().getModel().clearScrollBuffer();
		}
	}
	
	public static class CycleTabAction extends AbstractTabAction {
		private int delta;
		
		public CycleTabAction(int delta) {
			super(delta < 0 ? "Select Previous Tab" : "Select Next Tab");
			this.delta = delta;
			
			// The choice of keystrokes has gone back and forth a lot.
			
			// Originally, Phil wanted alt left/right, but his implementation didn't work, and we needed a proper Action we could put on the "Window" menu for Mac OS.
			// I wanted command { and command }, like Safari.
			// Sun bug 6350813 prevents us from doing that directly, but we tried command-shift [ and command-shift ].
			// Elias Naur explained that not only does that look wrong (even though it feels right on English keyboards), it doesn't work at all on Danish keyboards.
			
			// Ed did a quick survey and reported that Camino uses command-option left/right; Safari uses command {/}, while other programs (Adium, for example) use command left/right.
			// konsole uses shift left/right, and gnome-terminal uses control page up/page down.
			// Firefox uses control tab/control-shift tab, presumably because it had already used alt left/right for back/forward.
			// Firefox also supports control page up/page down.
			
			// We already support the control-tab sequences in doKeyboardTabSwitch, but control-shift tab isn't well known, and is quite uncomfortable.
			// There is some precedent for the left and right arrow keys, they are likely to be reasonably accessible on all keyboards, and they don't require the uncomfortable use of multiple modifier keys.
			
			// Alt left/right (which is what this will be except on Mac OS) is actually a combination we ought to report to our clients.
			// We've never implemented modifier key reporting, though, so we'll worry about that when someone actually complains.
			
			// If anyone complains about the removal of the Safari keys, we could add them back in doKeyboardTabSwitch.
			
			if (Terminator.getPreferences().getBoolean(TerminatorPreferences.USE_ALT_AS_META)) {
				// Aaron Harnly points out that without this special case, USE_ALT_AS_META causes "cycle tab" and "move tab" to share control-shift left/right.
				// Since the tab tool tip already recommends Firefox-like control-tab/control-shift-tab, let's do the same on the menu.
				// We use KeyStroke.getKeyStroke directly here because we know exactly what keystrokes we want.
				// The whole point of this code is to avoid having Terminator's custom modifiers applied.
				putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_MASK | (delta < 0 ? InputEvent.SHIFT_MASK : 0)));
			} else {
				// Given the above, why keep advertising these?
				// 1. We have no manual documenting the alternatives.
				// 2. For hands like enh's, these are easier to type.
				putValue(ACCELERATOR_KEY, TerminatorMenuBar.makeKeyStroke(delta < 0 ? "LEFT" : "RIGHT"));
			}
		}
		
		@Override protected void performFrameAction(TerminatorFrame frame) {
			frame.cycleTab(delta);
		}
	}
	
	public static class MoveTabAction extends AbstractTabAction {
		private int delta;
		
		public MoveTabAction(int delta) {
			super(delta < 0 ? "Move Tab Left" : "Move Tab Right");
			this.delta = delta;
			
			// Apple's Terminal doesn't yet support tabs.
			// Firefox only supports drag and drop; there's no keyboard tab movement.
			// gnome-terminal uses control-shift page up/page down.
			// konsole uses control-shift left/right.
			// We support those in our KeyEvent-handling code, but they're secret.
			
			// Our default, advertised, keystrokes are alt-shift left/right (command-shift left/right on Mac OS), by analogy to our next/previous tab keystrokes.
			
			putValue(ACCELERATOR_KEY, makeShiftedKeyStroke(delta < 0 ? "LEFT" : "RIGHT"));
		}
		
		@Override
		protected void performFrameAction(TerminatorFrame frame) {
			frame.moveCurrentTab(delta);
		}
	}
}
