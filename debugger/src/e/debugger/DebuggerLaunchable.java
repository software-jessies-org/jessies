package e.debugger;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

import e.util.*;

public class DebuggerLaunchable implements Launchable {
    
    public void parseCommandLine(List<String> arguments) {
        // FIXME
        System.setProperty("sourcepath", "/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home/src/;~/Projects/debugger/src/;/usr/java/jdk/src/");
        System.setProperty("classpath", FileUtilities.parseUserFriendlyName("~/Projects/salma-hayek/classes/") + File.pathSeparator + FileUtilities.parseUserFriendlyName("~/Projects/debugger/classes/"));
        System.setProperty("main_class", "e.debugger.TestProgram");
    }
    
    public void startGui() {
        JFrame f = new JFrame("Debugger");
        f.setSize(new Dimension(550, 600));
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setContentPane(new Debugger());
        f.setVisible(true);
    }
    
}
