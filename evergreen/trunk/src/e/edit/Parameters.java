package e.edit;

import e.util.*;
import java.util.*;
import java.io.*;

/**
 * The format of a properties file is as follows:
 * 
 * Lines whose first non-space character is # are comments and are ignored.
 * 
 * Lines whose first non-space character is < are include directives. The rest
 * of the line is the name of a file whose contents will be read as if they occurred
 * at this point.
 * 
 * Other lines are considered to be assignments of the form name=value.
 */
public class Parameters {
    private static File file;
    private static FileAlterationMonitor fileAlterationMonitor;
    
    private static HashMap<String, String> map = new HashMap<String, String>();
    private static ArrayList<Preferences.Listener> listeners = new ArrayList<Preferences.Listener>();
    
    private Parameters() { /* Not instantiable. */ }
    
    public static synchronized void initParameters(String filename) {
        file = FileUtilities.fileFromString(filename);
        
        // Arrange to reload the configuration whenever it changes.
        FileAlterationMonitor newFileAlterationMonitor = new FileAlterationMonitor(filename);
        newFileAlterationMonitor.addPathname(filename);
        setFileAlterationMonitor(newFileAlterationMonitor);
        
        // Load the initial configuration.
        reloadParametersFile();
    }
    
    private static void setFileAlterationMonitor(FileAlterationMonitor newFileAlterationMonitor) {
        if (fileAlterationMonitor != null) {
            fileAlterationMonitor.dispose();
        }
        fileAlterationMonitor = newFileAlterationMonitor;
        fileAlterationMonitor.addListener(new FileAlterationMonitor.Listener() {
            public void fileTouched(String pathname) {
                reloadParametersFile();
            }
        });
        
    }
    
    private static synchronized void reloadParametersFile() {
        if (file.exists() == false) {
            return;
        }
        
        try {
            Loader loader = new Loader();
            loader.load(file);
            map = loader.map;
            setFileAlterationMonitor(loader.fileAlterationMonitor);
            Evergreen.getInstance().showStatus("Configuration reloaded");
            firePreferencesChanged();
        } catch (Exception ex) {
            Log.warn("Unable to read properties file \"" + file + "\"", ex);
        }
    }
    
    private static class Loader {
        private FileAlterationMonitor fileAlterationMonitor;
        private HashMap<String, String> map;
        
        private void load(File file) {
            this.fileAlterationMonitor = new FileAlterationMonitor(file.toString());
            this.map = new HashMap<String, String>();
            loadProperties(file);
        }
        
        private void loadProperties(File file) {
            fileAlterationMonitor.addPathname(file.toString());
            for (String line : StringUtilities.readLinesFromFile(file)) {
                processPropertiesLine(line);
            }
        }
        
        private void processPropertiesLine(String line) {
            // We may need an escaping or quoting mechanism if we ever need to preserve trailing spaces.
            line = line.trim();
            
            if (isCommentLine(line)) {
                return;
            }
            
            if (isIncludeLine(line)) {
                processIncludeLine(line);
                return;
            }
            
            int equalsPos = line.indexOf('=');
            if (equalsPos == -1) {
                // TODO: isn't this an error worth reporting?
                Log.warn("line without '=' found in properties file");
                return;
            }
            final String name = line.substring(0, equalsPos);
            final String value = line.substring(equalsPos + 1);
            map.put(name, value);
        }
        
        private static boolean isCommentLine(String line) {
            return line.length() == 0 || line.charAt(0) == '#';
        }
        
        private static boolean isIncludeLine(String line) {
            return line.charAt(0) == '<';
        }
        
        private void processIncludeLine(String line) {
            final String filename = line.substring(1);
            try {
                loadProperties(FileUtilities.fileFromString(filename));
            } catch (Exception ex) {
                Log.warn("Unable to include properties file \"" + filename + "\"", ex);
            }
        }
    }
    
    public static synchronized String getString(String name, String defaultValue) {
        final String value = map.get(name);
        return (value != null) ? value : defaultValue;
    }
    
    public static synchronized boolean getBoolean(String name, boolean defaultValue) {
        // Yet again, the total lack of thought that went into java.lang.Boolean bites us.
        final String value = getString(name, defaultValue ? "true" : "false");
        return value.equalsIgnoreCase("true");
    }
    
    public static synchronized int getInteger(String name, int defaultValue) {
        final String value = getString(name, null);
        return (value != null) ? Integer.parseInt(value) : defaultValue;
    }
    
    /**
     * Returns an array with an item for each semicolon-separated element of the property.
     */
    public static synchronized String[] getArrayOfSemicolonSeparatedElements(String name) {
        final String value = getString(name, null);
        if (value == null || value.trim().length() == 0) {
            return new String[0];
        }
        return value.split(";");
    }
    
    public static void addPreferencesListener(Preferences.Listener l) {
        listeners.add(l);
    }
    
    public static void removePreferencesListener(Preferences.Listener l) {
        listeners.remove(l);
    }
    
    private static void firePreferencesChanged() {
        for (Preferences.Listener l : listeners) {
            l.preferencesChanged();
        }
    }
}
