package terminator;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import e.gui.*;

import javax.swing.Timer;

/**

@author Phil Norman
*/

public class TerminatorFrame implements Controller {
	private Terminator terminator;
	private Dimension terminalSize;
	private JFrame frame;
	private JTabbedPane tabbedPane;
	
	private ArrayList terminals = new ArrayList();

	public TerminatorFrame(Terminator terminator, JTerminalPaneFactory[] paneFactories) {
		this.terminator = terminator;
		JTerminalPane[] panes = new JTerminalPane[paneFactories.length];
		for (int i = 0; i < paneFactories.length; i++) {
			panes[i] = paneFactories[i].create(this);
			terminals.add(panes[i]);
		}
		initFrame();
		initFindField();
		initFocus();
		for (int i = 0; i < panes.length; i++) {
			panes[i].start();
		}
	}
	
	public void updateFrameTitle() {
		StringBuffer title = new StringBuffer();
		if (terminalSize != null) {
			title.append("[").append(terminalSize.width).append(" x ").append(terminalSize.height).append("] ");
		}
		if (terminals.size() >= 1) {
			JTerminalPane pane;
			if (tabbedPane == null) {
				pane = (JTerminalPane) terminals.get(0);
			} else {
				pane = (JTerminalPane) tabbedPane.getSelectedComponent();
			}
			if (pane != null) {
				title.append(pane.getName());
			}
		}
		frame.setTitle(title.toString());
	}
	
	private void initFrame() {
		frame = new JFrame(Options.getSharedInstance().getTitle());
		frame.setBackground(Options.getSharedInstance().getColor("background"));
		
		if (Options.getSharedInstance().shouldUseMenuBar()) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			frame.setJMenuBar(new TerminatorMenuBar());
		}
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent event) {
				terminator.frameClosed(TerminatorFrame.this);
			}
		});
		initTerminals();
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	private void initTerminals() {
		if (terminals.size() == 1) {
			initSingleTerminal();
		} else {
			initTabbedTerminals();
		}
	}
	
	/**
	 * Give focus to the first terminal.
	 */
	private void initFocus() {
		((JTerminalPane) terminals.get(0)).requestFocus();
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
	
	public void openNewWindow() {
		terminator.openFrame();
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
	
	private class FindField extends EMonitoredTextField {
		public FindField() {
			super(40);
			addFocusListener(new FocusAdapter() {
				public void focusLost(FocusEvent e) {
					hideFindDialog();
				}
			});
			addKeyListener(new KeyAdapter() {
				public void keyTyped(KeyEvent e) {
					if (textToFindIn != null && e.getKeyChar() == '\n') {
						find();
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
		
		public void timerExpired() {
			find();
		}
		
		private void find() {
			String regularExpression = getText();
			try {
				getFindHighlighter().setRegularExpression(textToFindIn, regularExpression);
				setForeground(UIManager.getColor("TextField.foreground"));
			} catch (PatternSyntaxException ex) {
				setForeground(Color.RED);
			}
		}
	}
	
	private void initFindField() {
		findField = new FindField();
		// Make sure the find dialog is closed if the user drags the window.
		// Strangely, on Linux this doesn't seem to cause the find dialog to lose the focus.
		frame.addComponentListener(new ComponentAdapter() {
			public void componentMoved(ComponentEvent e) {
				hideFindDialog();
			}
		});
	}
	
	private FindHighlighter getFindHighlighter() {
		return (FindHighlighter) textToFindIn.getHighlighterOfClass(FindHighlighter.class);
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
}
