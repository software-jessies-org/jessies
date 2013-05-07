package e.util;

import java.util.*;

/**
 * Handles the boilerplate for initializing the GUI on the event dispatch
 * thread. An application wanting to be launched by this method should
 * implement Launchable.
 */
public class Launcher {
    public static void main(String[] arguments) throws Exception {
        if (arguments.length < 2) {
            System.err.println("Usage: java Launcher <human-readable app name> <fully-qualified Launchable class name> ARGUMENTS...");
            System.exit(1);
        }
        //String appName = arguments[0];
        String className = arguments[1];
        Class<?> appClass = Class.forName(className);
        final Launchable app = (Launchable) appClass.newInstance();
        final ArrayList<String> appArguments = new ArrayList<String>();
        for (int i = 2; i < arguments.length; ++i) {
            appArguments.add(arguments[i]);
        }
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                GuiUtilities.initLookAndFeel();
                app.parseCommandLine(appArguments);
                app.startGui();
            }
        });
    }
}
