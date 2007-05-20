package terminator;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import terminator.view.*;

public class TerminatorTabbedPane extends JTabbedPane {
    public TerminatorTabbedPane() {
        // We want to provide custom tool tips.
        ToolTipManager.sharedInstance().registerComponent(this);
        
        initPopUpMenu();
        
        addChangeListener(new TerminalFocuser());
        ComponentUtilities.disableFocusTraversal(this);
        
        // The tabs themselves (the components with the labels)
        // shouldn't be able to get the focus. If they can, clicking
        // on an already-selected tab takes focus away from the
        // associated terminal, which is annoying.
        setFocusable(false);
    }
    
    private void initPopUpMenu() {
        EPopupMenu tabMenu = new EPopupMenu(this);
        tabMenu.addMenuItemProvider(new MenuItemProvider() {
            public void provideMenuItems(MouseEvent e, Collection<Action> actions) {
                // If the user clicked on some part of the tabbed pane that isn't actually a tab, we're not interested.
                int tabIndex = indexAtLocation(e.getX(), e.getY());
                if (tabIndex == -1) {
                    return;
                }
                
                actions.add(new TerminatorMenuBar.NewTabAction());
                actions.add(new TerminatorMenuBar.DetachTabAction());
                actions.add(null);
                actions.add(new TerminatorMenuBar.CloseAction());
            }
        });
    }
    
    public void moveTab(int originalIndex, int newIndex) {
        // Clamp movement to the ends (rather than wrapping round or going out of bounds).
        newIndex = Math.max(0, newIndex);
        newIndex = Math.min(newIndex, getTabCount() - 1);
        if (newIndex == originalIndex) {
            return;
        }
        
        // Remember everything about the original tab.
        final Color background = getBackgroundAt(originalIndex);
        final Component component = getComponentAt(originalIndex);
        final Icon disabledIcon = getDisabledIconAt(originalIndex);
        final int displayedMnemonicIndex = getDisplayedMnemonicIndexAt(originalIndex);
        final boolean enabled = isEnabledAt(originalIndex);
        final Color foreground = getForegroundAt(originalIndex);
        final Icon icon = getIconAt(originalIndex);
        final int mnemonic = getMnemonicAt(originalIndex);
        final String title = getTitleAt(originalIndex);
        final String toolTip = getToolTipTextAt(originalIndex);
        final Component tabComponent = getTabComponentAt_safe(originalIndex);
        
        // Remove the original tab and add a new one.
        remove(originalIndex);
        insertTab(title, icon, component, toolTip, newIndex);
        setSelectedIndex(newIndex);
        
        // Configure the new tab to look like the old one.
        setBackgroundAt(newIndex, background);
        setDisabledIconAt(newIndex, disabledIcon);
        setDisplayedMnemonicIndexAt(newIndex, displayedMnemonicIndex);
        setEnabledAt(newIndex, enabled);
        setForegroundAt(newIndex, foreground);
        setMnemonicAt(newIndex, mnemonic);
        setTabComponentAt_safe(newIndex, tabComponent);
    }
    
    @Override
    public String getToolTipTextAt(int index) {
        String primaryModifier = GuiUtilities.isMacOs() ? "\u2318" : "Alt+";
        String control = GuiUtilities.isMacOs() ? "\u2303" : "Ctrl+";
        return "<html><body>Use " + primaryModifier + (index + 1) + " to switch to this tab.<br>Use " + control + "Tab to cycle through the tabs.";
    }
    
    // Just overriding getToolTipTextAt is insufficient because the default implementation of getToolTipText doesn't call it.
    @Override
    public String getToolTipText(MouseEvent event) {
        int index = indexAtLocation(event.getX(), event.getY());
        if (index != -1) {
            return getToolTipTextAt(index);
        }
        return super.getToolTipText(event);
    }
    
    @Override
    public void addTab(String name, Component c) {
        super.addTab(name, c);
        
        int newIndex = getTabCount() - 1;
        Component tabEar = new TerminatorTabComponent((JTerminalPane) c);
        setTabComponentAt_safe(newIndex, tabEar);
    }
    
    @Override
    public void setTitleAt(int index, String title) {
        TerminatorTabComponent c = (TerminatorTabComponent) getTabComponentAt_safe(index);
        if (c != null) {
            c.setTitle(title);
        }
        super.setTitleAt(index, title);
    }
    
    // We make use of Java 6's custom tab ear components if available, but we still want to work on Java 5 until Java 6 is widespread.
    // FIXME: Java 6 getTabComponentAt
    private Component getTabComponentAt_safe(int index) {
        try {
            return (Component) JTabbedPane.class.getMethod("getTabComponentAt", int.class).invoke(this, index);
        } catch (Exception ex) {
            return null;
        }
    }
    
    // We make use of Java 6's custom tab ear components if available, but we still want to work on Java 5 until Java 6 is widespread.
    // FIXME: Java 6 => setTabComponentAt
    private void setTabComponentAt_safe(int index, Component c) {
        try {
            JTabbedPane.class.getMethod("setTabComponentAt", int.class, Component.class).invoke(this, index, c);
        } catch (Exception ex) {
        }
    }
    
    @Override
    protected void fireStateChanged() {
        super.fireStateChanged();
        updateSpinnerVisibilities();
    }
    
    /**
     * Hides the selected tab's spinner.
     */
    private synchronized void updateSpinnerVisibilities() {
        int index = getSelectedIndex();
        if (index != -1) {
            JTerminalPane visibleTerminal = (JTerminalPane) getComponentAt(index);
            visibleTerminal.getOutputSpinner().setPainted(false);
        }
    }
    
    private static class TerminatorTabComponent extends JPanel {
        private JLabel label;
        
        private TerminatorTabComponent(JTerminalPane terminalPane) {
            super(new BorderLayout());
            this.label = new JLabel(terminalPane.getName());
            
            label.setOpaque(false);
            setOpaque(false);
            
            ((BorderLayout) getLayout()).setHgap(4);
            add(label, BorderLayout.CENTER);
            add(terminalPane.getOutputSpinner(), BorderLayout.EAST);
        }
        
        public void setTitle(String title) {
            label.setText(title);
        }
    }
    
    /**
     * Ensures that when we change tab, we give focus to that terminal.
     */
    private class TerminalFocuser implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            final JTerminalPane selected = (JTerminalPane) getSelectedComponent();
            if (selected != null) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        selected.requestFocus();
                        selected.getTerminatorFrame().updateFrameTitle();
                    }
                });
            }
        }
    }
}
