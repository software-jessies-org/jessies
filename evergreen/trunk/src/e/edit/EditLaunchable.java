package e.edit;

import e.util.*;
import java.util.*;

public class EditLaunchable implements Launchable {
    private List/*<String>*/ arguments;
    
    public void parseCommandLine(List/*<String>*/ arguments) {
        this.arguments = arguments;
    }
    
    public void startGui() {
        Edit edit = Edit.getInstance();
        for (int i = 0; i < arguments.size(); ++i) {
            edit.openFile((String) arguments.get(i));
        }
    }
}
