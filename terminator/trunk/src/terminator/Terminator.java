package terminatorn;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import e.util.*;

import javax.swing.Timer;

public class Terminator implements Controller {
	private List arguments;
	
	private Dimension terminalSize;
	private JFrame frame;
	private JTabbedPane tabbedPane;
	
	private ArrayList terminals = new ArrayList();

	public Terminator(final String[] argumentArray) throws IOException {
		Log.setApplicationName("Terminator");
		arguments = Options.getSharedInstance().parseCommandLine(argumentArray);
		if (arguments.contains("-h") || arguments.contains("-help") || arguments.contains("--help")) {
			showUsage();
		}
		if (arguments.contains("-v") || arguments.contains("-version") || arguments.contains("--version")) {
			showVersion();
		}
		initUi();
	}
	
	public void updateFrameTitle() {
		StringBuffer title = new StringBuffer("Terminator");
		if (terminalSize != null) {
			title.append(" [").append(terminalSize.width).append(" x ").append(terminalSize.height).append("]");
		}
		if (terminals.size() >= 1) {
			JTerminalPane pane;
			if (tabbedPane == null) {
				pane = (JTerminalPane) terminals.get(0);
			} else {
				pane = (JTerminalPane) tabbedPane.getSelectedComponent();
			}
			if (pane != null) {
				title.append(" - ").append(pane.getName());
			}
		}
		frame.setTitle(title.toString());
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
			terminals.add(JTerminalPane.newCommandWithTitle(this, command, name));
			name = null;
		}
		
		if (arguments.isEmpty()) {
			terminals.add(JTerminalPane.newShell(this));
		}
		
		if (terminals.size() == 1) {
			initSingleTerminal();
		} else {
			initTabbedTerminals();
		}
	}
	
	private void initSingleTerminal() {
		JTerminalPane terminalPane = (JTerminalPane) terminals.get(0);
		frame.setContentPane(terminalPane);
		frame.setTitle(terminalPane.getName());
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
				updateFrameTitle();
			}
		});
		
		if (oldContentPane instanceof JTerminalPane) {
			addPaneToUI((JTerminalPane) oldContentPane);
		}
		
		frame.setContentPane(tabbedPane);
	}
	
	private void initTabbedTerminals() {
		initTabbedPane();
		for (int i = 0; i < terminals.size(); ++i) {
			JTerminalPane terminalPane = (JTerminalPane) terminals.get(i);
			addPaneToUI(terminalPane);
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
		((JTerminalPane) terminals.get(0)).requestFocus();
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
			((JTerminalPane) terminals.get(i)).start();
		}
	}

	/**
	 * Removes the given terminal. If this is the last terminal, close
	 * the window.
	 */
	public void closeTerminalPane(JTerminalPane victim) {
		terminals.remove(victim);
		if (tabbedPane != null) {
			closeTab(victim);
		} else {
			closeWindow();
		}
	}
	
	/**
	 * Implements closeTerminalPane.
	 */
	private void closeTab(JTerminalPane victim) {
		tabbedPane.remove(victim);
		if (tabbedPane.getTabCount() == 0) {
			closeWindow();
		} else if (tabbedPane.getTabCount() == 1) {
			JTerminalPane soleSurvivor = (JTerminalPane) terminals.get(0);
			soleSurvivor.invalidate();
			tabbedPane.remove(soleSurvivor);
			frame.setContentPane(soleSurvivor);
			soleSurvivor.revalidate();
			frame.repaint();
			tabbedPane = null;
			updateFrameTitle();
		}
	}
	
	/**
	 * Implements closeTerminalPane.
	 */
	private void closeWindow() {
		frame.setVisible(false);
		frame.dispose();
	}
	
	public void openShellPane(boolean focusOnNewTab) {
		addPane(JTerminalPane.newShell(this), focusOnNewTab);
	}
	
	public void openCommandPane(String command, boolean focusOnNewTab) {
		addPane(JTerminalPane.newCommandWithTitle(this, command, command), focusOnNewTab);
	}

	private Timer terminalSizeTimer = null;

	public void setTerminalSize(Dimension size) {
		this.terminalSize = size;
		updateFrameTitle();
		if (terminalSizeTimer != null) {
			terminalSizeTimer.stop();
		}
		terminalSizeTimer = new Timer(2000, new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				terminalSizeTimer = null;
				terminalSize = null;
				updateFrameTitle();
			}
		});
		terminalSizeTimer.setRepeats(false);
		terminalSizeTimer.start();
	}
	
	public void terminalNameChanged(JTerminalPane terminal) {
		if (tabbedPane != null) {
			int index = tabbedPane.indexOfComponent(terminal);
			tabbedPane.setTitleAt(index, terminal.getName());
		}
		updateFrameTitle();
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
	
	private void addPane(JTerminalPane newPane, boolean focusOnNewTab) {
		terminals.add(newPane);
		addPaneToUI(newPane);
		newPane.start();
		if (focusOnNewTab) {
			tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
		}
		updateFrameTitle();
	}
	
	private void addPaneToUI(JTerminalPane newPane) {
		initTabbedPane();
		tabbedPane.add(newPane.getName(), newPane);
		newPane.getTextPane().removeKeyListener(keyHandler);
		newPane.getTextPane().addKeyListener(keyHandler);
	}

	public void showUsage() {
		System.err.println("Usage: Terminator [--help] [-xrm <resource-string>]... [[-n <name>] command]...");
		System.exit(0);
	}
	
	public void showVersion() {
		System.err.println("Terminator 1.0 (28th May 2004), copyright Phil Norman, Elliott Hughes.");
		System.exit(0);
	}

	public static void main(final String[] arguments) throws IOException {
		new Terminator(arguments);
	}
}
