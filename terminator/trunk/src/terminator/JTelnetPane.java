package terminatorn;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

/**

@author Phil Norman
@author Elliott Hughes
*/

public class JTelnetPane extends JPanel {
	private Controller controller;
	private TelnetControl control;
	private JTextBuffer textPane;
	private String name;
	
	/**
	 * Creates a new terminal with the given name, running the given command.
	 */
	private JTelnetPane(Controller controller, String name, String command) {
		super(new BorderLayout());
		this.controller = controller;
		this.name = name;
		
		try {
			Log.warn("Starting process '" + command + "'");
			final Process proc = Runtime.getRuntime().exec(System.getProperty("pty.binary") + " " + command);
			init(command, proc);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Creates a new terminal running the given command, with the given
	 * title. If 'title' is null, we use the first word of the command
	 * as the the title.
	 */
	public static JTelnetPane newCommandWithTitle(Controller controller, String command, String title) {
		if (title == null) {
			title = command.trim();
			if (title.indexOf(' ') != -1) {
				title = title.substring(0, title.indexOf(' '));
			}
		}
		return new JTelnetPane(controller, title, command);
	}
	
	/**
	 * Creates a new terminal running the user's shell.
	 */
	public static JTelnetPane newShell(Controller controller) {
		String user = System.getProperty("user.name");
		String command = getUserShell(user);
		if (Options.getSharedInstance().isLoginShell()) {
			command += " -l";
		}
		return new JTelnetPane(controller, user + "@localhost", command);
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
					} catch (IOException ex) { }
				}
			}
		}
		return "bash";
	}

	private void init(String command, Process process) throws IOException {
		textPane = new JTextBuffer(controller);
		textPane.addKeyListener(new KeyHandler());
		
		JScrollPane scrollPane = new JScrollPane(new BorderPanel(textPane));
		scrollPane.setBorder(null);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.getViewport().setBackground(textPane.getBackground());
		
		fixScrollBarForMacOs(scrollPane);
		
		add(scrollPane, BorderLayout.CENTER);
		
		initSizeMonitoring(scrollPane);
		textPane.sizeChanged();
		try {
			control = new TelnetControl(this, textPane.getModel(), command, process);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public Controller getController() {
		return controller;
	}
	
	private void initSizeMonitoring(JScrollPane scrollPane) {
		scrollPane.addComponentListener(new ComponentAdapter() {
			private Dimension currentSize;
			public void componentShown(ComponentEvent e) {
				this.currentSize = textPane.getVisibleSizeInCharacters();
			}
			public void componentResized(ComponentEvent e) {
				Dimension size = textPane.getVisibleSizeInCharacters();
				if (size.equals(currentSize) == false) {
					try {
						control.sizeChanged(size, textPane.getVisibleSize());
					} catch (IOException ex) {
						Log.warn("Failed to notify pty of size change.", ex);
					}
					currentSize = size;
				}
			}
		});
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
		if (System.getProperty("os.name").indexOf("Mac OS") == -1) {
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
	
	public Dimension getOptimalViewSize() {
		return textPane.getOptimalViewSize();
	}
	
	private class KeyHandler implements KeyListener {
		/**
		 * On the Mac, the Command key (called 'meta' by Java) is always
		 * used for keyboard equivalents. On other systems, Control tends to
		 * be used, but in the special case of terminal emulators this
		 * conflicts with the ability to type control characters. The
		 * traditional work-around has always been to use Alt, which --
		 * conveniently for Mac users -- is in the same place on a PC
		 * keyboard as Command on a Mac keyboard.
		 */
		private int keyboardEquivalentModifier;
		{
			if (System.getProperty("os.name").indexOf("Mac OS") != -1) {
				keyboardEquivalentModifier = KeyEvent.META_MASK;
			} else {
				keyboardEquivalentModifier = KeyEvent.ALT_MASK;
			}
		}
		
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
				switch (ch) {
					case 'e': case 'E':
						String commandToRun = StringEntryDialog.getString(JTelnetPane.this, "Enter Command to Run");
						if (commandToRun != null && commandToRun.length() > 0) {
							controller.openCommandPane(commandToRun, true);
						}
						break;
					case 'f': case 'F':
						controller.showFindDialogFor(textPane);
						break;
					case 'k': case 'K':
						textPane.clearScrollBuffer();
						break;
					case 'n': case 'N':
						// TODO: Open a new window.
						break;
					case 't': case 'T':
						controller.openShellPane(true);
						break;
					case 'w': case 'W':
						control.destroyProcess();
						controller.closeTelnetPane(JTelnetPane.this);
						break;
				}
			} else {
				if (ch != KeyEvent.CHAR_UNDEFINED) {
					control.sendChar(ch);
					scroll();
				}
			}
			event.consume();
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
}
