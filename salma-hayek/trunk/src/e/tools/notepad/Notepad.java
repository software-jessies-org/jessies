package e.tools.notepad;

import e.util.*;
import java.util.*;

public class Notepad implements Launchable {
    public void parseCommandLine(List<String> arguments) {
    }
    
    public void startGui() {
        NotepadWindow notepadWindow = new NotepadWindow();
        notepadWindow.setVisible(true);
    }
}
