package terminator;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import terminator.view.*;

public class TerminatorFrame extends JFrame {
	private Terminator terminator;
	private Dimension terminalSize;
	private JTabbedPane tabbedPane;
	
	private ArrayList terminals = new ArrayList();

	public TerminatorFrame(Terminator terminator, JTerminalPaneFactory[] paneFactories) {
		super(Options.getSharedInstance().getTitle());
		this.terminator = terminator;
		JTerminalPane[] panes = new JTerminalPane[paneFactories.length];
		for (int i = 0; i < paneFactories.length; i++) {
			panes[i] = paneFactories[i].create();
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
		setTitle(title.toString());
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
	
	private void initIcon() {
		if (FRAME_ICON != null) {
			setIconImage(FRAME_ICON);
		}
	}
	
	private void initFrame() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		if (GuiUtilities.isMacOs() == false) {
			setBackground(Options.getSharedInstance().getColor("background"));
		}
		initIcon();
		
		if (Options.getSharedInstance().shouldUseMenuBar()) {
			setJMenuBar(new TerminatorMenuBar());
		}
		addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent event) {
				terminator.frameClosed(TerminatorFrame.this);
			}
		});
		initTerminals();
		pack();
		// FIXME: until Mac OS has Java 1.5, we'll have to set the java.awt.Window.locationByPlatform property instead.
		//setLocationByPlatform(true);
		setVisible(true);
		WindowMenu.getSharedInstance().addWindow(this);
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
		setContentPane(terminalPane);
		setTitle(terminalPane.getName());
	}
	
	private void initTabbedPane() {
		if (tabbedPane != null) {
			return;
		}
		
		JComponent oldContentPane = (JComponent) getContentPane();
		
		tabbedPane = new JTabbedPane();
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
		disableFocusTraversal(tabbedPane);
		
		if (oldContentPane instanceof JTerminalPane) {
			addPaneToUI((JTerminalPane) oldContentPane);
		}
		
		setContentPane(tabbedPane);
	}
	
	public static void disableFocusTraversal(Component c) {
		c.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
		c.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
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
			setContentPane(soleSurvivor);
			soleSurvivor.revalidate();
			repaint();
			tabbedPane = null;
			updateFrameTitle();
		}
	}
	
	/**
	 * Implements closeTerminalPane.
	 */
	private void closeWindow() {
		setVisible(false);
		dispose();
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
