package terminatorn;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import java.util.List;

public class Terminator implements Controller {
	private List arguments;
	
	private JFrame frame;
	private JTabbedPane tabbedPane;
	
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
		frame = new JFrame(Options.getSharedInstance().getTitle());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(makeContentPane());
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	private JComponent makeContentPane() {
		String name = null;
		for (int i = 0; i < arguments.size(); ++i) {
			String word = (String) arguments.get(i);
			if (word.equals("-n")) {
				name = (String) arguments.get(++i);
				continue;
			}
			
			String command = word;
			terminals.add(JTelnetPane.newCommandWithTitle(this, command, name));
			name = null;
		}
		
		if (arguments.isEmpty()) {
			terminals.add(JTelnetPane.newShell(this));
		}
		
//		return (terminals.size() == 1) ? makeSingleTerminal() : makeTabbedTerminals();
		return makeTabbedTerminals();
	}
	
	private JComponent makeSingleTerminal() {
		JTelnetPane telnetPane = (JTelnetPane) terminals.get(0);
		frame.setTitle(telnetPane.getName());
		return telnetPane;
	}
	
	private JComponent makeTabbedTerminals() {
		tabbedPane = new JTabbedPane() {
			/**
			 * Prevents the tabs (ass opposed to their components)
			 * from getting the focus.
			 */
			public boolean isFocusTraversable() {
				return false;
			}
		};
		tabbedPane.addChangeListener(new ChangeListener() {
			/**
			 * Ensures that when we change tab, we give focus to that terminal.
			 */
			public void stateChanged(ChangeEvent e) {
				final Component selected = tabbedPane.getSelectedComponent();
				if (selected != null) {
					// I dislike the invokeLater, but it's sadly necessary.
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							selected.requestFocus();
						}
					});
				}
			}
		});
		for (int i = 0; i < terminals.size(); ++i) {
			JTelnetPane telnetPane = (JTelnetPane) terminals.get(i);
			addPaneToUI(telnetPane);
		}
		return tabbedPane;
	}
	
	private class KeyHandler implements KeyListener {
		public void keyPressed(KeyEvent event) {
			if (event.isAltDown()) {
				switch (event.getKeyCode()) {
					case KeyEvent.VK_RIGHT:
						setSelectedTab(tabbedPane.getSelectedIndex() + 1);
						break;
					case KeyEvent.VK_LEFT:
						setSelectedTab(tabbedPane.getSelectedIndex() - 1);
						break;
				}
			}
		}
		public void keyReleased(KeyEvent event) { }
		public void keyTyped(KeyEvent event) { }
	}
	
	private void setSelectedTab(int index) {
		int tabCount = tabbedPane.getTabCount();
		tabbedPane.setSelectedIndex((index + tabCount) % tabCount);
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

	public void closeTelnetPane(JTelnetPane victim) {
		terminals.remove(victim);
		tabbedPane.remove(victim);
		if (tabbedPane.getTabCount() == 0) {
			frame.setVisible(false);
			frame.dispose();
		}
	}
	
	public void openShellPane(boolean focusOnNewTab) {
		addPane(JTelnetPane.newShell(this), focusOnNewTab);
	}
	
	public void openCommandPane(String command, boolean focusOnNewTab) {
		addPane(JTelnetPane.newCommandWithTitle(this, command, command), focusOnNewTab);
	}
	
	private void addPane(JTelnetPane newPane, boolean focusOnNewTab) {
		terminals.add(newPane);
		addPaneToUI(newPane);
		newPane.start();
		if (focusOnNewTab) {
			tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
		}
	}
	
	private void addPaneToUI(JTelnetPane newPane) {
		tabbedPane.add(newPane.getName(), newPane);
		newPane.getTextPane().addKeyListener(new KeyHandler());
	}

	public void showUsage() {
		System.err.println("Usage: Terminator [--help] [-xrm <resource-string>]... [[-n <name>] command]...");
		System.exit(0);
	}

	public static void main(final String[] arguments) throws IOException {
		new Terminator(arguments);
	}
}
