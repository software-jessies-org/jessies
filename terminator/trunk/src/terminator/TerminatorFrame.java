package terminator;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;
import terminator.view.*;

public class TerminatorFrame extends JFrame implements TerminalPaneHost {
    private Dimension terminalSize;
    private TerminatorTabbedPane tabbedPane;
    
    private ArrayList<JTerminalPane> terminals;
    
    private Timer terminalSizeTimer;
    
    private final Color originalBackground = getBackground();
    
    public TerminatorFrame(List<JTerminalPane> initialTerminalPanes) {
        super("Terminator");
        terminals = new ArrayList<JTerminalPane>(initialTerminalPanes);
        initFrame();
        initFocus();
        for (JTerminalPane terminal : terminals) {
            terminal.start(this);
        }
    }
    
    public void updateFrameTitle() {
        StringBuilder title = new StringBuilder();
        if (terminalSize != null) {
            title.append("[").append(terminalSize.width).append(" x ").append(terminalSize.height).append("] ");
        }
        if (terminals.size() >= 1) {
            JTerminalPane pane;
            if (tabbedPane == null) {
                pane = terminals.get(0);
            } else {
                pane = (JTerminalPane) tabbedPane.getSelectedComponent();
            }
            if (pane != null) {
                title.append(pane.getTerminalName());
            }
        }
        setTitle(title.toString());
    }
    
    private void initFrame() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Misnomer: we add our own WindowListener.
        
        JFrameUtilities.setFrameIcon(this);
        
        Terminator.getSharedInstance().getFrames().addFrame(this);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                Terminator.getSharedInstance().getFrames().frameStateChanged();
            }
            
            @Override
            public void windowClosing(WindowEvent event) {
                handleWindowCloseRequestFromUser();
            }
            
            @Override
            public void windowIconified(WindowEvent event) {
                Terminator.getSharedInstance().getFrames().frameStateChanged();
            }
            
            @Override
            public void windowDeiconified(WindowEvent event) {
                Terminator.getSharedInstance().getFrames().frameStateChanged();
            }
        });
        
        // Work around Sun bug 6526971 (quick alt-tabbing on Windows can give focus to menu bar).
        if (GuiUtilities.isWindows()) {
            addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowLostFocus(WindowEvent e) {
                    MenuSelectionManager.defaultManager().clearSelectedPath();
                }
            });
        }
        
        initTerminals();
        optionsDidChange();
        
        pack();
        setVisible(true);
        
        if (GuiUtilities.isMacOs()) {
            WindowMenu.getSharedInstance().addWindow(this);
        }
    }
    
    private void initTerminals() {
        // Set up the basic single-pane UI...
        switchToSinglePane();
        // Add any other terminals...
        for (int i = 1; i < terminals.size(); ++i) {
            addPaneToUI(terminals.get(i));
        }
        // And make sure the user got what they wanted...
        updateTabbedPane();
    }
    
    private void updateTabbedPane() {
        if (terminals.size() == 1 && !Terminator.getPreferences().getBoolean(TerminatorPreferences.ALWAYS_SHOW_TABS)) {
            if (tabbedPane != null) {
                switchToSinglePane();
            }
        } else {
            switchToTabbedPane();
        }
    }
    
    /**
     * It's okay to call this multiple times, and we deliberately do so whenever the preferences change.
     * This lets us update suggested keystrokes when the use-alt-as-meta option changes.
     */
    private void updateMenuBar() {
        // Replace any existing menu bar.
        setJMenuBar(new TerminatorMenuBar());
        
        // Work around Sun bug 4949810 (setJMenuBar doesn't call revalidate/repaint).
        getJMenuBar().revalidate();
    }
    
    /**
     * Give focus to the first terminal.
     */
    private void initFocus() {
        terminals.get(0).requestFocus();
    }
    
    /**
     * Switches to a tabbed-pane UI where we can have one tab per terminal.
     */
    private void switchToTabbedPane() {
        if (tabbedPane != null) {
            return;
        }
        
        updateBackground();
        
        JComponent oldContentPane = (JComponent) getContentPane();
        Dimension initialSize = oldContentPane.getSize();
        
        tabbedPane = new TerminatorTabbedPane();
        if (oldContentPane instanceof JTerminalPane) {
            addPaneToUI((JTerminalPane) oldContentPane);
        }
        setContentPane(tabbedPane);
        validate();
        
        Dimension finalSize = oldContentPane.getSize();
        fixTerminalSizesAfterAddingOrRemovingTabbedPane(initialSize, finalSize);
    }
    
    /**
     * Switches to a simple UI where we can have only one terminal.
     */
    private void switchToSinglePane() {
        // It's only safe to call this if tabbedPane != null *or* we're setting up the window contents for the first time.
        
        updateBackground();
        
        JTerminalPane soleSurvivor = terminals.get(0);
        Dimension initialSize = soleSurvivor.getSize();
        
        soleSurvivor.invalidate();
        setContentPane(soleSurvivor);
        validate();
        
        soleSurvivor.requestFocus();
        tabbedPane = null;
        
        Dimension finalSize = getContentPane().getSize();
        fixTerminalSizesAfterAddingOrRemovingTabbedPane(initialSize, finalSize);
        
        updateFrameTitle();
    }
    
    /**
     * Increases the size of the frame based on the amount of space taken
     * away from the terminal to insert the tabbed pane. The end result
     * should be that the terminal size remains constant but the window
     * grows.
     */
    private void fixTerminalSizesAfterAddingOrRemovingTabbedPane(Dimension initialSize, Dimension finalSize) {
        // GNOME's current default window manager automatically ignores setSize if the window is maximized.
        // Windows doesn't, and that causes us to resize a maximized window to be larger than the display, which is obviously unwanted.
        // This early exit fixes Windows' behavior and doesn't hurt Linux.
        if ((getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH) {
            return;
        }
        
        Dimension size = getSize();
        size.height += (initialSize.height - finalSize.height);
        size.width += (initialSize.width - finalSize.width);
        
        // We dealt above with the case where the window is maximized, but we also have to deal with the case where the window is simply very tall.
        // GNOME and Mac OS will correctly constrain the window for us, but on Windows we have to try to do it ourselves.
        if (GuiUtilities.isWindows()) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
            final int availableVerticalScreenSpace = screenSize.height - screenInsets.top - screenInsets.bottom;
            if (getLocation().y + size.height > availableVerticalScreenSpace) {
                size.height = availableVerticalScreenSpace - getLocation().y;
            }
        }
        
        setSize(size);
    }
    
    public int getTerminalPaneCount() {
        if (tabbedPane == null) {
            return 1;
        }
        return tabbedPane.getTabCount();
    }
    
    public void detachCurrentTab() {
        JTerminalPane escapee = (JTerminalPane) tabbedPane.getSelectedComponent();
        terminals.remove(escapee);
        closeTab(escapee);
        Terminator.getSharedInstance().openFrame(escapee);
    }
    
    public void cycleTab(int delta) {
        if (tabbedPane != null) {
            int tabCount = tabbedPane.getTabCount();
            tabbedPane.setSelectedIndex((tabbedPane.getSelectedIndex() + delta + tabCount) % tabCount);
        }
    }
    
    public void setSelectedTabIndex(int index) {
        if (tabbedPane != null) {
            int tabCount = tabbedPane.getTabCount();
            if (index < tabCount) {
                tabbedPane.setSelectedIndex(index);
            }
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
            setVisible(false);
        }
    }
    
    /**
     * Removes the given terminal when there's more than one terminal in the frame.
     */
    private void closeTab(JTerminalPane victim) {
        tabbedPane.remove(victim);
        if (tabbedPane.getTabCount() == 0) {
            // FIXME: how can we ever get here?
            setVisible(false);
        } else {
            // Switch back to a single terminal if appropriate.
            updateTabbedPane();
            // Just hand focus to the visible tab's terminal. We
            // do this later because otherwise Swing seems to give
            // the focus to the tab itself, rather than the
            // component on the tab.
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    if (tabbedPane == null) {
                        return;
                    }
                    Component terminal = tabbedPane.getSelectedComponent();
                    if (terminal == null) {
                        return;
                    }
                    terminal.requestFocus();
                }
            });
        }
    }
    
    public void handleWindowCloseRequestFromUser() {
        // We can't iterate over "terminals" directly because we're causing terminals to close and be removed from the list.
        ArrayList<JTerminalPane> copyOfTerminals = new ArrayList<JTerminalPane>(terminals);
        for (JTerminalPane terminal : copyOfTerminals) {
            if (tabbedPane != null) {
                tabbedPane.setSelectedComponent(terminal);
            }
            if (terminal.doCheckedCloseAction() == false) {
                // If the user hit "Cancel" for one terminal, cancel the close for all other terminals in the same window.
                return;
            }
        }
    }
    
    /**
     * Tidies up after the frame has been hidden.
     * We can't use a ComponentListener because that's invoked on the EDT, as is handleQuit, which relies on us tidying up while it goes.
     */
    @Override
    public void setVisible(boolean newState) {
        super.setVisible(newState);
        if (newState == false) {
            for (JTerminalPane terminal : terminals) {
                terminal.destroyProcess();
            }
            dispose();
            Terminator.getSharedInstance().getFrames().removeFrame(this);
        }
    }
    
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
            tabbedPane.setTitleAt(index, terminal.getTerminalName());
        }
        updateFrameTitle();
    }
    
    public void addTab(JTerminalPane newPane) {
        terminals.add(newPane);
        addPaneToUI(newPane);
        newPane.start(this);
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        updateFrameTitle();
    }
    
    private void addPaneToUI(JTerminalPane newPane) {
        switchToTabbedPane();
        tabbedPane.addTab(newPane.getTerminalName(), newPane);
    }
    
    public void moveCurrentTab(int direction) {
        if (tabbedPane != null) {
            final int currentTabIndex = tabbedPane.getSelectedIndex();
            tabbedPane.moveTab(currentTabIndex, currentTabIndex + direction);
        }
    }
    
    /**
     * Gives the frame the same background color as the terminal to improve appearance during resizes.
     * We don't do this when showing multiple tabs because Mac OS' tabbed pane is partially transparent.
     * Even on GNOME and Windows it would look odd because the tabbed pane is the outermost component, and the new space ought to belong to it, and share its color.
     */
    private void updateBackground() {
        if (GuiUtilities.isMacOs() == false) {
            setBackground(terminals.size() > 1 ? originalBackground : Terminator.getPreferences().getColor(TerminatorPreferences.BACKGROUND_COLOR));
        }
    }
    
    @Override
    public void addNotify() {
        super.addNotify();
        updateTransparency();
    }
    
    private void updateTransparency() {
        GuiUtilities.setFrameAlpha(this, Terminator.getPreferences().getDouble(TerminatorPreferences.ALPHA));
    }
    
    public void optionsDidChange() {
        updateBackground();
        updateTransparency();
        updateMenuBar();
        updateTabbedPane();
        for (JTerminalPane terminal : terminals) {
            terminal.optionsDidChange();
        }
        repaint();
    }
    
    public ArrayList<JTerminalPane> getPanes() {
        return terminals;
    }

    public boolean confirmClose(String processesUsingTty) {
        return SimpleDialog.askQuestion(this, "Close Terminal?", "Closing this terminal may terminate the following processes: " + processesUsingTty, "Close");
    }

    public MenuItemProvider createMenuItemProvider(JTerminalPane terminalPane) {
        return new TerminatorMenuItemProvider(terminalPane);
    }
}
