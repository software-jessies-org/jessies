package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class EvergreenTabbedPane extends JTabbedPane {
    public EvergreenTabbedPane() {
        super(GuiUtilities.isMacOs() ? JTabbedPane.LEFT : JTabbedPane.TOP);
        
        // We want to provide custom tool tips.
        ToolTipManager.sharedInstance().registerComponent(this);
        
        initPopUpMenu();
        
        addChangeListener(new WorkspaceFocuser());
        ComponentUtilities.disableFocusTraversal(this);
        
        // The tabs themselves (the components with the labels)
        // shouldn't be able to get the focus. If they can, clicking
        // on an already-selected tab takes focus away from the
        // associated content, which is annoying.
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
                
                Workspace workspace = (Workspace) getComponentAt(tabIndex);
                actions.add(new RescanWorkspaceAction(workspace));
                actions.add(null);
                actions.add(new EditWorkspaceAction(workspace));
                actions.add(new RemoveWorkspaceAction(workspace));
            }
        });
    }
    
    @Override
    public String getToolTipTextAt(int index) {
        final int numberKey = (index + 1);
        if (numberKey > 9) {
            // No keyboard I've ever seen has number keys above 9.
            // Extending the current scheme to "0" (and "-" and "="?) is more work than it's probably worth.
            // Apple keyboards have function keys up to F16 if we were to use those instead, but Mac OS uses many of them from F9 up anyway.
            return null;
        }
        String primaryModifier = "Alt+";
        return "<html>Use " + primaryModifier + numberKey + " to switch to this tab.";
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
    
    /**
     * Ensures that when we change tab, we give focus to that workspace.
     */
    private class WorkspaceFocuser implements ChangeListener {
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
