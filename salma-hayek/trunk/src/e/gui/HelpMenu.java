package e.gui;

import javax.swing.*;

/**
 * A "Help" menu for a GUI application.
 */
public class HelpMenu {
    private String applicationName;
    private String websiteUrl;
    private String changeLogUrl;
    
    public HelpMenu(String applicationName) {
        this.applicationName = applicationName;
    }
    
    public void setWebsite(String url) {
        this.websiteUrl = url;
    }
    
    public void setChangeLog(String url) {
        this.changeLogUrl = url;
    }
    
    public JMenu makeJMenu() {
        JMenu menu = new JMenu("Help");
        
        // We don't support this yet, because we've got nothing to point it to.
        //menu.add(makeDisabledItem(applicationName + " Help"));
        //menu.addSeparator();
        
        if (websiteUrl != null) {
            menu.add(new WebLinkAction(applicationName + " Website", websiteUrl));
        }
        if (changeLogUrl != null) {
            menu.add(new WebLinkAction(applicationName + " Change Log", changeLogUrl));
        }
        
        // We don't support this yet, because we've got nothing to point it to.
        if (menu.getItemCount() > 0) {
            menu.addSeparator();
        }
        menu.add(makeDisabledItem("View Bugs List"));
        menu.add(makeDisabledItem("Report a Bug"));
        
        // FIXME: GNOME users would expect a link to the application's about box here.
        
        return menu;
    }
    
    private JMenuItem makeDisabledItem(String name) {
        JMenuItem item = new JMenuItem(name);
        item.setEnabled(false);
        return item;
    }
}
