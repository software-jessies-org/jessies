package terminator;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import e.forms.*;
import e.gui.*;
import terminator.view.*;
import terminator.view.highlight.*;

import javax.swing.Timer;

/**

@author Phil Norman
*/

public class TerminatorFrame implements TerminalPaneMaster {
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
	
	/**
	 * An icon to use for each frame, if the terminator shell script suggested one.
	 * We don't use this on Mac OS, because there windows don't have icons
	 * unless the window's contents are themselves a graphic. On Linux, we use
	 * one of the available GNOME icons so we look like a terminal rather than a
	 * generic 'window'.
	 */
	private static final Image FRAME_ICON;
	static {
		String iconFile = System.getProperty("terminator.frame.icon");
		FRAME_ICON = (iconFile != null) ? new ImageIcon(iconFile).getImage() : null;
	}
	
	private void initIcon(JFrame frame) {
		if (FRAME_ICON != null) {
			frame.setIconImage(FRAME_ICON);
		}
	}
	
	private void initFrame() {
		frame = new JFrame(Options.getSharedInstance().getTitle());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setBackground(Options.getSharedInstance().getColor("background"));
		initIcon(frame);
		
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

	
	private FindField findField = new FindField();
	private JLabel findStatus = new JLabel(" ");
	private JTextBuffer textToFindIn;
	
	private class FindField extends EMonitoredTextField {
		public FindField() {
			super(40);
			addKeyListener(new KeyAdapter() {
				public void keyTyped(KeyEvent e) {
					if (textToFindIn != null && e.getKeyChar() == '\n') {
						find();
						e.consume();
					}
				}
				public void keyPressed(KeyEvent e) {
					if (textToFindIn != null && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						getFindHighlighter().forgetRegularExpression(textToFindIn);
					}
				}
				public void keyReleased(KeyEvent e) {
					if (JTerminalPane.isKeyboardEquivalent(e)) {
						if (e.getKeyCode() == KeyEvent.VK_D) {
							textToFindIn.findPrevious();
						} else if (e.getKeyCode() == KeyEvent.VK_G) {
							textToFindIn.findNext();
						}
					}
				}
			});
		}
		
		public void timerExpired() {
			find();
		}
		
		public void find() {
			String regularExpression = getText();
			try {
				int matchCount = getFindHighlighter().setRegularExpression(textToFindIn, regularExpression);
				findStatus.setText("Matches: " + matchCount);
				setForeground(UIManager.getColor("TextField.foreground"));
			} catch (PatternSyntaxException ex) {
				setForeground(Color.RED);
				findStatus.setText(ex.getDescription());
			}
		}
	}
	
	private FindHighlighter getFindHighlighter() {
		return (FindHighlighter) textToFindIn.getHighlighterOfClass(FindHighlighter.class);
	}
	
	public void showFindDialogFor(JTextBuffer text) {
		this.textToFindIn = text;
		
		FormPanel formPanel = new FormPanel();
		formPanel.addRow("Find:", findField);
		formPanel.setStatusBar(findStatus);
		FormDialog.showNonModal(frame, "Find", formPanel);
		
		findField.selectAll();
		findField.requestFocus();
		findStatus.setText(" ");
		
		findField.find();
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
