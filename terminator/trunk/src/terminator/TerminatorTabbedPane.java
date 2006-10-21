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
    // We make use of Java 6's custom tab ear components if available, but we still want to work on Java 5 until Java 6 is widespread.
    public static boolean haveConfigurableTabs = true;
    
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
        
        if (haveConfigurableTabs == false) {
            return;
        }
        int newIndex = getTabCount() - 1;
        Component tabEar = new TerminatorTabComponent((JTerminalPane) c);
        // FIXME: Java 6 => setTabComponentAt(newIndex, tabEar);
        try {
            JTabbedPane.class.getMethod("setTabComponentAt", int.class, Component.class).invoke(this, newIndex, tabEar);
        } catch (Exception ex) {
            haveConfigurableTabs = false;
        }
    }
    
    @Override
    public void setTitleAt(int index, String title) {
        if (haveConfigurableTabs) {
            try {
                // FIXME: Java 6 => c = (TerminatorTabComponent) getTabComponentAt(index);
                TerminatorTabComponent c = (TerminatorTabComponent) JTabbedPane.class.getMethod("getTabComponentAt", int.class).invoke(this, index);
                c.setTitle(title);
            } catch (Exception ex) {
                haveConfigurableTabs = false;
            }
        }
        super.setTitleAt(index, title);
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
            visibleTerminal.getOutputSpinner().setVisible(false);
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
