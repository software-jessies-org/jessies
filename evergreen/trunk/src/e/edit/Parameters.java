package e.edit;

import e.util.*;
import java.util.Properties;
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
public class Parameters extends Properties {
    
    private Parameters() { /* Not instantiable. */ }
    
    public static void readPropertiesFile(String fileName) {
        File file = FileUtilities.fileFromString(fileName);
        if (file.exists() == false) {
            return;
        }
        Properties props = new Properties(System.getProperties());
        try {
            loadProperties(props, new BufferedReader(new FileReader(file)));
        } catch (Exception ex) {
            Log.warn("Unable to read properties file \"" + fileName + "\"", ex);
        }
        System.setProperties(props);
    }
    
    /** Reads properties into a Properties object from an input stream. */
    public static synchronized void loadProperties(Properties props, BufferedReader inStream) throws IOException {
        try {
            String line;
            while ((line = inStream.readLine()) != null) {
                processPropertiesLine(props, line);
            }
        } finally {
            try {
                inStream.close();
            } catch (IOException ex) {
                /* Ignore. */
                ex = ex;
            }
        }
    }
    
    public static boolean isCommentLine(String line) {
        String trimmedLine = line.trim();
        return trimmedLine.length() == 0 || trimmedLine.charAt(0) == '#';
    }
    
    public static void processIncludeLine(Properties props, String line) {
        String filename = line.trim().substring(1).trim();
        File file = FileUtilities.fileFromString(filename);
        try {
            loadProperties(props, new BufferedReader(new FileReader(file)));
        } catch (Exception ex) {
            Log.warn("Unable to include properties file \"" + filename + "\"", ex);
        }
    }
    
    public static void processPropertiesLine(Properties props, String line) {
        // FIXME: what's the right behavior here? Either we disallow properties
        // like the indent.string property (which will only ever be whitespace)
        // or we insist that you be careful about trailing spaces. For now, I'll
        // go with the latter because I need to get some C++ written... -enh
        //        line = line.trim();
        if (isCommentLine(line)) {
            return;
        }
        
        if (line.charAt(0) == '<') {
            processIncludeLine(props, line);
            return;
        }
        
        if (line.charAt(0) == '?') {
            int end = line.indexOf(':');
            String os = line.substring(1, end);
            if (os.equals(System.getProperty("os.name")) == false) {
                return;
            }
            line = line.substring(end + 1);
        }
        
        /* Assignment. */
        int equalsPos = line.indexOf('=');
        if (equalsPos == -1) {
            Log.warn("line without '=' found in properties file");
            return;
        }
        String name = line.substring(0, equalsPos);
        String value = line.substring(equalsPos + 1);
        //Log.warn("Setting property \"" + name + "\" to \"" + value + "\"");
        props.put(name, value);
    }
    
    public static String getParameter(String name) {
        return System.getProperty(name);
    }
    
    public static String getParameter(String name, String defaultValue) {
        return System.getProperty(name, defaultValue);
    }
    
    public static boolean getParameter(String name, boolean defaultValue) {
        // Yet again, the total lack of thought that went into java.lang.Boolean bites us.
        String result = System.getProperty(name, defaultValue ? "true" : "false");
        return result.equalsIgnoreCase("true");
    }
    
    public static int getParameter(String name, int defaultValue) {
        try {
            String value = getParameter(name);
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
    
    /**
     * Returns an array with an item for each semicolon-separated element of the property.
     */
    public static String[] getArrayOfSemicolonSeparatedElements(String propertyName) {
        String configuration = Parameters.getParameter(propertyName, null);
        if (configuration == null || configuration.trim().length() == 0) {
            return new String[0];
        }
        return configuration.split(";");
    }
}
