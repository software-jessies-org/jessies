package e.util;

import java.io.*;
import java.util.regex.*;

public class Log {
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
    
    private static String applicationName = System.getProperty("e.util.Log.applicationName", "unknown");
    
    private static LogWriter out = new DefaultLogWriter(applicationName);
    static {
        final String logWriterClassName = System.getProperty("e.util.Log.logWriter");
        try {
            if (logWriterClassName != null) {
                out = (LogWriter) Class.forName(logWriterClassName).newInstance();
            }
        }
        catch (Exception ex) {
            warn("Error configuring log writer \"" + logWriterClassName + "\".", ex);
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
        //launcherOsVersion = "CYGWIN_NT-5.0 1.5.25(0.156/4/2) 2008-06-12 19:34";        
        if (launcherOsVersion != null) {
            Matcher matcher = Pattern.compile("^CYGWIN\\S* ([0-9.]+)").matcher(launcherOsVersion);
            if (matcher.find()) {
                osVersion = osVersion + " Cygwin " + matcher.group(1);
            }
        }
        String osArch = System.getProperty("os.arch");
        final int processorCount = Runtime.getRuntime().availableProcessors();
        return osName + " " + osVersion + "/" + osArch + " x" + processorCount;
    }
    
    public static String getSystemDetailsForProblemReport() {
        String systemDetails = System.getProperty("java.runtime.version") + "/" + getOsVersion();
        return systemDetails;
    }
    
    public static void warn(String message) {
        warn(message, null);
    }

    public static void warn(String message, Throwable th) {
        out.log(message, th);
    }
    
    /** Protects against instantiation. */
    private Log() {
    }
}
