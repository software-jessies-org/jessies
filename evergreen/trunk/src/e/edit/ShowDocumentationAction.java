package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * The ETextArea "Show Documentation" action. Asks the registered documentation providers ("researchers") if they have anything of relevance.
 */
public class ShowDocumentationAction extends ETextAction {
    private boolean building = false;
    
    public ShowDocumentationAction() {
        super("Show Documentation for Word");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
    }
    
    public void actionPerformed(ActionEvent e) {
        Advisor.getInstance().showDocumentation();
    }
}
