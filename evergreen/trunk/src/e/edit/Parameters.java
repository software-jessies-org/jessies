package e.edit;

import e.util.*;
import java.util.*;
import java.io.*;

/**
The format of a properties file is as follows:

Lines whose first non-space character is # are comments and are ignored.

Lines whose first non-space character is < are include directives. The rest
of the line is the name of a file whose contents will be read as if they occured
at this point.

Lines whose first non-space character is ? are conditional on the current OS.
The line

    ?Mac OS X:name=value

sets the property 'name' to 'value' only if the current OS is "Mac OS X".

Other lines are considered to be assignments of the form name=value.

*/
public class Parameters {
    private static HashMap<String, String> map = new HashMap<String, String>();
    
    private Parameters() { /* Not instantiable. */ }
    
    public static synchronized void readPropertiesFile(String fileName) {
        File file = FileUtilities.fileFromString(fileName);
        if (file.exists() == false) {
            return;
        }
        
        try {
            map = loadProperties(new HashMap<String, String>(), file);
        } catch (Exception ex) {
            Log.warn("Unable to read properties file \"" + fileName + "\"", ex);
        }
    }
    
    /** Reads properties from the given file. */
    private static HashMap<String, String> loadProperties(HashMap<String, String> props, File file) {
        for (String line : StringUtilities.readLinesFromFile(file)) {
            processPropertiesLine(props, line);
        }
        return props;
    }
    
    private static boolean isCommentLine(String line) {
        return line.length() == 0 || line.charAt(0) == '#';
    }
    
    private static boolean isIncludeLine(String line) {
        return line.charAt(0) == '<';
    }
    
    private static void processIncludeLine(HashMap<String, String> props, String line) {
        final String filename = line.substring(1);
        try {
            loadProperties(props, FileUtilities.fileFromString(filename));
        } catch (Exception ex) {
            Log.warn("Unable to include properties file \"" + filename + "\"", ex);
        }
    }
    
    private static void processPropertiesLine(HashMap<String, String> props, String line) {
        // We may need an escaping or quoting mechanism if we ever need to preserve trailing spaces.
        line = line.trim();
        
        if (isCommentLine(line)) {
            return;
        }
        
        if (isIncludeLine(line)) {
            processIncludeLine(props, line);
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
        props.put(name, value);
    }
    
    public static synchronized String getParameter(String name) {
        return map.get(name);
    }
    
    public static synchronized String getParameter(String name, String defaultValue) {
        final String value = getParameter(name);
        return (value != null) ? value : defaultValue;
    }
    
    public static synchronized boolean getParameter(String name, boolean defaultValue) {
        // Yet again, the total lack of thought that went into java.lang.Boolean bites us.
        final String value = getParameter(name, defaultValue ? "true" : "false");
        return value.equalsIgnoreCase("true");
    }
    
    public static synchronized int getParameter(String name, int defaultValue) {
        final String value = getParameter(name);
        return (value != null) ? Integer.parseInt(value) : defaultValue;
    }
    
    /**
     * Returns an array with an item for each semicolon-separated element of the property.
     */
    public static synchronized String[] getArrayOfSemicolonSeparatedElements(String name) {
        final String value = getParameter(name, null);
        if (value == null || value.trim().length() == 0) {
            return new String[0];
        }
        return value.split(";");
    }
}
