package terminatorn;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import e.util.*;

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
				initFindField();
				initFocus();
				startThreads();
			}
		});
	}
	
	private void initFrame() {
		frame = new JFrame(Options.getSharedInstance().getTitle());
		frame.setBackground(Options.getSharedInstance().getColor("background"));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		initTerminals();
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	private void initTerminals() {
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
		
		if (terminals.size() == 1) {
			initSingleTerminal();
		} else {
			initTabbedTerminals();
		}
	}
	
	private void initSingleTerminal() {
		JTelnetPane telnetPane = (JTelnetPane) terminals.get(0);
		frame.setContentPane(telnetPane);
		frame.setTitle(telnetPane.getName());
	}
	
	private void initTabbedPane() {
		if (tabbedPane != null) {
			return;
		}
		
		JComponent oldContentPane = (JComponent) frame.getContentPane();
		
		tabbedPane = new JTabbedPane() {
			/**
			 * Prevents the tabs (as opposed to their components)
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
		
		if (oldContentPane instanceof JTelnetPane) {
			addPaneToUI((JTelnetPane) oldContentPane);
		}
		
		frame.setContentPane(tabbedPane);
	}
	
	private void initTabbedTerminals() {
		initTabbedPane();
		for (int i = 0; i < terminals.size(); ++i) {
			JTelnetPane telnetPane = (JTelnetPane) terminals.get(i);
			addPaneToUI(telnetPane);
		}
	}
	
	private final KeyHandler keyHandler = new KeyHandler();
	
	private class KeyHandler implements KeyListener {
		public void keyPressed(KeyEvent event) {
			if (event.isAltDown()) {
				switch (event.getKeyCode()) {
					case KeyEvent.VK_RIGHT:
						if (tabbedPane != null) {
							setSelectedTab(tabbedPane.getSelectedIndex() + 1);
							event.consume();
						}
						break;
					case KeyEvent.VK_LEFT:
						if (tabbedPane != null) {
							setSelectedTab(tabbedPane.getSelectedIndex() - 1);
							event.consume();
						}
						break;
				}
			}
		}
		public void keyReleased(KeyEvent event) { }
		public void keyTyped(KeyEvent event) { }
		
		private void setSelectedTab(int index) {
			int tabCount = tabbedPane.getTabCount();
			tabbedPane.setSelectedIndex((index + tabCount) % tabCount);
		}
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

	/**
	 * Removes the given terminal. If this is the last terminal, close
	 * the window.
	 */
	public void closeTelnetPane(JTelnetPane victim) {
		terminals.remove(victim);
		if (tabbedPane != null) {
			closeTab(victim);
		} else {
			closeWindow();
		}
	}
	
	/**
	 * Implements closeTelnetPane.
	 */
	private void closeTab(JTelnetPane victim) {
		tabbedPane.remove(victim);
		if (tabbedPane.getTabCount() == 0) {
			closeWindow();
		} else if (tabbedPane.getTabCount() == 1) {
			JTelnetPane soleSurvivor = (JTelnetPane) terminals.get(0);
			soleSurvivor.invalidate();
			tabbedPane.remove(soleSurvivor);
			frame.setContentPane(soleSurvivor);
			soleSurvivor.revalidate();
			frame.repaint();
			tabbedPane = null;
		}
	}
	
	/**
	 * Implements closeTelnetPane.
	 */
	private void closeWindow() {
		frame.setVisible(false);
		frame.dispose();
	}
	
	public void openShellPane(boolean focusOnNewTab) {
		addPane(JTelnetPane.newShell(this), focusOnNewTab);
	}
	
	public void openCommandPane(String command, boolean focusOnNewTab) {
		addPane(JTelnetPane.newCommandWithTitle(this, command, command), focusOnNewTab);
	}
	
	private JWindow findWindow;
	private JTextField findField;
	private JTextBuffer textToFindIn;
	
	private void initFindField() {
		findField = new JTextField("", 40);
		findField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				hideFindDialog();
			}
		});
		findField.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				if (textToFindIn != null && e.getKeyChar() == '\n') {
					find(findField.getText());
					hideFindDialog();
					e.consume();
				}
			}
			public void keyPressed(KeyEvent e) {
				if (textToFindIn != null && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					getFindHighlighter().forgetRegularExpression(textToFindIn);
					hideFindDialog();
					e.consume();
				}
			}
		});
	}
	
	private FindHighlighter getFindHighlighter() {
		return (FindHighlighter) textToFindIn.getHighlighterOfClass(FindHighlighter.class);
	}
	
	private void find(String regularExpression) {
		getFindHighlighter().setRegularExpression(textToFindIn, regularExpression);
	}
	
	public void showFindDialogFor(JTextBuffer text) {
		hideFindDialog();
		
		this.textToFindIn = text;
		
		Point location = frame.getLocationOnScreen();
		location.y += frame.getHeight();
		
		findWindow = new JWindow(frame);
		findWindow.setContentPane(findField);
		findWindow.setLocation(location);
		findWindow.pack();
		
		Dimension findWindowSize = findWindow.getSize();
		findWindowSize.width = frame.getWidth();
		findWindow.setSize(findWindowSize);
		
		findField.selectAll();
		
		findWindow.setVisible(true);
		findField.requestFocus();
	}
	
	private void hideFindDialog() {
		if (findWindow != null) {
			findWindow.setVisible(false);
			findWindow.dispose();
			findWindow = null;
		}
		if (textToFindIn != null) {
			textToFindIn.requestFocus();
			textToFindIn = null;
		}
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
		initTabbedPane();
		tabbedPane.add(newPane.getName(), newPane);
		newPane.getTextPane().removeKeyListener(keyHandler);
		newPane.getTextPane().addKeyListener(keyHandler);
	}

	public void showUsage() {
		System.err.println("Usage: Terminator [--help] [-xrm <resource-string>]... [[-n <name>] command]...");
		System.exit(0);
	}

	public static void main(final String[] arguments) throws IOException {
		new Terminator(arguments);
	}
}
