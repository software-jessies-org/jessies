package terminatorn;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

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
	
	public JTelnetPane(String hostAndPort) {
		super(new BorderLayout());
		
		this.name = hostAndPort;
		
		String host = hostAndPort;
		int port = 23;
		if (name.indexOf(':') != -1) {
			port = Integer.parseInt(name.substring(name.indexOf(':') + 1));
			host = name.substring(0, name.indexOf(':'));
			if (name.endsWith("/")) {
				name = name.substring(0, name.length() - 1);
			}
		}

		textPane = new JTextBuffer();
		textPane.addKeyListener(new KeyHandler());
		JScrollPane scrollPane = new JScrollPane(textPane);
		scrollPane.getViewport().setBackground(Color.WHITE);
		add(scrollPane, BorderLayout.CENTER);
		textPane.sizeChanged();
		try {
			Socket sock = new Socket(host, port);
			control = new TelnetControl(textPane.getModel(), sock.getInputStream(), sock.getOutputStream());
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public String getName() {
		return name;
	}
	
	public Dimension getOptimalViewSize() {
		return textPane.getOptimalViewSize();
	}
	
	private class KeyHandler implements KeyListener {
		public void keyPressed(KeyEvent event) {
			String sequence = getSequenceForKeyCode(event.getKeyCode());
			if (sequence != null) {
				control.sendEscapeString(sequence);
				event.consume();
			}
		}

		private String getSequenceForKeyCode(int keyCode) {
			switch (keyCode) {
				case KeyEvent.VK_UP: return "[A";
				case KeyEvent.VK_DOWN: return "[B";
				case KeyEvent.VK_RIGHT: return "[C";
				case KeyEvent.VK_LEFT: return "[D";
				default: return null;
			}
		}

		public void keyReleased(KeyEvent event) {
//			event.consume();
		}

		public void keyTyped(KeyEvent event) {
			char ch = event.getKeyChar();
//			System.err.println("Got key " + ((int) ch));
//			if (ch != KeyEvent.CHAR_UNDEFINED) {
				control.sendChar(ch);
//			}
			event.consume();
		}
	}

	public static void main(final String[] arguments) throws IOException {
		Log.setApplicationName("Terminator");
		if (arguments.length < 1) {
			System.err.println("Usage: JTelnetPane <host>[:<port>]...");
			System.exit(1);
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame("Terminator");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				
				ArrayList telnetPanes = new ArrayList();
				for (int i = 0; i < arguments.length; ++i) {
					String hostAndPort = arguments[i];
					telnetPanes.add(new JTelnetPane(hostAndPort));
				}

				JComponent content = null;
				if (telnetPanes.size() == 1) {
					JTelnetPane telnetPane = (JTelnetPane) telnetPanes.get(0);
					frame.setTitle(telnetPane.getName());
					content = telnetPane;
				} else {
					JTabbedPane tabbedPane = new JTabbedPane();
					for (int i = 0; i < telnetPanes.size(); ++i) {
						JTelnetPane telnetPane = (JTelnetPane) telnetPanes.get(i);
						tabbedPane.add(telnetPane.getName(), telnetPane);
					}
					content = tabbedPane;
				}
				
				frame.getContentPane().add(content);
				frame.setSize(new Dimension(600, 400));
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});
	}
}
