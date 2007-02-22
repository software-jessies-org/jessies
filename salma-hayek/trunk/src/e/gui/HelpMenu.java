package e.gui;

import e.util.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A "Help" menu for a GUI application.
 */
public class HelpMenu {
    private String applicationName;
    private String changeLogUrl;
    
    public HelpMenu(String applicationName) {
        this.applicationName = applicationName;
    }
    
    public void setChangeLog(String url) {
        this.changeLogUrl = url;
    }
    
    public JMenu makeJMenu() {
        JMenu menu = new JMenu("Help");
        
        // We don't support this yet, because we've got nothing to point it to.
        //menu.add(new PlaceholderAction(applicationName + " Help"));
        //menu.addSeparator();
        
        if (changeLogUrl != null) {
            menu.add(new WebLinkAction("View " + applicationName + " Change Log", changeLogUrl));
            menu.addSeparator();
        }
        
        menu.add(DebugMenu.makeJMenu());
        // FIXME: anyone else using HelpMenu is going to want some control over this. "bk sendbug" and Safari's "Report Bugs to Apple..." are both good role models.
        menu.add(new WebLinkAction("Report a Bug", "mailto:software@jessies.org?subject=" + AboutBox.getSharedInstance().getBugReportSubject()));
        // We don't support this yet, because we've got nothing to point it to.
        menu.add(new PlaceholderAction("View Bugs List"));
        menu.addSeparator();
        
        if (GuiUtilities.isMacOs() == false) {
            // GNOME and Win32 users expect a link to the application's about box on the help menu.
            menu.add(new AboutBoxAction());
        }
        
        return menu;
    }
    
    private class AboutBoxAction extends AbstractAction {
        private AboutBoxAction() {
            String name = "About";
            if (GuiUtilities.isMacOs() == false) {
                name += " " + applicationName;
            }
            putValue(NAME, name);
            GnomeStockIcon.useStockIcon(this, "gtk-about");
        }
        
        public boolean isEnabled() {
            return AboutBox.getSharedInstance().isConfigured();
        }
        
        public void actionPerformed(ActionEvent e) {
            AboutBox.getSharedInstance().show();
        }
    }
}
