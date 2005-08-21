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
	
	private Process process;
	private TerminalControl control;
	private JTextBuffer textPane;
	private JScrollPane scrollPane;
	private String name;
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
		new TerminatorMenuBar.NextTerminalAction(),
		new TerminatorMenuBar.PreviousTerminalAction(),
		null,
		new TerminatorMenuBar.ShowInfoAction(),
		new TerminatorMenuBar.ResetAction()
	};
	
	/**
	 * Creates a new terminal with the given name, running the given command.
	 */
	public JTerminalPane(String name, String command) {
		super(new BorderLayout());
		this.name = name;
		init(command);
	}
	
	/**
	 * Creates a new terminal running the given command, with the given
	 * title. If 'title' is null, we use the first word of the command
	 * as the the title.
	 */
	public static JTerminalPane newCommandWithTitle(String command, String title) {
		if (title == null) {
			title = command.trim();
			if (title.contains(" ")) {
				title = title.substring(0, title.indexOf(' '));
			}
		}
		return new JTerminalPane(title, command);
	}
	
	/**
	 * Creates a new terminal running bash(1), which is assumed to be the
	 * user's shell.
	 */
	public static JTerminalPane newShell() {
		String user = System.getProperty("user.name");
		String command = "bash";
		if (Options.getSharedInstance().isLoginShell()) {
			command += " -l";
		}
		return new JTerminalPane(user + "@localhost", command);
	}
	
	public Dimension getPaneSize() {
		return scrollPane.getViewport().getSize();
	}
	
	private void init(String command) {
		textPane = new JTextBuffer();
		textPane.addKeyListener(new KeyHandler());
		
		initOutputSpinner();
		
		EPopupMenu popupMenu = new EPopupMenu(textPane);
		popupMenu.addMenuItemProvider(new TerminatorMenuItemProvider());
		
		scrollPane = new JScrollPane(new BorderPanel(textPane));
		scrollPane.setBorder(null);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.getViewport().setBackground(textPane.getBackground());
		
		fixScrollBarForMacOs(scrollPane);
		
		add(scrollPane, BorderLayout.CENTER);
		GuiUtilities.keepMaximumShowing(scrollPane.getVerticalScrollBar());
		
		textPane.sizeChanged();
		try {
			control = new TerminalControl(this, textPane.getModel(), command);
			textPane.setTerminalControl(control);
			initSizeMonitoring();
		} catch (Exception ex) {
			Log.warn("Couldn't initialize terminal", ex);
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
	
	/**
	 * Mac OS' grow box intrudes in the lower right corner of every window.
	 * In our case, with a scroll bar hard against the right edge of the
	 * window, that means our down scroll arrow gets covered.
	 */
	private void fixScrollBarForMacOs(JScrollPane scrollPane) {
		if (GuiUtilities.isMacOs() == false) {
			return;
		}
		
		// Make a JPanel the same size as the grow box.
		final int size = (int) scrollPane.getVerticalScrollBar().getMinimumSize().getWidth();
		JPanel growBoxPanel = new JPanel();
		Dimension growBoxSize = new Dimension(size, size);
		growBoxPanel.setPreferredSize(growBoxSize);
		
		// Stick the scroll pane's scroll bar in a new panel with
		// our fake grow box at the bottom, so the real grow box
		// can sit on top of it.
		JPanel sidePanel = new JPanel(new BorderLayout());
		sidePanel.add(scrollPane.getVerticalScrollBar(), BorderLayout.CENTER);
		sidePanel.add(growBoxPanel, BorderLayout.SOUTH);
		
		// Put our scroll bar plus spacer panel against the edge of
		// the window.
		add(sidePanel, BorderLayout.EAST);
	}
	
	/** Starts the process listening once all the user interface stuff is set up. */
	public void start() {
		control.start();
	}
	
	public void reset() {
		control.reset();
	}
	
	public LogWriter getLogWriter() {
		return control.getLogWriter();
	}
	
	public String getName() {
		return name;
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
			switch (keyCode) {
				case KeyEvent.VK_ESCAPE: return Ascii.ESC + "";
				
				case KeyEvent.VK_HOME: return Ascii.ESC + "[H";
				case KeyEvent.VK_END: return Ascii.ESC + "[F";
				
				case KeyEvent.VK_PAGE_UP: return Ascii.ESC + "[5~";
				case KeyEvent.VK_PAGE_DOWN: return Ascii.ESC + "[6~";
				
				case KeyEvent.VK_UP:
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_LEFT:
				{
					/* Send xterm sequences. */
					char letter = "DACB".charAt(keyCode - KeyEvent.VK_LEFT);
					if (event.isControlDown()) {
						return Ascii.ESC + "[5" + letter;
					} else {
						return Ascii.ESC + "[" + letter;
					}
				}

				default: return null;
			}
		}

		public void keyReleased(KeyEvent event) {
		}
		
		private String getUtf8ForKeyEvent(KeyEvent e) {
			char ch = e.getKeyChar();
			if (ch == Ascii.LF) {
				return String.valueOf(Ascii.CR);
			} else if (ch == Ascii.CR) {
				return control.isAutomaticNewline() ? "\r\n" : "\r";
			} else if (ch == Ascii.BS) {
				return ERASE_STRING;
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
		JViewport viewport = scrollPane.getViewport();
		
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
	
	public void doCloseAction() {
		destroyProcess();
		getTerminatorFrame().closeTerminalPane(this);
	}
	
	private TerminatorFrame getTerminatorFrame() {
		return (TerminatorFrame) SwingUtilities.getAncestorOfClass(TerminatorFrame.class, this);
	}
}
