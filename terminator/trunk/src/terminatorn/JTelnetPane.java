package terminatorn;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
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
	
	public JTelnetPane(String host, int port) {
		super(new BorderLayout());
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

	public static void main(String[] argv) throws IOException {
		if (argv.length < 1 || argv.length > 2) {
			System.err.println("Usage: JTelnetPane <host> [<port>]");
			System.exit(1);
		}
		final String host = argv[0];
		final int port = (argv.length == 2) ? Integer.parseInt(argv[1]) : 23;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame("telnet://" + host + ":" + port);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				JTelnetPane telPane = new JTelnetPane(host, port);
				frame.getContentPane().add(telPane);
				frame.setSize(new Dimension(600, 400));
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});
	}
}
