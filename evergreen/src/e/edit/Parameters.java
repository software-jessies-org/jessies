package e.edit;

import e.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import javax.swing.*;

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
    private static final String MONITOR_NAME = "configuration";
    private static ArrayList<Path> files = new ArrayList<>();
    private static FileAlterationMonitor fileAlterationMonitor;
    
    private static HashMap<String, String> map = new HashMap<>();
    private static ArrayList<Preferences.Listener> listeners = new ArrayList<>();
    
    private Parameters() { /* Not instantiable. */ }
    
    public static synchronized void initParameters() {
        files.add(FileUtilities.pathFrom(Evergreen.getResourceFilename("lib", "data", "default.properties")));
        files.add(FileUtilities.pathFrom("/usr/lib/software.jessies.org/evergreen/evergreen.properties"));
        files.add(FileUtilities.pathFrom("/usr/local/software.jessies.org/evergreen/evergreen.properties"));
        files.add(FileUtilities.pathFrom(Evergreen.getUserPropertiesFilename()));
        
        try {
            fileAlterationMonitor = new FileAlterationMonitor(MONITOR_NAME);
            fileAlterationMonitor.addListener(new FileAlterationMonitor.Listener() {
                public void fileTouched(String pathname) {
                    GuiUtilities.invokeLater(() -> {
                        reloadParametersFile();
                    });
                }
            });
        } catch (Exception ex) {
            Log.warn("Unable to start config file monitor", ex);
        }
        // Load the initial configuration.
        reloadParametersFile();
    }
    
    private static synchronized void reloadParametersFile() {
        try {
            Loader loader = new Loader();
            loader.load(files);
            map = loader.map;
            Evergreen.getInstance().showStatus("Configuration reloaded");
            firePreferencesChanged();
        } catch (Exception ex) {
            Log.warn("Unable to reload properties files", ex);
        }
    }
    
    private static class Loader {
        private HashMap<String, String> map = new HashMap<>();
        
        private void load(Iterable<Path> files) {
            for (Path file : files) {
                loadProperties(file);
            }
        }
        
        private void loadProperties(Path file) {
            Path parent = file.getParent();
            if (!Files.exists(parent)) {
                return;
            }
            // The file alteration monitor only monitors directories.
            fileAlterationMonitor.addPath(parent);
            if (!Files.exists(file)) {
                return;
            }
            try (Stream<String> stream = Files.lines(file)) {
                stream.map(v -> v.trim())
                    .filter(v -> !isCommentLine(v))
                    .forEach(v -> processPropertiesLine(v));
            } catch (IOException ex) {
                Log.warn("Failed to read file", ex);
            }
        }
        
        private void processPropertiesLine(String line) {
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
                loadProperties(FileUtilities.pathFrom(filename));
            } catch (Exception ex) {
                Log.warn("Unable to include properties file \"" + filename + "\"", ex);
            }
        }
    }
    
    public static synchronized String getString(String name, String defaultValue) {
        final String value = map.get(name);
        return (value != null) ? value : defaultValue;
    }
    
    /** As getStrings, but trims the prefix from each of the keys in the returned map. */
    public static synchronized Map<String, String> getStringsTrimmed(String prefix) {
        Map<String, String> result = new HashMap<>();
        final int prefixLen = prefix.length();
        for (Map.Entry<String, String> entry : getStrings(prefix).entrySet()) {
            result.put(entry.getKey().substring(prefixLen), entry.getValue());
        }
        return result;
    }

    /**
     * Returns a (copy of a) subset of this map containing only those entries whose keys start with 'prefix'.
     */
    public static synchronized Map<String, String> getStrings(String prefix) {
        final Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
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
     * Returns a list with an item for each semicolon-separated element of the property.
     */
    public static synchronized List<String> getListOfSemicolonSeparatedElements(String name) {
        final String value = getString(name, null);
        if (value == null || value.trim().length() == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split(";"));
    }
    
    public static void addPreferencesListener(Preferences.Listener l) {
        listeners.add(l);
    }
    
    public static void removePreferencesListener(Preferences.Listener l) {
        listeners.remove(l);
    }
    
    private static void firePreferencesChanged() {
        for (Preferences.Listener l : listeners) {
            try {
                l.preferencesChanged();
            } catch (Exception ex) {
                Log.warn("Exception thrown by preferences listener " + l, ex);
            }
        }
    }
}
