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
		new TerminatorMenuBar.FindNextLinkAction(),
		new TerminatorMenuBar.FindPreviousLinkAction(),
		null,
		new TerminatorMenuBar.ClearScrollbackAction(),
		null,
		new TerminatorMenuBar.NextTabAction(),
		new TerminatorMenuBar.PreviousTabAction(),
		null,
		new TerminatorMenuBar.ShowInfoAction(),
		new TerminatorMenuBar.ResetAction()
	};
	
	/**
	 * Creates a new terminal with the given name, running the given command.
	 */
	private JTerminalPane(String name, List<String> command, boolean wasCreatedAsNewShell) {
		super(new BorderLayout());
		this.name = name;
		this.wasCreatedAsNewShell = wasCreatedAsNewShell;
		init(command);
	}
	
	/**
	 * Creates a new terminal running the given command, with the given
	 * name. If 'name' is null, we use the command as the the name.
	 */
	public static JTerminalPane newCommandWithName(String originalCommand, String name) {
		if (name == null) {
			name = originalCommand;
		}
		
		// Avoid having to interpret the command (as java.lang.Process brokenly does) by passing it to the shell as-is.
		ArrayList<String> command = getShellCommand();
		command.add("-c");
		command.add(originalCommand);
		
		return new JTerminalPane(name, command, false);
	}
	
	/**
	 * Creates a new terminal running the user's shell.
	 */
	public static JTerminalPane newShell() {
		return newShellWithName(null);
	}
	
	/**
	 * Creates a new terminal running the user's shell with the given name.
	 */
	public static JTerminalPane newShellWithName(String name) {
		if (name == null) {
			String user = System.getProperty("user.name");
			name = user + "@localhost";
		}
		return new JTerminalPane(name, getShellCommand(), true);
	}
	
	private static ArrayList<String> getShellCommand() {
		ArrayList<String> command = new ArrayList<String>();
		String shell = System.getenv("SHELL");
		if (shell == null) {
			// Has there ever been a Unix without a /bin/sh?
			shell = "/bin/sh";
		}
		command.add(shell);
		if (Options.getSharedInstance().isLoginShell()) {
			command.add("-l");
		}
		return command;
	}
	
	public Dimension getPaneSize() {
		return viewport.getSize();
	}
	
	private void init(List<String> command) {
		textPane = new JTextBuffer();
		textPane.addKeyListener(new KeyHandler());
		
		initOutputSpinner();
		
		EPopupMenu popupMenu = new EPopupMenu(textPane);
		popupMenu.addMenuItemProvider(new TerminatorMenuItemProvider());
		
		JComponent view = new BorderPanel(textPane);
		
		viewport = new VisualBellViewport();
		viewport.setBackground(textPane.getBackground());
		viewport.setView(view);
		
		scrollPane = new JScrollPane();
		scrollPane.setBorder(null);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setViewport(viewport);
		
		add(scrollPane, BorderLayout.CENTER);
		GuiUtilities.keepMaximumShowing(scrollPane.getVerticalScrollBar());
		
		textPane.sizeChanged();
		try {
			control = new TerminalControl(this, textPane.getModel());
			textPane.setTerminalControl(control);
			control.initProcess(command.toArray(new String[command.size()]));
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
				currentSizeInChars = textPane.getVisibleSizeInCharacters();
			}
			
			@Override
			public void componentResized(ComponentEvent event) {
				Dimension size = textPane.getVisibleSizeInCharacters();
				if (size.equals(currentSizeInChars) == false) {
					try {
						control.sizeChanged(size, textPane.getVisibleSize());
						getTerminatorFrame().setTerminalSize(size);
					} catch (IOException ex) {
						Log.warn("Failed to notify process of size change", ex);
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
		// The user will already have seen any failure in a shell window.
		return status != 0 && wasCreatedAsNewShell;
	}
	
	public void setName(String name) {
		this.name = name;
		getTerminatorFrame().terminalNameChanged(this);
		HyperlinkHighlighter linker = (HyperlinkHighlighter) textPane.getHighlighterOfClass(HyperlinkHighlighter.class);
		linker.setDirectory(name);
	}
	
	public Dimension getOptimalViewSize() {
		return textPane.getOptimalViewSize();
	}
	
	private class KeyHandler implements KeyListener {
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
			if (TerminatorMenuBar.isKeyboardEquivalent(event)) {
				if (Options.getSharedInstance().shouldUseMenuBar() == false) {
					handleKeyboardEquivalent(event);
				}
				return;
			}
			
			if (doKeyboardScroll(event)) {
				event.consume();
				return;
			}
			
			String sequence = getEscapeSequenceForKeyCode(event);
			if (sequence != null) {
				control.sendUtf8String(sequence);
				textPane.userIsTyping();
				scroll();
				event.consume();
			}
		}

		private String getEscapeSequenceForKeyCode(KeyEvent event) {
			int keyCode = event.getKeyCode();
			// If the key press wll generate a keyTyped event, you must NOT handle it here.
			switch (keyCode) {
				case KeyEvent.VK_ESCAPE: return Ascii.ESC + "";
				
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
			// This modifier test lets Ctrl-H and Ctrl-J generate ^H and ^J instead of
			// mangling them into ^? and ^M.
			// That's useful on those rare but annoying occasions where Backspace and
			// Enter aren't working and it's how other terminals behave.
			if (e.getModifiers() != 0) {
				return String.valueOf(ch);
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
		
		public void keyTyped(KeyEvent event) {
			if (TerminatorMenuBar.isKeyboardEquivalent(event) == false) {
				String utf8 = getUtf8ForKeyEvent(event);
				control.sendUtf8String(utf8);
				textPane.userIsTyping();
				scroll();
			}
			event.consume();
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
		
		/**
		 * Scrolls the display to the bottom if we're configured to do so.
		 * This should be invoked after any action is performed as a
		 * result of a key press/release/type.
		 */
		public void scroll() {
			if (Options.getSharedInstance().isScrollKey()) {
				textPane.scrollToBottom();
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
	
	/**
	 * Closes the terminal pane after checking with the user.
	 * Returns false if the user canceled the close, true otherwise.
	 */
	public boolean doCheckedCloseAction() {
		boolean shouldAskUser = true;
		
		final PtyProcess ptyProcess = control.getPtyProcess();
		final int directChildProcessId = ptyProcess.getProcessId();
		final String processesUsingTty = ptyProcess.listProcessesUsingTty();
		
		// If the only thing still running is the shell we started, don't bother the user.
		// FIXME: ideally, PtyProcess would give us a List<ProcessInfo>, but that opens a whole can of JNI worms. Hence the following hack.
		final String[] processList = processesUsingTty.split(", ");
		if (wasCreatedAsNewShell && processList.length == 1 && processList[0].matches("^.*\\(" + directChildProcessId + "\\)$")) {
			shouldAskUser = false;
		}
		
		// If there's nothing still running, don't bother the user.
		if (processesUsingTty.length() == 0) {
			shouldAskUser = false;
		}
		
		if (shouldAskUser) {
			boolean reallyClose = SimpleDialog.askQuestion(getTerminatorFrame(), "Terminator", "<html><b>Close Terminal?</b><p>Closing this terminal may terminate the following processes: " + processesUsingTty, "Terminate");
			if (reallyClose == false) {
				return false;
			}
		}
		
		doCloseAction();
		return true;
	}
	
	public void doCloseAction() {
		destroyProcess();
		getTerminatorFrame().closeTerminalPane(this);
	}
	
	private TerminatorFrame getTerminatorFrame() {
		return (TerminatorFrame) SwingUtilities.getAncestorOfClass(TerminatorFrame.class, this);
	}
	
	/**
	 * Implements visual bell.
	 */
	public void flash() {
		viewport.flash();
	}
}
