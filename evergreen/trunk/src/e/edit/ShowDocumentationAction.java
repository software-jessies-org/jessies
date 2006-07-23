package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * The ETextArea "Show Documentation" action. Asks the registered documentation providers ("researchers") if they have anything of relevance.
 */
public class ShowDocumentationAction extends ETextAction {
    private static final String ACTION_NAME = "Show Documentation";
    
    private boolean building = false;
    
    public ShowDocumentationAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        GnomeStockIcon.useStockIcon(this, "gtk-help");
    }
    
    public void actionPerformed(ActionEvent e) {
        Advisor.getInstance().research(Advisor.getInstance().getLookupString());
    }
}
