package terminatorn;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

/**

@author Phil Norman
*/

public class JTelnetPane extends JScrollPane {
	private TelnetControl control;
	private JTextBuffer textPane;
	private int viewWidth = 80;
	private int viewHeight = 24;
	
	public JTelnetPane(String host, int port) {
		textPane = new JTextBuffer();
		textPane.addKeyListener(new KeyHandler());
		getViewport().setView(textPane);
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
//			event.consume();
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
				frame.setVisible(true);
				frame.setLocationRelativeTo(null);
			}
		});
	}
}
