package org.jessies.os;

public class OS {
    private static final boolean isMacOs;
    private static final boolean isWindows;
    static {
        final String osName = System.getProperty("os.name");
        isMacOs = osName.contains("Mac");
        isWindows = osName.contains("Windows");
    }
    
    public static boolean isMacOs() {
        return isMacOs;
    }
    
    public static boolean isWindows() {
        return isWindows;
    }
}
