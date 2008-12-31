package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class EvergreenTabbedPane extends TabbedPane {
    public EvergreenTabbedPane() {
        super(GuiUtilities.isMacOs() ? JTabbedPane.LEFT : JTabbedPane.TOP);
        
        initPopUpMenu();
        addChangeListener(new WorkspaceFocuser());
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
                
                Workspace workspace = (Workspace) getComponentAt(tabIndex);
                actions.add(new RescanWorkspaceAction(workspace));
                actions.add(null);
                actions.add(new EditWorkspaceAction(workspace));
                actions.add(new RemoveWorkspaceAction(workspace));
            }
        });
    }
    
    @Override public String getToolTipTextAt(int index) {
        // You always get this bit.
        String normalText = super.getToolTipTextAt(index);
        if (normalText == null) {
            normalText = "";
        }
        
        // Unless you have a ridiculous number of tabs, you'll get this bit.
        String switchMessage = "";
        String key = tabIndexToKey(index);
        if (key != null) {
            String primaryModifier = GuiUtilities.isMacOs() ? "\u2318" : "Alt+";
            switchMessage = "Use " + primaryModifier + key + " to switch to this tab.";
        }
        
        return "<html><body>" + normalText + "<p>" + switchMessage;
    }
    
    /**
     * Ensures that when we change tab, we give focus to that workspace.
     */
    private static class WorkspaceFocuser implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    Evergreen.getInstance().getTagsPanel().ensureTagsAreHidden();
                    Evergreen.getInstance().getCurrentWorkspace().restoreFocusToRememberedTextWindow();
                }
            });
        }
    }
}
