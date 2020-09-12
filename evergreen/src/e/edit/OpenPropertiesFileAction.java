package e.edit;

import java.awt.event.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import e.util.*;

public class OpenPropertiesFileAction extends AbstractAction {
    public OpenPropertiesFileAction() {
        super("Open Properties File", null);
    }
    
    public void actionPerformed(ActionEvent e) {
        final String userPropertiesFilename = Evergreen.getUserPropertiesFilename();
        Path path = Paths.get(userPropertiesFilename);
        if (!Files.exists(path)) {
            ArrayList<String> content = new ArrayList<>();
            content.add("# Evergreen properties file.");
            content.add("# See https://github.com/software-jessies-org/jessies/wiki/EvergreenManual#properties");
            StringUtilities.writeFile(path.toFile(), content);
        }
        Evergreen.getInstance().openFile(userPropertiesFilename);
    }
}
