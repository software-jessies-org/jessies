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
        JMenu menu = new JMenu("Help");
        
        String webSiteAddress = AboutBox.getSharedInstance().getWebSiteAddress();
        if (webSiteAddress != null) {
            if (Boolean.getBoolean("e.gui.HelpMenu.hasManual")) {
                // FIXME: GNOME usually says "Contents", but also usually implies there's a local copy.
                Action manualAction = new WebLinkAction("View " + Log.getApplicationName() + " _Manual", webSiteAddress + "manual.html");
                GnomeStockIcon.useStockIcon(manualAction, "gtk-help");
                menu.add(manualAction);
                menu.addSeparator();
            }
            // FIXME: how useful are the non-FAQ links, especially since you can get to them from the FAQ?
            menu.add(new WebLinkAction("View " + Log.getApplicationName() + " _Web Page", webSiteAddress));
            menu.add(new WebLinkAction("View " + Log.getApplicationName() + " _Change Log", webSiteAddress + "ChangeLog.html"));
            menu.add(new WebLinkAction("View " + Log.getApplicationName() + " _FAQ", webSiteAddress + "faq.html"));
            menu.addSeparator();
        }
        
        menu.add(DebugMenu.makeJMenu());
        // FIXME: anyone else using HelpMenu is going to want some control over this; add a system property, and set a default in invoke-java?
        menu.add(new WebLinkAction("Report a Problem", "mailto:software@jessies.org?subject=" + AboutBox.getSharedInstance().getProblemReportSubject()));
        // We don't support this yet, because we've got nothing to point it to.
        menu.add(new PlaceholderAction("View Bugs List"));
        
        if (GuiUtilities.isMacOs() == false) {
            // GNOME and Win32 users expect a link to the application's about box on the help menu.
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
