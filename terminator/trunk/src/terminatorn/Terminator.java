package terminatorn;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class Terminator {
	private List arguments;
	
	private JFrame frame;
	
	private ArrayList terminals = new ArrayList();

	public Terminator(final String[] argumentArray) throws IOException {
		Log.setApplicationName("Terminator");
		arguments = Options.getSharedInstance().parseCommandLine(argumentArray);
		if (arguments.contains("-h") || arguments.contains("-help") || arguments.contains("--help")) {
			showUsage();
		}
		initUi();
	}
	
	/**
	 * Sets up the user interface on the AWT event thread. I've never
	 * seen this matter in practice, but strictly speaking, you're
	 * supposed to do this.
	 */
	private void initUi() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				initFrame();
				initFocus();
				startThreads();
			}
		});
	}
	
	private void initFrame() {
		frame = new JFrame("Terminator");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(makeContentPane());
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	private JComponent makeContentPane() {
		for (int i = 0; i < arguments.size(); ++i) {
			String hostAndPort = (String) arguments.get(i);
			terminals.add(JTelnetPane.newTelnetHostAndPort(hostAndPort));
		}
		
		if (arguments.isEmpty()) {
			terminals.add(JTelnetPane.newShell());
		}
		
		return (terminals.size() == 1) ? makeSingleTerminal() : makeTabbedTerminals();
	}
	
	private JComponent makeSingleTerminal() {
		JTelnetPane telnetPane = (JTelnetPane) terminals.get(0);
		frame.setTitle(telnetPane.getName());
		return telnetPane;
	}
	
	private JComponent makeTabbedTerminals() {
		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addChangeListener(new ChangeListener() {
			/**
			 * Ensures that when we change tab, we give focus to that terminal.
			 */
			public void stateChanged(ChangeEvent e) {
				tabbedPane.getSelectedComponent().requestFocus();
			}
		});
		for (int i = 0; i < terminals.size(); ++i) {
			JTelnetPane telnetPane = (JTelnetPane) terminals.get(i);
			tabbedPane.add(telnetPane.getName(), telnetPane);
		}
		return tabbedPane;
	}
	
	/**
	 * Give focus to the first terminal.
	 */
	private void initFocus() {
		((JTelnetPane) terminals.get(0)).requestFocus();
	}
	
	/**
	 * Starts up the threads listening on the connections after the UI is
	 * visible.
	 * If we don't do this separately, we get a race condition whereby
	 * title setting (or other actions which need a JFrame) can be
	 * performed before the JFrame is available.
	 */
	private void startThreads() {
		for (int i = 0; i < terminals.size(); i++) {
			((JTelnetPane) terminals.get(i)).start();
		}
	}

	public void showUsage() {
		System.err.println("Usage: Terminator [--help] [<host>[:<port>]]...");
		System.exit(0);
	}

	public static void main(final String[] arguments) throws IOException {
		new Terminator(arguments);
	}
}
