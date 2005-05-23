package e.edit;

import e.util.*;
import java.util.*;

public class EditLaunchable implements Launchable {
    private List<String> arguments;
    
    public void parseCommandLine(List<String> arguments) {
        this.arguments = arguments;
    }
    
    public void startGui() {
        Edit edit = Edit.getInstance();
        for (String argument : arguments) {
            edit.openFile(argument);
        }
    }
}
