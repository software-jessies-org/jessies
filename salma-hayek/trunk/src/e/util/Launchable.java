package e.util;

import java.util.List;

public interface Launchable {
    /**
     * Invoked before the UI is created.
     */
    public void parseCommandLine(List/*<String>*/ arguments);
    
    /**
     * Invoked on the event dispatch thread.
     */
    public void startGui();
}
