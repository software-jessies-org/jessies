package terminator;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import e.forms.*;
import e.util.*;

/**

@author Phil Norman
@author Elliott Hughes
*/

public class JTerminalPane extends JPanel {
	private Controller controller;
	private TerminalControl control;
	private JTextBuffer textPane;
	private JScrollPane scrollPane;
	private String name;
	private Dimension currentSizeInChars;
	private MenuKeyAction[] menuAndKeyActions = new MenuKeyAction[] {
		new CopyAction(),
		new PasteAction(),
		null,
		new NewTabAction(),
		new RunCommandAction(),
		new CloseTabAction(),
		null,
		new FindAction(),
		new FindNextAction(),
		new FindPreviousAction(),
		null,
		new ClearScrollbackAction(),
		null,
		new ChangeColourAction(),
//		new NewWindowAction(),
	};
	
	/**
	 * Creates a new terminal with the given name, running the given command.
	 */
	private JTerminalPane(Controller controller, String name, String command, boolean ignoreExitStatus) {
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
	public static JTerminalPane newCommandWithTitle(Controller controller, String command, String title) {
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
	public static JTerminalPane newShell(Controller controller) {
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
		
		initSizeMonitoring(scrollPane);
		textPane.sizeChanged();
		try {
			control = new TerminalControl(this, textPane.getModel(), command, process, ignoreExitStatus);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public Controller getController() {
		return controller;
	}
	
	private void initSizeMonitoring(final JScrollPane scrollPane) {
		class SizeMonitor extends ComponentAdapter {
			private Dimension currentSize;
			private boolean isAtEnd = true;

			public void componentShown(ComponentEvent event) {
				currentSizeInChars = textPane.getVisibleSizeInCharacters();
			}
			
			public void componentResized(ComponentEvent event) {
				Dimension size = textPane.getVisibleSizeInCharacters();
				if (size.equals(currentSizeInChars) == false) {
					try {
						control.sizeChanged(size, textPane.getVisibleSize());
						controller.setTerminalSize(size);
						textPane.scrollToBottom();
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
	private int keyboardEquivalentModifier = GuiUtilities.isMacOs() ? KeyEvent.META_MASK : KeyEvent.ALT_MASK;
	
	private class KeyHandler implements KeyListener {
		/**
		 * Tests whether the given event corresponds to a keyboard
		 * equivalent. In the long run, this code should all disappear
		 * and be replaced by a Swing menu, but in the meantime, this
		 * abstracts away cross-platform differences.
		 */
		public boolean isKeyboardEquivalent(KeyEvent event) {
			return ((event.getModifiers() & keyboardEquivalentModifier) == keyboardEquivalentModifier);
		}
		
		public void keyPressed(KeyEvent event) {
			if (isKeyboardEquivalent(event)) {
				return;
			}
			if (doKeyboardScroll(event)) {
				event.consume();
				return;
			}
			String sequence = getSequenceForKeyCode(event);
			if (sequence != null) {
				control.sendEscapeString(sequence);
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
			char ch = event.getKeyChar();
			if (isKeyboardEquivalent(event)) {
				for (int i = 0; i < menuAndKeyActions.length; i++) {
					if (menuAndKeyActions[i] == null) {
						continue;
					}
					char actionChar = Character.toLowerCase(menuAndKeyActions[i].getHotkeyChar());
					if (Character.toLowerCase(ch) == actionChar) {
						menuAndKeyActions[i].performAction();
						break;
					}
				}
			} else {
				if (ch != KeyEvent.CHAR_UNDEFINED) {
					control.sendChar(ch);
					scroll();
				}
			}
			event.consume();
		}
		
		private boolean doKeyboardScroll(KeyEvent event) {
			if (event.isShiftDown()) {
				switch (event.getKeyCode()) {
					case KeyEvent.VK_HOME:
					case KeyEvent.VK_LEFT:
						scrollBy(-0.5, 0);
						return true;
					case KeyEvent.VK_END:
					case KeyEvent.VK_RIGHT:
						scrollBy(0.5, 0);
						return true;
					case KeyEvent.VK_PAGE_UP:
					case KeyEvent.VK_UP:
						scrollBy(0, -0.5);
						return true;
					case KeyEvent.VK_PAGE_DOWN:
					case KeyEvent.VK_DOWN:
						scrollBy(0, 0.5);
						return true;
					default:
						return false;
				}
			} else {
				return false;
			}
		}
		
		private void scrollBy(double xMul, double yMul) {
			Rectangle rect = scrollPane.getViewport().getViewRect();
			rect.x += (int) (xMul * rect.width);
			rect.y += (int) (yMul * rect.height);
			textPane.scrollRectToVisible(rect);
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
				JPopupMenu menu = new JPopupMenu();
				for (int i = 0; i < menuAndKeyActions.length; i++) {
					if (menuAndKeyActions[i] == null) {
						menu.addSeparator();
					} else {
						menu.add(getMenuItem(menuAndKeyActions[i], event.getPoint()));
					}
				}
				menu.show((Component) event.getSource(), event.getX() + 1, event.getY());
			}
		}
		
		private JMenuItem getMenuItem(final MenuKeyAction action, Point mousePosition) {
			JMenuItem result = new JMenuItem(action.getName(mousePosition));
			result.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					action.performAction();
				}
			});
			char hotkey = action.getHotkeyChar();
			if (hotkey != (char) 0) {
				result.setAccelerator(KeyStroke.getKeyStroke(new Character(hotkey), keyboardEquivalentModifier));
			}
			return result;
		}
	}
	
	public interface MenuKeyAction {
		public String getName(Point mousePosition);
		
		public void performAction();
		
		public char getHotkeyChar();
	}
	
	public class CopyAction implements MenuKeyAction {
		public String getName(Point mousePosition) {
			return "Copy";
		}
		
		public void performAction() {
			// FIXME: we should probably have an "explicit copy" mode.
		}
		
		public char getHotkeyChar() {
			return 'C';
		}
	}
	
	public class PasteAction implements MenuKeyAction {
		public String getName(Point mousePosition) {
			return "Paste";
		}
		
		public void performAction() {
			textPane.paste();
		}
		
		public char getHotkeyChar() {
			return 'V';
		}
	}
	
	public class RunCommandAction implements MenuKeyAction {
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
	}
	
	public class FindAction implements MenuKeyAction {
		public String getName(Point mousePosition) {
			return "Find...";
		}
		
		public void performAction() {
			controller.showFindDialogFor(textPane);
		}
		
		public char getHotkeyChar() {
			return 'F';
		}
	}
	
	public class FindNextAction implements MenuKeyAction {
		public String getName(Point mousePosition) {
			return "Find Next";
		}
		
		public void performAction() {
			textPane.findNext();
		}
		
		public char getHotkeyChar() {
			return 'G';
		}
	}
	
	public class FindPreviousAction implements MenuKeyAction {
		public String getName(Point mousePosition) {
			return "Find Previous";
		}
		
		public void performAction() {
			textPane.findPrevious();
		}
		
		public char getHotkeyChar() {
			return 'D';
		}
	}
	
	public class ClearScrollbackAction implements MenuKeyAction {
		public String getName(Point mousePosition) {
			return "Clear Scrollback";
		}
		
		public void performAction() {
			textPane.clearScrollBuffer();
			control.sendRedrawScreen();
		}
		
		public char getHotkeyChar() {
			return 'K';
		}
	}
	
	public class NewWindowAction implements MenuKeyAction {
		public String getName(Point mousePosition) {
			return "New Window";
		}
		
		public void performAction() {
			// TODO: Open a new window.
		}
		
		public char getHotkeyChar() {
			return 'N';
		}
	}
	
	public class NewTabAction implements MenuKeyAction {
		public String getName(Point mousePosition) {
			return "New Tab";
		}
		
		public void performAction() {
			controller.openShellPane(true);
		}
		
		public char getHotkeyChar() {
			return 'T';
		}
	}
	
	public class CloseTabAction implements MenuKeyAction {
		public String getName(Point mousePosition) {
			return "Close Tab";
		}
		
		public void performAction() {
			control.destroyProcess();
			controller.closeTerminalPane(JTerminalPane.this);
		}
		
		public char getHotkeyChar() {
			return 'W';
		}
	}
	
	public class ChangeColourAction implements MenuKeyAction {
		private String colourName;
		private String colourDescription;

		public String getName(Point mousePosition) {
			colourDescription = "Background";
			Location location = textPane.viewToModel(mousePosition);
			TextBuffer model = textPane.getModel();
			if (location.getLineIndex() >= model.getLineCount()) {
				colourName = "background";
			} else {
				TextLine line = model.get(location.getLineIndex());
				if (location.getCharOffset() >= line.length()) {
					colourName = "background";
				} else {
					byte style = line.getStyleAt(location.getCharOffset());
					if (line.getText().charAt(location.getCharOffset()) == ' ') {
						colourName = StyledText.getBackgroundColourName(style);
						colourDescription = StyledText.getBackgroundColourDescription(style);
					} else {
						colourName = StyledText.getForegroundColourName(style);
						colourDescription = StyledText.getForegroundColourDescription(style);
					}
				}
			}
			return "Change Colour " + colourDescription + "...";
		}
		
		public void performAction() {
			Color colour = Options.getSharedInstance().getColor(colourName);
			colour = JColorChooser.showDialog(JTerminalPane.this, "Change Colour " + colourDescription, colour);
			if (colour != null) {
				Options.getSharedInstance().setColor(colourName, colour);
				textPane.repaint();
			}
		}
		
		public char getHotkeyChar() {
			return (char) 0;  // No hot key for this.
		}
	}
}
