package e.gui;

import e.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A "Help" menu for a GUI application.
 */
public class HelpMenu {
    public HelpMenu() {
    }
    
    public JMenu makeJMenu() {
        JMenu menu = GuiUtilities.makeMenu("Help", 'H');
        
        String webSiteAddress = AboutBox.getSharedInstance().getWebSiteAddress();
        if (webSiteAddress != null) {
            if (Boolean.getBoolean("e.gui.HelpMenu.hasManual")) {
                // FIXME: GNOME usually says "Contents", but also usually implies there's a local copy.
                Action manualAction = new WebLinkAction("View _Manual", "https://code.google.com/p/jessies/source/browse/" + Log.getApplicationName().toLowerCase() + "/trunk/www/manual.html");
                GnomeStockIcon.useStockIcon(manualAction, "gtk-help");
                menu.add(manualAction);
                menu.addSeparator();
            }
            // FIXME: how useful are the non-FAQ links, especially since you can get to them from the FAQ?
            menu.add(new WebLinkAction("View _Web Page", webSiteAddress));
            menu.add(new WebLinkAction("View _Change Log", "https://code.google.com/p/jessies/source/list"));
            menu.add(new WebLinkAction("View _FAQ", webSiteAddress + "FAQ"));
            menu.addSeparator();
        }
        
        menu.add(DebugMenu.makeJMenu());
        
        // "Report a Problem".
        final String supportAddress = System.getProperty("e.gui.HelpMenu.supportAddress");
        if (supportAddress != null) {
            final String subjectLine = AboutBox.getSharedInstance().getProblemReportSubject();
            menu.add(new WebLinkAction("Report a Problem", "mailto:" + supportAddress + "?subject=" + subjectLine));
        }
        
        // "Get Help Online...".
        final String supportSite = System.getProperty("e.gui.HelpMenu.supportSite");
        if (supportSite != null) {
            menu.add(new WebLinkAction("Get Help Online...", supportSite));
        } else {
            menu.add(new PlaceholderAction("Get Help Online..."));
        }
        
        if (GuiUtilities.isMacOs() == false) {
            // GNOME and Windows users expect a link to the application's about box on the help menu.
            menu.addSeparator();
            menu.add(new AboutBoxAction());
        }
        
        return menu;
    }
    
    private static class AboutBoxAction extends AbstractAction {
        private AboutBoxAction() {
            String name = "_About";
            if (GuiUtilities.isWindows()) {
                name += " " + Log.getApplicationName();
            }
            GuiUtilities.configureAction(this, name, null);
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
