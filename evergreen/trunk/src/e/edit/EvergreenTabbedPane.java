package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class EvergreenTabbedPane extends TabbedPane {
    public EvergreenTabbedPane() {
        super(GuiUtilities.isMacOs() ? JTabbedPane.LEFT : JTabbedPane.TOP);
        addChangeListener(new WorkspaceFocuser());
    }
    
    @Override protected void provideMenuItems(int index, Collection<Action> actions) {
        final Workspace workspace = (Workspace) getComponentAt(index);
        actions.add(new RescanWorkspaceAction(workspace));
        actions.add(null);
        actions.add(new EditWorkspaceAction(workspace));
        actions.add(new RemoveWorkspaceAction(workspace));
    }
    
    @Override public String getToolTipTextAt(int index) {
        // You always get this bit.
        final Workspace workspace = (Workspace) getComponentAt(index);
        final File rootDirectory = FileUtilities.fileFromString(workspace.getRootDirectory());
        String normalText = FileUtilities.getUserFriendlyName(rootDirectory);
        if (rootDirectory.exists() == false) {
            normalText += "<p><font color='red'>(This directory doesn't exist.)</font>";
        } else if (rootDirectory.isDirectory() == false) {
            normalText += "<p><font color='red'>(This isn't a directory.)</font>";
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
