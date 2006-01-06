package e.edit;

import java.util.*;

/**
 * Used by the FindInFilesDialog to know when results are potentially out of
 * date.
 */
public class SaveMonitor {
    private static final SaveMonitor INSTANCE = new SaveMonitor();
    
    public static SaveMonitor getInstance() {
        return INSTANCE;
    }
    
    private SaveMonitor() {
    }
    
    private ArrayList<SaveMonitor.Listener> saveListeners = new ArrayList<SaveMonitor.Listener>();
    
    /**
     * Adds a listener to be notified when any file is saved.
     */
    public synchronized void addSaveListener(SaveMonitor.Listener saveListener) {
        saveListeners.add(saveListener);
    }
    
    public synchronized void removeSaveListener(SaveMonitor.Listener saveListener) {
        saveListeners.remove(saveListener);
    }
    
    /**
     * Informs all listeners that a file has been saved.
     */
    public synchronized void fireSaveListeners() {
        for (int i = saveListeners.size() - 1; i >= 0; --i) {
            SaveMonitor.Listener listener = saveListeners.get(i);
            listener.fileSaved();
        }
    }
    
    public interface Listener {
        /**
         * Invoked when a file is saved.
         */
        public void fileSaved();
    }
}
