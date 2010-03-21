package e.edit;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;

public class ShowHideTagsAction extends AbstractAction {
    private static final String SHOW_ACTION_NAME = "Show Symbols Tree";
    private static final String HIDE_ACTION_NAME = "Hide Symbols Tree";
    
    private static JMenuItem menuItem;
    
    // When we hide the tags panel, the EColumn will get all the space, and it'll keep it when we re-show the tags panel, so we need to remember where the splitter used to be.
    // JSplitPane.getLastDividerLocation won't remember the particular intermediate value we need.
    public static int oldDividerLocation;
    
    // We need to set the divider size to 0 to make it disappear, so we need to remember what size to restore it to.
    // There's no JSplitPane API to restore the divider size to its default.
    private static int oldDividerSize;
    
    public static synchronized JMenuItem makeMenuItem() {
        if (menuItem == null) {
            if (GuiUtilities.isMacOs()) {
                menuItem = new JMenuItem(new ShowHideTagsAction());
                updateName(areTagsVisible());
            } else {
                menuItem = new JCheckBoxMenuItem(new ShowHideTagsAction());
                menuItem.setText(SHOW_ACTION_NAME);
            }
        }
        return menuItem;
    }
    
    private ShowHideTagsAction() {
        putValue(SELECTED_KEY, areTagsVisible());
    }
    
    public void actionPerformed(ActionEvent e) {
        setTagsPanelVisibility(areTagsVisible() == false);
    }
    
    public static void setTagsPanelVisibility(boolean visible) {
        TagsPanel tagsPanel = Evergreen.getInstance().getTagsPanel();
        JSplitPane splitPane = Evergreen.getInstance().getSplitPane();
        if (visible) {
            splitPane.setDividerLocation(oldDividerLocation);
            splitPane.setDividerSize(oldDividerSize);
        } else {
            oldDividerLocation = splitPane.getDividerLocation();
            oldDividerSize = splitPane.getDividerSize();
            splitPane.setDividerSize(0);
        }
        tagsPanel.setVisible(visible);
        if (menuItem instanceof JCheckBoxMenuItem == false) {
            updateName(visible);
        }
    }
    
    private static void updateName(boolean visible) {
        menuItem.setText(visible ? HIDE_ACTION_NAME : SHOW_ACTION_NAME);
    }
    
    private static boolean areTagsVisible() {
        return Evergreen.getInstance().getTagsPanel().isVisible();
    }
}
