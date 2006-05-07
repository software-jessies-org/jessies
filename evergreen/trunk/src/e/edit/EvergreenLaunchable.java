package e.edit;

import e.util.*;
import java.util.*;

public class EvergreenLaunchable implements Launchable {
    private List<String> arguments;
    
    public void parseCommandLine(List<String> arguments) {
        this.arguments = arguments;
    }
    
    public void startGui() {
        Evergreen editor = Evergreen.getInstance();
        for (String argument : arguments) {
            editor.openFile(argument);
        }
    }
}
