package terminatorn;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

/**

@author Phil Norman
@author Elliott Hughes
*/

public class JTelnetPane extends JPanel {
	private TelnetControl control;
	private JTextBuffer textPane;
	private int viewWidth = 80;
	private int viewHeight = 24;
	private String name;
	
	/**
	 * Creates a new terminal with the given name, running the given command.
	 */
	private JTelnetPane(String name, String command) {
		super(new BorderLayout());
		this.name = name;
		
		try {
			Log.warn("Starting process '" + command + "'");
			final Process proc = Runtime.getRuntime().exec("pty " + command);
			init(command, proc.getInputStream(), proc.getOutputStream());
			// Probably should do this somewhere else rather than setting up a whole Thread for it.
			Thread reaper = new Thread(new Runnable() {
				public void run() {
					try {
						proc.waitFor();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			});
			reaper.start();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Creates a new terminal running telnet to the given host and port.
	 * The hostAndPort parameter is of the form "host:port", as in a telnet:// URI.
	 */
	public static JTelnetPane newTelnetHostAndPort(String hostAndPort) {
		String host = hostAndPort;
		int port = 23;
		String title = hostAndPort;
		if (title.indexOf(':') != -1) {
			port = Integer.parseInt(title.substring(title.indexOf(':') + 1));
			host = title.substring(0, title.indexOf(':'));
			if (title.endsWith("/")) {
				title = title.substring(0, title.length() - 1);
			}
		}
		String command = "telnet " + host + " " + port;
		return new JTelnetPane(title, command);
	}
	
	/**
	 * Creates a new terminal running the user's shell.
	 */
	public static JTelnetPane newShell() {
		String user = System.getProperty("user.name");
		String command = getUserShell(user);
		if (Options.getSharedInstance().isLoginShell()) {
			command += " -l";
		}
		return new JTelnetPane(user + "@localhost", command);
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

	private void init(String command, InputStream in, OutputStream out) throws IOException {
		textPane = new JTextBuffer();
		textPane.addKeyListener(new KeyHandler());
		
		// Add a border. (We could simplify this if JTextBuffer coped
		// with having a border.)
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.add(textPane, BorderLayout.CENTER);
		wrapper.setBackground(textPane.getBackground());
		final int internalBorder = Options.getSharedInstance().getInternalBorder();
		wrapper.setBorder(new javax.swing.border.EmptyBorder(internalBorder, internalBorder, internalBorder, internalBorder));
		
		JScrollPane scrollPane = new JScrollPane(wrapper, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getViewport().setBackground(textPane.getBackground());
		scrollPane.setBorder(null);
		add(scrollPane, BorderLayout.CENTER);
		
		textPane.sizeChanged();
		try {
			control = new TelnetControl(textPane.getModel(), command, in, out);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
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
		public void keyPressed(KeyEvent event) {
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
//			System.err.println("Got key " + ((int) ch));
			if (ch != KeyEvent.CHAR_UNDEFINED) {
				control.sendChar(ch);
				scroll();
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

	public static void main(final String[] arguments) throws IOException {
		Log.setApplicationName("Terminator");
		if (arguments.length == 1 && arguments[0].endsWith("-help")) {
			System.err.println("Usage: JTelnetPane [<host>[:<port>]]...");
			System.exit(0);
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame("Terminator");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				
				ArrayList telnetPanes = new ArrayList();
				for (int i = 0; i < arguments.length; ++i) {
					String hostAndPort = arguments[i];
					telnetPanes.add(newTelnetHostAndPort(hostAndPort));
				}
				if (arguments.length == 0) {
					telnetPanes.add(newShell());
				}

				JComponent content = null;
				if (telnetPanes.size() == 1) {
					JTelnetPane telnetPane = (JTelnetPane) telnetPanes.get(0);
					frame.setTitle(telnetPane.getName());
					content = telnetPane;
				} else {
					final JTabbedPane tabbedPane = new JTabbedPane();
					tabbedPane.addChangeListener(new ChangeListener() {
						/**
						 * Ensures that when we change tab, we give focus to that terminal.
						 */
						public void stateChanged(ChangeEvent e) {
							tabbedPane.getSelectedComponent().requestFocus();
						}
					});
					for (int i = 0; i < telnetPanes.size(); ++i) {
						JTelnetPane telnetPane = (JTelnetPane) telnetPanes.get(i);
						tabbedPane.add(telnetPane.getName(), telnetPane);
					}
					content = tabbedPane;
				}
				
				frame.getContentPane().add(content);
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
				
				//
				((JTelnetPane) telnetPanes.get(0)).requestFocus();
				
				// Start up the threads listening on the connections now that the UI is up.
				// If we don't do this separately, we get a race condition whereby title
				// setting (or other actions which need a JFrame) can be performed before
				// the JFrame is available.
				for (int i = 0; i < telnetPanes.size(); i++) {
					((JTelnetPane) telnetPanes.get(i)).start();
				}
			}
		});
	}
}
