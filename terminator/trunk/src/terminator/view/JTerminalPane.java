package terminator.view;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import javax.swing.*;
import e.forms.*;
import e.gui.*;
import e.util.*;
import terminator.*;
import terminator.model.*;
import terminator.terminal.*;
import terminator.view.highlight.*;

/**

@author Phil Norman
@author Elliott Hughes
*/

public class JTerminalPane extends JPanel {
	private TerminalPaneMaster controller;
	private TerminalControl control;
	private JTextBuffer textPane;
	private JScrollPane scrollPane;
	private String name;
	private Dimension currentSizeInChars;
	private Action[] menuAndKeyActions = new Action[] {
		new CopyAction(),
		new PasteAction(),
		null,
		new NewWindowAction(),
//		new NewTabAction(),
//		new RunCommandAction(),
		new CloseAction(),
		null,
		new FindAction(),
		new FindNextAction(),
		new FindPreviousAction(),
		null,
		new ClearScrollbackAction(),
	};
	
	/**
	 * Creates a new terminal with the given name, running the given command.
	 */
	public JTerminalPane(TerminalPaneMaster controller, String name, String command, boolean ignoreExitStatus) {
		super(new BorderLayout());
		this.controller = controller;
		this.name = name;
		
		try {
//			Log.warn("Starting process '" + command + "'");
			final Process proc = Runtime.getRuntime().exec(System.getProperty("pty.binary") + " " + command);
			init(command, proc, ignoreExitStatus);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Creates a new terminal running the given command, with the given
	 * title. If 'title' is null, we use the first word of the command
	 * as the the title.
	 */
	public static JTerminalPane newCommandWithTitle(TerminalPaneMaster controller, String command, String title) {
		if (title == null) {
			title = command.trim();
			if (title.indexOf(' ') != -1) {
				title = title.substring(0, title.indexOf(' '));
			}
		}
		return new JTerminalPane(controller, title, command, false);
	}
	
	/**
	 * Creates a new terminal running the user's shell.
	 */
	public static JTerminalPane newShell(TerminalPaneMaster controller) {
		String user = System.getProperty("user.name");
		String command = getUserShell(user);
		if (Options.getSharedInstance().isLoginShell()) {
			command += " -l";
		}
		return new JTerminalPane(controller, user + "@localhost", command, true);
	}
	
	public Dimension getPaneSize() {
		return scrollPane.getViewport().getSize();
	}
	
	public Dimension getCharUnitSize() {
		return textPane.getCharUnitSize();
	}
	
	/**
	* Returns the command to execute as the user's shell, parsed from the /etc/passwd file.
	* On any kind of failure, 'bash' is returned as default.
	*/
	private static String getUserShell(String user) {
		File passwdFile = new File("/etc/passwd");
		if (passwdFile.exists()) {
			BufferedReader in = null;
			try {
				in = new BufferedReader(new FileReader(passwdFile));
				String line;
				while ((line = in.readLine()) != null) {
					if (line.startsWith(user + ":")) {
						return line.substring(line.lastIndexOf(':') + 1);
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException ex) {
						Log.warn("Couldn't close file.", ex);
					}
				}
			}
		}
		return "bash";
	}

	private void init(String command, Process process, boolean ignoreExitStatus) throws IOException {
		textPane = new JTextBuffer(controller);
		textPane.addKeyListener(new KeyHandler());
		textPane.addMouseListener(new ContextMenuOpener());
		
		scrollPane = new JScrollPane(new BorderPanel(textPane));
		scrollPane.setBorder(null);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.getViewport().setBackground(textPane.getBackground());
		
		fixScrollBarForMacOs(scrollPane);
		
		add(scrollPane, BorderLayout.CENTER);
		new StickyBottomScrollBarListener(scrollPane.getVerticalScrollBar());
		
		initSizeMonitoring(scrollPane);
		textPane.sizeChanged();
		try {
			control = new TerminalControl(this, textPane.getModel(), command, process, ignoreExitStatus);
			textPane.setTerminalControl(control);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public TerminalPaneMaster getTerminalPaneMaster() {
		return controller;
	}
	
	private void initSizeMonitoring(final JScrollPane scrollPane) {
		class SizeMonitor extends ComponentAdapter {
			public void componentShown(ComponentEvent event) {
				currentSizeInChars = textPane.getVisibleSizeInCharacters();
			}
			
			public void componentResized(ComponentEvent event) {
				Dimension size = textPane.getVisibleSizeInCharacters();
				if (size.equals(currentSizeInChars) == false) {
					try {
						control.sizeChanged(size, textPane.getVisibleSize());
						controller.setTerminalSize(size);
					} catch (IOException ex) {
						Log.warn("Failed to notify pty of size change.", ex);
					}
					currentSizeInChars = size;
				}
			}
		};
		scrollPane.getViewport().addComponentListener(new SizeMonitor());
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
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
		controller.terminalNameChanged(this);
		HyperlinkHighlighter linker = (HyperlinkHighlighter) textPane.getHighlighterOfClass(HyperlinkHighlighter.class);
		linker.setDirectory(name);
	}
	
	public Dimension getOptimalViewSize() {
		return textPane.getOptimalViewSize();
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
	 * equivalent. In the long run, this code should all disappear
	 * and be replaced by a Swing menu, but in the meantime, this
	 * abstracts away cross-platform differences.
	 */
	public static boolean isKeyboardEquivalent(KeyEvent event) {
		return ((event.getModifiers() & KEYBOARD_EQUIVALENT_MODIFIER) == KEYBOARD_EQUIVALENT_MODIFIER);
	}
	
	private class KeyHandler implements KeyListener {
		private void handleKeyboardEquivalent(KeyEvent event) {
			for (int i = 0; i < menuAndKeyActions.length; i++) {
				if (menuAndKeyActions[i] == null) {
					continue;
				}
				KeyStroke accelerator = (KeyStroke) menuAndKeyActions[i].getValue(Action.ACCELERATOR_KEY);
				KeyStroke thisStroke = KeyStroke.getKeyStrokeForEvent(event);
				if (thisStroke.equals(accelerator)) {
					menuAndKeyActions[i].actionPerformed(null);
					break;
				}
			}
		}
		
		public void keyPressed(KeyEvent event) {
			if (isKeyboardEquivalent(event)) {
				handleKeyboardEquivalent(event);
				return;
			}
			if (doKeyboardScroll(event)) {
				event.consume();
				return;
			}
			String sequence = getSequenceForKeyCode(event);
			if (sequence != null) {
				control.sendEscapeString(sequence);
				textPane.userIsTyping();
				scroll();
				event.consume();
			}
		}

		private String getSequenceForKeyCode(KeyEvent event) {
			int keyCode = event.getKeyCode();
			switch (keyCode) {
				case KeyEvent.VK_ESCAPE: return "";
				
				case KeyEvent.VK_HOME: return "[H";
				case KeyEvent.VK_END: return "[F";
				
				case KeyEvent.VK_UP:
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_LEFT:
				{
					/* Send xterm sequences. */
					char letter = "DACB".charAt(keyCode - KeyEvent.VK_LEFT);
					if (event.isControlDown()) {
						return "[5" + letter;
					} else {
						return "[" + letter;
					}
				}

				default: return null;
			}
		}

		public void keyReleased(KeyEvent event) {
		}

		public void keyTyped(KeyEvent event) {
			if (isKeyboardEquivalent(event) == false) {
				char ch = event.getKeyChar();
				if (ch != KeyEvent.CHAR_UNDEFINED) {
					control.sendChar(ch);
					textPane.userIsTyping();
					scroll();
				}
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
					case KeyEvent.VK_UP:
						scrollVertically(-0.5);
						return true;
					case KeyEvent.VK_PAGE_DOWN:
					case KeyEvent.VK_DOWN:
						scrollVertically(0.5);
						return true;
					default:
						return false;
				}
			} else {
				return false;
			}
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
	
	/**
	 * Hands focus to our text pane.
	 */
	public void requestFocus() {
		textPane.requestFocus();
	}
	
	public class ContextMenuOpener extends MouseAdapter {
		public void mousePressed(MouseEvent event) {
			maybeShowPopup(event);
		}

		public void mouseReleased(MouseEvent event) {
			maybeShowPopup(event);
		}

		private void maybeShowPopup(MouseEvent event) {
			if (event.isPopupTrigger()) {
				EPopupMenu menu = new EPopupMenu();
				for (int i = 0; i < menuAndKeyActions.length; i++) {
					if (menuAndKeyActions[i] == null) {
						menu.addSeparator();
					} else {
						menu.add(menuAndKeyActions[i]);
					}
				}
				addInfoItems(menu);
				menu.show((Component) event.getSource(), event.getX() + 1, event.getY());
			}
		}
	}
	
	public class InfoAction extends AbstractAction {
		public InfoAction(String text) {
			super(text);
			setEnabled(false);
		}
		
		public void actionPerformed(ActionEvent e) {
			// Do nothing.
		}
	}
	
	private void addSelectionInfoItems(EPopupMenu menu, String selectedText) {
		if (selectedText.length() == 0) {
			return;
		}
		
		int selectedLineCount = 0;
		for (int i = 0; i < selectedText.length(); ++i) {
			if (selectedText.charAt(i) == '\n') {
				++selectedLineCount;
			}
		}
		menu.addSeparator();
		menu.add(makeInfoItem("Selection"));
		menu.add(makeInfoItem("  characters: " + selectedText.length()));
		if (selectedLineCount != 0) {
			menu.add(makeInfoItem("  lines: " + selectedLineCount));
		}
	}
	
	private void addNumberInfoItems(EPopupMenu menu, String selectedText) {
		if (selectedText.indexOf("\n") != -1) {
			return;
		}
		
		NumberDecoder numberDecoder = new NumberDecoder(selectedText);
		if (numberDecoder.isValid()) {
			menu.addSeparator();
			List items = numberDecoder.toStrings();
			for (int i = 0; i < items.size(); ++i) {
				String item = (String) items.get(i);
				menu.add(makeInfoItem(item));
			}
		}
	}
	
	private void addInfoItems(EPopupMenu menu) {
		String selectedText = getSelectedText();
		addSelectionInfoItems(menu, selectedText);
		addNumberInfoItems(menu, selectedText);
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
			Log.warn("Couldn't get system selection.", ex);
		}
		return result;
	}
	
	private Action makeInfoItem(String text) {
		return new InfoAction(text);
	}
	
	public static KeyStroke makeKeyStroke(String key) {
		return GuiUtilities.makeKeyStrokeForModifier(KEYBOARD_EQUIVALENT_MODIFIER, key, false);
	}
	
	public class CopyAction extends AbstractAction {
		public CopyAction() {
			super("Copy");
			putValue(ACCELERATOR_KEY, makeKeyStroke("C"));
		}
		
		public void actionPerformed(ActionEvent e) {
			// FIXME: we should probably have an "explicit copy" mode.
		}
	}
	
	public class PasteAction extends AbstractAction {
		public PasteAction() {
			super("Paste");
			putValue(ACCELERATOR_KEY, makeKeyStroke("V"));
		}
		
		public void actionPerformed(ActionEvent e) {
			textPane.paste();
		}
	}
	
/*	public class RunCommandAction implements MenuKeyAction {
		public String getName(Point mousePosition) {
			return "Run In Tab...";
		}
		
		public void performAction() {
			JTextField commandField = new JTextField(40);
			FormPanel formPanel = new FormPanel();
			formPanel.addRow("Command:", commandField);
			boolean okay = FormDialog.show(null, "Run Command", formPanel, "Run");
			if (okay == false) {
				return;
			}
			String command = commandField.getText();
			if (command.length() > 0) {
				controller.openCommandPane(command, true);
			}
		}
		
		public char getHotkeyChar() {
			return 'E';
		}
	}*/
	
	public class FindAction extends AbstractAction {
		public FindAction() {
			super("Find...");
			putValue(ACCELERATOR_KEY, makeKeyStroke("F"));
		}
		
		public void actionPerformed(ActionEvent e) {
			controller.showFindDialogFor(textPane);
		}
	}
	
	public class FindNextAction extends AbstractAction {
		public FindNextAction() {
			super("Find Next");
			putValue(ACCELERATOR_KEY, makeKeyStroke("G"));
		}
		
		public void actionPerformed(ActionEvent e) {
			textPane.findNext();
		}
	}
	
	public class FindPreviousAction extends AbstractAction {
		public FindPreviousAction() {
			super("Find Previous");
			putValue(ACCELERATOR_KEY, makeKeyStroke("D"));
		}
		
		public void actionPerformed(ActionEvent e) {
			textPane.findPrevious();
		}
	}
	
	public class ClearScrollbackAction extends AbstractAction {
		public ClearScrollbackAction() {
			super("Clear Scrollback");
			putValue(ACCELERATOR_KEY, makeKeyStroke("K"));
		}
		
		public void actionPerformed(ActionEvent e) {
			textPane.clearScrollBuffer();
			control.sendRedrawScreen();
		}
	}
	
	public class NewWindowAction extends AbstractAction {
		public NewWindowAction() {
			super("New Window");
			putValue(ACCELERATOR_KEY, makeKeyStroke("N"));
		}
		
		public void actionPerformed(ActionEvent e) {
			controller.openNewWindow();
		}
	}
	
/*	public class NewTabAction implements MenuKeyAction {
		public String getName(Point mousePosition) {
			return "New Tab";
		}
		
		public void performAction() {
			controller.openShellPane(true);
		}
		
		public char getHotkeyChar() {
			return 'T';
		}
	}*/
	
	public class CloseAction extends AbstractAction {
		public CloseAction() {
			super("Close");
			putValue(ACCELERATOR_KEY, makeKeyStroke("W"));
		}
		
		public void actionPerformed(ActionEvent e) {
			control.destroyProcess();
			controller.closeTerminalPane(JTerminalPane.this);
		}
	}
}
