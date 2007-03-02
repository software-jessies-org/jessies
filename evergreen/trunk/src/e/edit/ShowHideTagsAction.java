package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class ShowHideTagsAction extends AbstractAction {
    private static final String ACTION_NAME = "Show Tags";
    
    // When we hide the tags panel, the EColumn will get all the space, and it'll keep it when we re-show the tags panel, so we need to remember where the splitter used to be.
    // JSplitPane.getLastDividerLocation won't remember the particular intermediate value we need.
    public static int oldDividerLocation;
    
    // We need to set the divider size to 0 to make it disappear, so we need to remember what size to restore it to.
    // There's no JSplitPane API to restore the divider size to its default.
    private static int oldDividerSize;
    
    public ShowHideTagsAction() {
        super(ACTION_NAME);
        // FIXME: SELECTED_KEY only works on Java 6.
        // Using the string's value means that we can compile on Java 5, but we don't select the check box on Java 5.
        putValue("SwingSelectedKey", Evergreen.getInstance().getTagsPanel().isVisible());
    }
    
    public void actionPerformed(ActionEvent e) {
        setTagsPanelVisibility(Evergreen.getInstance().getTagsPanel().isVisible() == false);
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
    }
}
