package e.edit;

import java.awt.event.*;
import javax.swing.*;

/**
 * Implements "Show Documentation". Asks the registered documentation providers ("researchers") if they have anything of relevance.
 */
public class ShowDocumentationAction extends ETextAction {
    public ShowDocumentationAction() {
        super("Show Documentation for Word", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
    }
    
    public void actionPerformed(ActionEvent e) {
        Advisor.getInstance().showDocumentation();
    }
}
