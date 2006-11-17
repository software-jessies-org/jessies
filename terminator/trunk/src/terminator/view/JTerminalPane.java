package terminator.view;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import e.gui.*;
import e.util.*;
import terminator.*;
import terminator.model.*;
import terminator.terminal.*;
import terminator.view.highlight.*;

public class JTerminalPane extends JPanel {
	// The probably over-simplified belief here is that Unix terminals always send ^? and Windows always sends ^H.
	private static final String ERASE_STRING = String.valueOf(GuiUtilities.isWindows() ? Ascii.BS : Ascii.DEL);
	
	private TerminalControl control;
	private JTextBuffer textPane;
	private JScrollPane scrollPane;
	private VisualBellViewport viewport;
	private String name;
	private boolean wasCreatedAsNewShell;
	private Dimension currentSizeInChars;
	private JAsynchronousProgressIndicator outputSpinner;
	private Action[] menuAndKeyActions = new Action[] {
		new TerminatorMenuBar.CopyAction(),
		new TerminatorMenuBar.PasteAction(),
		null,
		new TerminatorMenuBar.NewShellAction(),
		new TerminatorMenuBar.NewTabAction(),
		new TerminatorMenuBar.CloseAction(),
		null,
		new TerminatorMenuBar.FindAction(),
		new TerminatorMenuBar.FindNextAction(),
		new TerminatorMenuBar.FindPreviousAction(),
		null,
		new TerminatorMenuBar.ClearScrollbackAction(),
		null,
		new TerminatorMenuBar.CycleTabAction(1),
		new TerminatorMenuBar.CycleTabAction(-1),
		null,
		new TerminatorMenuBar.ShowInfoAction(),
		new TerminatorMenuBar.ResetAction()
	};
	
	/**
	 * Creates a new terminal with the given name, running the given command.
	 */
	private JTerminalPane(String name, String workingDirectory, List<String> command, boolean wasCreatedAsNewShell) {
		super(new BorderLayout());
		this.name = name;
		this.wasCreatedAsNewShell = wasCreatedAsNewShell;
		init(command, workingDirectory);
	}
	
	/**
	 * Creates a new terminal running the given command, with the given
	 * name. If 'name' is null, we use the command as the the name.
	 */
	public static JTerminalPane newCommandWithName(String originalCommand, String name, String workingDirectory) {
		if (name == null) {
			name = originalCommand;
		}
		
		// Avoid having to interpret the command (as java.lang.Process brokenly does) by passing it to the shell as-is.
		ArrayList<String> command = getShellCommand();
		command.add("-c");
		command.add(originalCommand);
		
		return new JTerminalPane(name, workingDirectory, command, false);
	}
	
	/**
	 * Creates a new terminal running the user's shell.
	 */
	public static JTerminalPane newShell() {
		return newShellWithName(null, null);
	}
	
	/**
	 * Creates a new terminal running the user's shell with the given name.
	 */
	public static JTerminalPane newShellWithName(String name, String workingDirectory) {
		if (name == null) {
			String user = System.getProperty("user.name");
			name = user + "@localhost";
		}
		return new JTerminalPane(name, workingDirectory, getShellCommand(), true);
	}
	
	private static ArrayList<String> getShellCommand() {
		ArrayList<String> command = new ArrayList<String>();
		String shell = System.getenv("SHELL");
		command.add(shell);
		if (Options.getSharedInstance().isLoginShell()) {
			command.add("-l");
		}
		return command;
	}
	
	public Dimension getPaneSize() {
		return viewport.getSize();
	}
	
	public void optionsDidChange() {
		if (Options.getSharedInstance().shouldUseAltKeyAsMeta()) {
			// If we want to handle key events when alt is down, we need to turn off input methods.
			textPane.enableInputMethods(false);
		}
	}
	
	private void init(List<String> command, String workingDirectory) {
		textPane = new JTextBuffer();
		textPane.addKeyListener(new KeyHandler());
		optionsDidChange();
		
		initOutputSpinner();
		
		EPopupMenu popupMenu = new EPopupMenu(textPane);
		popupMenu.addMenuItemProvider(new TerminatorMenuItemProvider());
		
		viewport = new VisualBellViewport();
		viewport.setBackground(textPane.getBackground());
		viewport.setView(textPane);
		
		scrollPane = new JScrollPane();
		scrollPane.setBorder(null);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setViewport(viewport);
		scrollPane.setViewportBorder(new javax.swing.border.LineBorder(Options.getSharedInstance().getColor("background"), Options.getSharedInstance().getInternalBorder()));
		
		BirdView birdView = new BirdView(textPane.getBirdsEye(), scrollPane.getVerticalScrollBar());
		textPane.setBirdView(birdView);
		
		add(scrollPane, BorderLayout.CENTER);
		add(birdView, BorderLayout.EAST);
		GuiUtilities.keepMaximumShowing(scrollPane.getVerticalScrollBar());
		
		textPane.sizeChanged();
		try {
			control = new TerminalControl(this, textPane.getModel());
			textPane.setTerminalControl(control);
			control.initProcess(command.toArray(new String[command.size()]), workingDirectory);
			initSizeMonitoring();
		} catch (final Throwable th) {
			Log.warn("Couldn't initialize terminal", th);
			// We can't call announceConnectionLost off the EDT.
			new Thread(new Runnable() {
				public void run() {
					String exceptionDetails = StringUtilities.stackTraceFromThrowable(th).replaceAll("\n", "\n\r");
					control.announceConnectionLost(exceptionDetails + "[Couldn't initialize terminal: " + th.getClass().getSimpleName() + ".]");
				}
			}).start();
		}
	}
	
	private void initOutputSpinner() {
		outputSpinner = new JAsynchronousProgressIndicator();
		outputSpinner.setDisplayedWhenStopped(true);
	}
	
	public JAsynchronousProgressIndicator getOutputSpinner() {
		return outputSpinner;
	}
	
	private void initSizeMonitoring() {
		class SizeMonitor extends ComponentAdapter {
			@Override
			public void componentShown(ComponentEvent event) {
				// Force a size check whenever we're shown in case we're a tab whose window resized while we weren't showing, because in that case we wouldn't have received a componentResized notification.
				componentResized(event);
			}
			
			@Override
			public void componentResized(ComponentEvent event) {
				Dimension size = textPane.getVisibleSizeInCharacters();
				if (size.equals(currentSizeInChars) == false) {
					try {
						control.sizeChanged(size, textPane.getVisibleSize());
						getTerminatorFrame().setTerminalSize(size);
					} catch (Exception ex) {
						if (control != null) {
							Log.warn("Failed to notify " + control.getPtyProcess() + " of size change", ex);
						}
					}
					currentSizeInChars = size;
				}
			}
		};
		// It's a mistake to listen to the JScrollPane's viewport, as
		// we used to, because that changes size when the horizontal
		// scrollbar appears. This caused us to lose data by moving
		// the cursor back over already-output text if that chunk made
		// the scrollbar appear.
		addComponentListener(new SizeMonitor());
	}
	
	public JTextBuffer getTextPane() {
		return textPane;
	}
	
	/** Starts the process listening once all the user interface stuff is set up. */
	public void start() {
		control.start();
	}
	
	public void reset() {
		control.reset();
	}
	
	public TerminalControl getControl() {
		return control;
	}
	
	public LogWriter getLogWriter() {
		return control.getLogWriter();
	}
	
	public String getName() {
		return name;
	}
	
	public boolean shouldHoldOnExit(int status) {
		// bash (and probably other shells) return as their own exit status that of the last command executed.
		// The user will already have seen any failure in a shell window, so we ignore them.
		return (wasCreatedAsNewShell == false) && (status != 0);
	}
	
	public void setName(String name) {
		this.name = name;
		getTerminatorFrame().terminalNameChanged(this);
	}
	
	public Dimension getOptimalViewSize() {
		return textPane.getOptimalViewSize();
	}
	
	private class KeyHandler implements KeyListener {
		private javax.swing.Timer waitForCorrespondingOutputTimer;
		private Location cursorPositionAfterOutput;
		private javax.swing.Timer scrollToBottomDelayTimer;
		
		public KeyHandler() {
			// If your remote-echoing device is more than roundTripMilliseconds away and doesn't automatically wrap at the
			// terminal width, the automatic horizontal scrolling as you type won't work.
			// If you raise the time-out, the automatic horizontal scrolling becomes less responsive.
			int roundTripMilliseconds = 200;
			
			// Give the corresponding output time to come out and so move the cursor, to which we'll scroll...
			waitForCorrespondingOutputTimer = new javax.swing.Timer(roundTripMilliseconds, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					cursorPositionAfterOutput = textPane.getCursorPosition();
					scrollToBottomDelayTimer.start();
				}
			});
			waitForCorrespondingOutputTimer.setRepeats(false);
			
			// ... providing that it doesn't move again for a while.
			scrollToBottomDelayTimer = new javax.swing.Timer(100, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// If the cursor is now in a different position, we conclude that the previous output was coincidental to user's
					// key press.
					// (What we'd really like to check is whether there's been any more output.
					// Checking the cursor position is just an approximation.)
					// This has the slightly unfortunate effect that, if the user is holding a key down,
					// we won't scroll until they let go.
					// The benefit is that we won't leave the window scrolled by half a width if the output goes
					// briefly off-screen just after a user's key press.
					if (cursorPositionAfterOutput != textPane.getCursorPosition()) {
						return;
					}
					textPane.scrollToBottom();
				}
			});
			scrollToBottomDelayTimer.setRepeats(false);
			
			// This automatic scrolling has caused minor trouble a lot of times.
			// Here's some test code which you wouldn't want to cause scrolling but which used to, all the time,
			// and now doesn't.
			// ruby -e 'while true; $stdout.write("X" * 90); $stdout.flush(); sleep(0.05); puts(); end'
		}
		
		/**
		 * On Mac OS, we have the screen menu bar to take care of
		 * all the keyboard equivalents. Elsewhere, we have to detect
		 * the events, and invoke actionPerformed on the relevant
		 * Action ourselves.
		 */
		private void handleKeyboardEquivalent(KeyEvent event) {
			for (Action action : menuAndKeyActions) {
				if (action == null) {
					continue;
				}
				KeyStroke accelerator = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
				KeyStroke thisStroke = KeyStroke.getKeyStrokeForEvent(event);
				if (thisStroke.equals(accelerator)) {
					action.actionPerformed(null);
					break;
				}
			}
		}
		
		public void keyPressed(KeyEvent event) {
			if (doKeyboardScroll(event) || doKeyboardTabSwitch(event)) {
				event.consume();
				return;
			}
			
			// Support keyboard equivalents when the user's been stupid enough to turn the menu off.
			if (TerminatorMenuBar.isKeyboardEquivalent(event)) {
				JFrame frame = getTerminatorFrame();
				if (frame != null && frame.getJMenuBar() == null) {
					handleKeyboardEquivalent(event);
				}
				return;
			}
			
			String sequence = getEscapeSequenceForKeyCode(event);
			if (sequence != null) {
				if (sequence.length() == 1) {
					char ch = sequence.charAt(0);
					// We don't get a KEY_TYPED event for the escape key or keypad enter on Mac OS, where we have to handle it in keyPressed.
					if (ch != Ascii.ESC && ch != Ascii.CR) {
						Log.warn("The constraint about not handling keys that generate KEY_TYPED events in keyPressed was probably violated when handling " + event);
					}
				}
				control.sendUtf8String(sequence);
				textPane.userIsTyping();
				scroll();
				event.consume();
			}
		}

		private String getEscapeSequenceForKeyCode(KeyEvent event) {
			int keyCode = event.getKeyCode();
			// If this event will be followed by a KEY_TYPED event (that is, has a corresponding Unicode character), you must NOT handle it here.
			switch (keyCode) {
				case KeyEvent.VK_ESCAPE:
					// Annoyingly, while Linux sends a KEY_TYPED event for the escape key, Mac OS doesn't.
					return GuiUtilities.isMacOs() ? String.valueOf(Ascii.ESC) : null;
				case KeyEvent.VK_ENTER:
					// Annoyingly, while Linux sends a KEY_TYPED event for the keypad enter, Mac OS doesn't.
					return (GuiUtilities.isMacOs() && event.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD) ? String.valueOf(Ascii.CR) : null;
				
				case KeyEvent.VK_HOME: return Ascii.ESC + "[1~";
				case KeyEvent.VK_END: return Ascii.ESC + "[4~";
				
				case KeyEvent.VK_INSERT: return Ascii.ESC + "[2~";
				
				case KeyEvent.VK_PAGE_UP: return Ascii.ESC + "[5~";
				case KeyEvent.VK_PAGE_DOWN: return Ascii.ESC + "[6~";
				
				case KeyEvent.VK_UP:
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_LEFT:
				{
					char letter = "DACB".charAt(keyCode - KeyEvent.VK_LEFT);
					if (event.isControlDown()) {
						return Ascii.ESC + "[5" + letter;
					} else {
						return Ascii.ESC + "[" + letter;
					}
				}
				
				case KeyEvent.VK_F1:
				case KeyEvent.VK_F2:
				case KeyEvent.VK_F3:
				case KeyEvent.VK_F4:
				case KeyEvent.VK_F5:
					return functionKeySequence(11, keyCode, KeyEvent.VK_F1);
				case KeyEvent.VK_F6:
				case KeyEvent.VK_F7:
				case KeyEvent.VK_F8:
				case KeyEvent.VK_F9:
				case KeyEvent.VK_F10:
					// "ESC[16~" isn't used.
					return functionKeySequence(17, keyCode, KeyEvent.VK_F6);
				case KeyEvent.VK_F11:
				case KeyEvent.VK_F12:
					// "ESC[22~" isn't used.
					return functionKeySequence(23, keyCode, KeyEvent.VK_F11);
					// The function key codes from here on are VT220 codes.
				case KeyEvent.VK_F13:
				case KeyEvent.VK_F14:
					// Java has a discontinuity between VK_F12 and VK_F13.
					return functionKeySequence(25, keyCode, KeyEvent.VK_F13);
				case KeyEvent.VK_F15:
				case KeyEvent.VK_F16:
					// "ESC[27~" isn't used.
					return functionKeySequence(28, keyCode, KeyEvent.VK_F15);
					// X11 key codes go up to F35.
					// Java key codes goes up to F24.
					// Escape sequences mentioned in XTerm's "ctlseqs.ms" go up to F20 (VT220).
					// Current Apple keyboards go up to F16, so that's where we stop.
					
				default:
					return null;
			}
		}
		
		private String functionKeySequence(int base, int keyCode, int keyCodeBase) {
			// FIXME: we should also send the modifier keys after a ';'.
			int argument = base + (keyCode - keyCodeBase);
			return Ascii.ESC + "[" + argument + "~";
		}
		
		public void keyReleased(KeyEvent event) {
		}
		
		// Handle key presses which generate keyTyped events.
		private String getUtf8ForKeyEvent(KeyEvent e) {
			char ch = e.getKeyChar();
			// Interpret the alt key as meta if that's what the user asked for. 
			if (Options.getSharedInstance().shouldUseAltKeyAsMeta() && e.isAltDown()) {
				return Ascii.ESC + String.valueOf(e.getKeyChar());
			}
			if (e.isControlDown() && ch == '\t') {
				// doKeyboardTabSwitch already handled this.
				return null;
			}
			// This modifier test lets Ctrl-H and Ctrl-J generate ^H and ^J instead of
			// mangling them into ^? and ^M.
			// That's useful on those rare but annoying occasions where Backspace and
			// Enter aren't working and it's how other terminals behave.
			if (e.isControlDown() && ch < ' ') {
				return String.valueOf(ch);
			}
			// Work around Sun bug 6320676, and provide support for various terminal eccentricities.
			if (e.isControlDown()) {
				// Control characters are usually typed unshifted, for convenience...
				if (ch >= 'a' && ch <= 'z') {
					return String.valueOf((char) (ch - '`'));
				}
				// ...but the complete range is really from ^@ (ASCII NUL) to ^_ (ASCII US).
				if (ch >= '@' && ch <= '_') {
					return String.valueOf((char) (ch - '@'));
				}
				// There are two special cases that correspond to ASCII NUL.
				// Control-' ' is important for emacs(1).
				if (ch == ' ' || ch == '`') {
					return "\u0000";
				}
				// And one last special case: control-/ is ^_ (ASCII US).
				if (ch == '/') {
					return String.valueOf(Ascii.US);
				}
			}
			if (ch == Ascii.LF) {
				return String.valueOf(Ascii.CR);
			} else if (ch == Ascii.CR) {
				return control.isAutomaticNewline() ? "\r\n" : "\r";
			} else if (ch == Ascii.BS) {
				return ERASE_STRING;
			} else if (ch == Ascii.DEL) {
				return Ascii.ESC + "[3~";
			} else {
				return String.valueOf(ch);
			}
		}
		
		/**
		 * Handling keyTyped instead of doing everything via keyPressed and keyReleased lets us rely on Sun's translation of key presses to characters.
		 * This includes alt-keypad character composition on Windows.
		 */
		public void keyTyped(KeyEvent event) {
			if (TerminatorMenuBar.isKeyboardEquivalent(event)) {
				event.consume();
				return;
			}
			
			String utf8 = getUtf8ForKeyEvent(event);
			if (utf8 != null) {
				control.sendUtf8String(utf8);
				textPane.userIsTyping();
				scroll();
				event.consume();
			}
		}
		
		private boolean doKeyboardScroll(KeyEvent event) {
			if (event.isShiftDown()) {
				switch (event.getKeyCode()) {
					case KeyEvent.VK_HOME:
						textPane.scrollToTop();
						return true;
					case KeyEvent.VK_END:
						textPane.scrollToBottom();
						return true;
					case KeyEvent.VK_PAGE_UP:
						pageUp();
						return true;
					case KeyEvent.VK_PAGE_DOWN:
						pageDown();
						return true;
					default:
						return false;
				}
			} else {
				return false;
			}
		}
		
		private boolean doKeyboardTabSwitch(KeyEvent event) {
			if (event.getKeyCode() == KeyEvent.VK_TAB && event.isControlDown()) {
				// Emulates Firefox's control-tab/control-shift-tab cycle-tab behavior.
				getTerminatorFrame().cycleTab(event.isShiftDown() ? -1 : 1);
				return true;
			} else if (TerminatorMenuBar.isKeyboardEquivalent(event)) {
				// Emulates gnome-terminal's alt-<number> jump-to-tab behavior, or an analog of Terminal.app's command-<number> jump-to-window behavior.
				char ch = event.getKeyChar();
				if (ch >= '1' && ch <= '9') {
					getTerminatorFrame().setSelectedTabIndex(ch - '1');
					return true;
				}
			}
			return false;
		}
		
		/**
		 * Scrolls the display to the bottom if we're configured to do so.
		 * This should be invoked after any action is performed as a
		 * result of a key press/release/type.
		 */
		public void scroll() {
			if (Options.getSharedInstance().isScrollKey()) {
				waitForCorrespondingOutputTimer.stop();
				scrollToBottomDelayTimer.stop();
				waitForCorrespondingOutputTimer.start();
			}
		}
	}
	
	public SelectionHighlighter getSelectionHighlighter() {
		return (SelectionHighlighter) textPane.getHighlighterOfClass(SelectionHighlighter.class);
	}
	
	public void selectAll() {
		getSelectionHighlighter().selectAll();
	}
	
	public void pageUp() {
		scrollVertically(-0.5);
	}
	
	public void pageDown() {
		scrollVertically(0.5);
	}
	
	public void lineUp() {
		scrollVertically(-1.0/currentSizeInChars.height);
	}
	
	public void lineDown() {
		scrollVertically(1.0/currentSizeInChars.height);
	}
	
	private void scrollVertically(double yMul) {
		// Translate JViewport's terrible confusing names into plain English.
		final int totalHeight = viewport.getViewSize().height;
		final int visibleHeight = viewport.getExtentSize().height;
		
		Point p = viewport.getViewPosition();
		p.y += (int) (yMul * visibleHeight);
		
		// Don't go off the top...
		p.y = Math.max(0, p.y);
		// Or bottom...
		p.y = Math.min(p.y, totalHeight - visibleHeight);
		
		viewport.setViewPosition(p);
	}
	
	/**
	 * Hands focus to our text pane.
	 */
	public void requestFocus() {
		textPane.requestFocus();
	}
	
	private class TerminatorMenuItemProvider implements MenuItemProvider {
		public void provideMenuItems(MouseEvent e, Collection<Action> actions) {
			actions.addAll(Arrays.asList(menuAndKeyActions));
			addInfoItems(actions);
		}
		
		private void addInfoItems(Collection<Action> actions) {
			String selectedText = getSelectedText();
			addSelectionInfoItems(actions, selectedText);
			addNumberInfoItems(actions, selectedText);
		}
		
		private void addSelectionInfoItems(Collection<Action> actions, String selectedText) {
			if (selectedText.length() == 0) {
				return;
			}
			
			int selectedLineCount = 0;
			for (int i = 0; i < selectedText.length(); ++i) {
				if (selectedText.charAt(i) == '\n') {
					++selectedLineCount;
				}
			}
			actions.add(null);
			actions.add(makeInfoItem("Selection"));
			actions.add(makeInfoItem("  characters: " + selectedText.length()));
			if (selectedLineCount != 0) {
				actions.add(makeInfoItem("  lines: " + selectedLineCount));
			}
		}
		
		private void addNumberInfoItems(Collection<Action> actions, String selectedText) {
			if (selectedText.contains("\n")) {
				return;
			}
			
			NumberDecoder numberDecoder = new NumberDecoder(selectedText);
			if (numberDecoder.isValid()) {
				actions.add(null);
				List<String> items = numberDecoder.toStrings();
				for (String item : items) {
					actions.add(makeInfoItem(item));
				}
			}
		}
		
		private Action makeInfoItem(String text) {
			return new InfoAction(text);
		}
	}
	
	public static class InfoAction extends AbstractAction {
		public InfoAction(String text) {
			super(text);
			setEnabled(false);
		}
		
		public void actionPerformed(ActionEvent e) {
			// Do nothing.
		}
	}
	
	private String getSelectedText() {
		String result = "";
		try {
			Clipboard selection = getToolkit().getSystemSelection();
			if (selection == null) {
				selection = getToolkit().getSystemClipboard();
			}
			Transferable transferable = selection.getContents(null);
			result = (String) transferable.getTransferData(DataFlavor.stringFlavor);
		} catch (Exception ex) {
			Log.warn("Couldn't get system selection", ex);
		}
		return result;
	}
	
	public void doCopyAction() {
		getSelectionHighlighter().copyToSystemClipboard();
	}
	public void doPasteAction() {
		textPane.pasteSystemClipboard();
	}
	
	public void destroyProcess() {
		control.destroyProcess();
	}
	
	private boolean shouldClose() {
		final PtyProcess ptyProcess = control.getPtyProcess();
		if (ptyProcess == null) {
			// This can happen if the JNI side failed to start.
			// There's no reason not to close such a terminal.
			return true;
		}

		final int directChildProcessId = ptyProcess.getProcessId();
		final String processesUsingTty = ptyProcess.listProcessesUsingTty();

		if (processesUsingTty.length() == 0) {
			// There's nothing still running, so just close.
			return true;
		}

		// Check if the only thing still running is the command we started, or that the pty has been closed.
		// In either of those cases, there's no reason to hang around.
		// FIXME: ideally, PtyProcess would give us a List<ProcessInfo>, but that opens a whole can of JNI worms. Hence the following hack.
		final String[] processList = processesUsingTty.split(", ");
		if (processList.length == 1 && (processList[0].matches("^.*\\(" + directChildProcessId + "\\)$") || processList[0].equals("(pty closed)"))) {
			return true;
		}

		boolean reallyClose = SimpleDialog.askQuestion(getTerminatorFrame(), "Close Terminal?", "Closing this terminal may terminate the following processes: " + processesUsingTty, "Close");
		return reallyClose;
	}

	/**
	 * Closes the terminal pane after checking with the user.
	 * Returns false if the user canceled the close, true otherwise.
	 */
	public boolean doCheckedCloseAction() {
		if (shouldClose()) {
			doCloseAction();
			return true;
		}
		return false;
	}
	
	public void doCloseAction() {
		destroyProcess();
		getLogWriter().close();
		getTerminatorFrame().closeTerminalPane(this);
	}
	
	public TerminatorFrame getTerminatorFrame() {
		return (TerminatorFrame) SwingUtilities.getAncestorOfClass(TerminatorFrame.class, this);
	}
	
	/**
	 * Implements visual bell.
	 */
	public void flash() {
		viewport.flash();
	}
}
