package e.edit;

import e.forms.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Provides quick access to man pages, without having to resort to opening a temporary terminal.
 */
public class ShowManPageAction extends ETextAction {
    private static final JTextField manPageField = new JTextField(40);
    
    public ShowManPageAction() {
        super("Show Man Page", GuiUtilities.makeKeyStroke("M", true));
    }
    
    public void actionPerformed(ActionEvent e) {
        FormBuilder form = new FormBuilder(Evergreen.getInstance().getFrame(), "Show Man Page");
        form.getFormPanel().addRow("Man Page:", manPageField);
        if (form.show("Show")) {
            // We invokeLater here so that the opening (and transfer of focus to) the documentation window occurs after the closing of the dialog.
            // Otherwise, focus will be returned to the original owner from before the dialog was shown.
            // FIXME: SwingWorker?
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    // FIXME: if the user said "readlink(2)", split off the section and use it.
                    final String manPageName = manPageField.getText();
                    final String content = ManPageResearcher.getSharedInstance().formatManPage(manPageName, "2:3");
                    if (content.length() > 0) {
                        Advisor.getInstance().setDocumentationVisible();
                        Advisor.getInstance().setDocumentationText(content);
                    } else {
                        Evergreen.getInstance().showAlert("Can't show man page", "No man page called \"" + manPageName + "\" found.");
                    }
                }
            });
        }
    }
}
