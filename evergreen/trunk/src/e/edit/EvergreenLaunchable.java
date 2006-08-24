package e.edit;

import e.util.*;
import java.util.*;

public class EvergreenLaunchable implements Launchable {
    private List<String> arguments;
    
    public void parseCommandLine(List<String> arguments) {
        this.arguments = arguments;
    }
    
    public void startGui() {
        // Start up the editor.
        final Evergreen editor = Evergreen.getInstance();
        
        // Open any files given as arguments after everything else has been set up to increase their chances of being on a visible workspace.
        for (final String argument : arguments) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    editor.openFile(argument);
                }
            });
        }
    }
}
