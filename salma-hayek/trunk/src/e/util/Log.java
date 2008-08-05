package e.util;

import java.io.*;
import java.lang.reflect.*;
import java.util.regex.*;

public class Log {
    private static final Pattern CYGWIN_VERSION_PATTERN = Pattern.compile("CYGWIN\\S* ([0-9.]+).*");
    
    /**
     * java.awt.EventDispatchThread checks this property before using its
     * default exception-reporting code; if you set it to the name of a class,
     * you can arrange for your code to be invoked instead.
     */
    private static final String HANDLER_PROPERTY = "sun.awt.exception.handler";
    static {
        System.setProperty(HANDLER_PROPERTY, "e.util.Log$AwtExceptionHandler");
    }
    
    /**
     * java.awt.EventDispatchThread needs a zero-argument constructor, so this
     * has to be a static nested class.
     */
    public static class AwtExceptionHandler {
        /**
         * Forwards the caught exception to our usual logging code.
         */
        public void handle(Throwable th) {
            Log.warn("Exception occurred during event dispatching.", th);
        }
    }
    
    private static String applicationName;
    static {
        applicationName = System.getProperty("e.util.Log.applicationName");
        if (applicationName == null) {
            applicationName = "unknown";
        }
    }
    
    private static PrintWriter out = new PrintWriter(System.err, true);
    static {
        String logFilename = System.getProperty("e.util.Log.filename");
        try {
            if (logFilename != null) {
                // Append to, rather than truncate, the log.
                FileOutputStream fileOutputStream = new FileOutputStream(logFilename, true);
                // Use the UTF-8 character encoding.
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
                // Auto-flush when println is called.
                out = new PrintWriter(outputStreamWriter, true);
            }
        } catch (Throwable th) {
            Log.warn("Couldn't redirect logging to \"" + logFilename + "\"", th);
        }
    }
    
    static {
        warn(getJavaVersion());
        warn(getOsVersion());
        String launcherOsVersion = System.getProperty("e.util.Log.launcherOsVersion");
        if (launcherOsVersion != null) {
            warn(launcherOsVersion);
        }
    }
    
    public static String getApplicationName() {
        return applicationName;
    }
    
    public static void setApplicationName(String name) {
        applicationName = name;
    }

    private static String getJavaVersion() {
        String vmVersion = System.getProperty("java.vm.version");
        String runtimeVersion = System.getProperty("java.runtime.version");
        String fullVersion = vmVersion.equals(runtimeVersion) ? vmVersion : ("VM " + vmVersion + ", runtime " + runtimeVersion);
        return "Java " + System.getProperty("java.version") + " (" + fullVersion + ")";
    }

    private static String getOsVersion() {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String launcherOsVersion = System.getProperty("e.util.Log.launcherOsVersion");
        if (launcherOsVersion != null) {
            Matcher cygwinVersionMatcher = CYGWIN_VERSION_PATTERN.matcher(launcherOsVersion);
            if (cygwinVersionMatcher.matches()) {
                osVersion = osVersion + " Cygwin " + cygwinVersionMatcher.group(1);
            }
        }
        String osArch = System.getProperty("os.arch");
        final int processorCount = Runtime.getRuntime().availableProcessors();
        return osName + " " + osVersion + "/" + osArch + " x" + processorCount;
    }
    
    public static String getSystemDetailsForProblemReport() {
        String systemDetails = System.getProperty("java.vm.version") + "/" + getOsVersion();
        return systemDetails;
    }
    
    public static void warn(String message) {
        warn(message, null);
    }

    public static void warn(String message, Throwable th) {
        out.println(TimeUtilities.currentIsoString() + " " + applicationName + ": " + message);
        if (th != null) {
            out.println("Associated exception:");
            th.printStackTrace(out);
        }
    }
    
    public static String inspect(Object o) {
        Class<?> c = o.getClass();
        
        if (c == String.class) {
            return "\"" + o.toString() + "\"";
        }
        
        if (c.isArray()) {
            return inspectArray(o);
        } else if (c.isPrimitive()) {
            return "" + o;
        }
        
        StringBuilder result = new StringBuilder(c.getName());
        result.append('[');
        
        Field[] fields = c.getDeclaredFields();
        boolean needComma = false;
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (needComma) {
                result.append(',');
            }
            result.append(field.getName());
            result.append('=');
            try {
                result.append(inspect(field.get(o)));
            } catch (Throwable th) {
                result.append("?");//th.getMessage());
            }
            needComma = true;
        }
        
        result.append(']');
        return result.toString();
    }
    
    public static String inspectArray(Object o) {
        Class<?> c = o.getClass();
        Class<?> itemClass = c.getComponentType();
        StringBuilder result = new StringBuilder(itemClass.getName());
        result.append("[] = {");
        if (itemClass.isPrimitive()) {
            result.append("primitives!");
        } else {
            Object[] array = (Object[]) o;
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    result.append(',');
                }
                result.append(inspect(array[i]));
            }
        }
        result.append('}');
        return result.toString();
    }
    
    public static void main(String[] args) {
        int[] numbers = new int[] { 1, 4, 173 };
        System.out.println(inspect(Boolean.TRUE));
        System.out.println(inspect(java.awt.Color.RED));
        System.out.println(inspect(args));
        System.out.println(inspect(numbers));
    }

    /** Protects against instantiation. */
    private Log() {
    }
}
