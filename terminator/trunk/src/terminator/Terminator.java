package terminatorn;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class Terminator {
	public Terminator(final String[] arguments) throws IOException {
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
					telnetPanes.add(JTelnetPane.newTelnetHostAndPort(hostAndPort));
				}
				if (arguments.length == 0) {
					telnetPanes.add(JTelnetPane.newShell());
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
				
				// Give focus to the first terminal.
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

	public static void main(final String[] arguments) throws IOException {
		new Terminator(arguments);
	}
}
