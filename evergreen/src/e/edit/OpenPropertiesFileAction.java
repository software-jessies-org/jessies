package e.edit;

import java.awt.event.*;
import javax.swing.*;

public class OpenPropertiesFileAction extends AbstractAction {
    public OpenPropertiesFileAction() {
        super("Open Properties File", null);
    }
    
    public void actionPerformed(ActionEvent e) {
        final String userPropertiesFilename = Evergreen.getUserPropertiesFilename();
        // FIXME: create an empty file if none exists.
        Evergreen.getInstance().openFile(userPropertiesFilename);
    }
}
