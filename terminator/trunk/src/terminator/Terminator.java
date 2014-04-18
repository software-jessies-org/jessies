package terminator;

import com.apple.eawt.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import terminator.model.*;
import terminator.view.*;

public class Terminator {
    private static Terminator instance;
    
    private TerminatorPreferences preferences;
    private Color boldForegroundColor;
    
    private Frames frames = new Frames();
    
    public static synchronized Terminator getSharedInstance() {
        if (instance == null) {
            instance = new Terminator();
        }
        return instance;
    }
    
    public static TerminatorPreferences getPreferences() {
        return getSharedInstance().preferences;
    }
    
    private Terminator() {
        initPreferences();
        initAboutBox();
        initMacOsEventHandlers();
    }
    
    private void initPreferences() {
        preferences = new TerminatorPreferences();
        preferences.addPreferencesListener(new Preferences.Listener() {
            public void preferencesChanged() {
                optionsDidChange();
            }
        });
        preferences.readFromDisk();
    }
    
    public Color getBoldColor() {
        return boldForegroundColor;
    }
    
    private void initMacOsEventHandlers() {
        if (GuiUtilities.isMacOs() == false) {
            return;
        }
        
        Application.getApplication().addApplicationListener(new ApplicationAdapter() {
            @Override
            public void handleReOpenApplication(ApplicationEvent e) {
                if (frames.isEmpty()) {
                    openFrame(JTerminalPane.newShell());
                }
                e.setHandled(true);
            }
            
            @Override
            public void handleOpenFile(ApplicationEvent e) {
                SimpleDialog.showAlert(null, "Received 'open file' AppleEvent", e.toString());
                Log.warn("open file " + e.toString());
            }
            
            @Override
            public void handleQuit(ApplicationEvent e) {
                // We can't iterate over "frames" directly because we're causing frames to close and be removed from the list.
                for (TerminatorFrame frame : frames.toArrayList()) {
                    frame.handleWindowCloseRequestFromUser();
                }
                
                // If there are windows still open, the user changed their mind; otherwise quit.
                e.setHandled(frames.isEmpty());
            }
        });
    }
    
    private void initAboutBox() {
        AboutBox aboutBox = AboutBox.getSharedInstance();
        aboutBox.setWebSiteAddress("https://code.google.com/p/jessies/wiki/Terminator");
        aboutBox.addCopyright("Copyright (C) 2004-2014 software.jessies.org team.");
        aboutBox.addCopyright("All Rights Reserved.");
        aboutBox.setLicense(AboutBox.License.GPL_2_OR_LATER);
    }
    
    private void startTerminatorServer() {
        InetAddress loopbackAddress = null;
        try {
            loopbackAddress = InetAddress.getByName(null);
        } catch (UnknownHostException ex) {
            Log.warn("Problem looking up the loopback address", ex);
        }
        new InAppServer("Terminator", System.getProperty("org.jessies.terminator.serverPortFileName"), loopbackAddress, TerminatorServer.class, new TerminatorServer());
    }
    
    public void flushBiggestScrollBuffer() {
        TerminalModel victim = null;
        for (TerminatorFrame frame : getFrames()) {
            for (JTerminalPane pane : frame.getPanes()) {
                TerminalModel candidate = pane.getTerminalView().getModel();
                if (victim == null || candidate.getLineCount() > victim.getLineCount()) {
                    victim = candidate;
                }
            }
        }
        if (victim == null) {
            return;
        }
        victim.flushScrollBuffer();
    }
    
    public void checkMemory() {
        Runtime runtime = Runtime.getRuntime();
        long neverUsedMemory = runtime.maxMemory() - runtime.totalMemory();
        long availableMemory = runtime.freeMemory() + neverUsedMemory;
        // By the time 90% of the memory is gone, we've ground to a halt.
        if (availableMemory > runtime.maxMemory() / 2) {
            return;
        }
        Log.warn("Available memory down to " + availableMemory + ", flushing biggest scroll buffer...");
        flushBiggestScrollBuffer();
        // Turning the scroll buffer to garbage won't necessarily cause it to be collected.
        // If it's not collected, then we'll clear another scroll buffer, needlessly and again ineffectually.
        runtime.gc();
    }
    
    private void startMemoryMonitor() {
        // When streaming blank lines, one check per minute wasn't enough to save us.
        javax.swing.Timer timer = new javax.swing.Timer(5 * 1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                checkMemory();
            }
        });
        timer.setRepeats(true);
        timer.start();
    }
    
    /**
     * Returns whether we did whatever was requested.
     */
    private boolean parseOriginalCommandLine(final List<String> arguments) {
        PrintWriter out = new PrintWriter(System.out);
        PrintWriter err = new PrintWriter(System.err);
        try {
            TerminatorOpener opener = new TerminatorOpener(arguments, err);
            if (opener.showUsageIfRequested(out)) {
                // Exit with success and without starting the UI or the TerminatorServer.
                return true;
            }
            // We're already on the EDT.
            TerminatorFrame window = opener.createUi();
            if (window == null) {
                // Any syntax error will have been reported but we should still exit with failure,
                // but only after our "finally" clause.
                return false;
            }
            startTerminatorServer();
            startMemoryMonitor();
            // We have no need to wait for the window to be closed.
        } finally {
            out.flush();
            err.flush();
            // In the TerminatorServer case, by contrast, the Ruby has to handle this.
            // The existing Terminator won't have the right DESKTOP_STARTUP_ID.
            GuiUtilities.finishGnomeStartup();
        }
        return true;
    }
    
    public Frames getFrames() {
        return frames;
    }
    
    public void openFrame(JTerminalPane terminalPane) {
        new TerminatorFrame(Collections.singletonList(terminalPane));
    }
    
    /**
     * Invoked (via our Preferences.Listener, above) by the preferences dialog whenever an option is changed.
     */
    private void optionsDidChange() {
        boldForegroundColor = Palettes.getBrightColorFor(preferences.getColor(TerminatorPreferences.FOREGROUND_COLOR));
        
        // On the Mac, the Command key (called 'meta' by Java) is always used for keyboard equivalents.
        // On other systems, Control tends to be used, but in the special case of terminal emulators this conflicts with the ability to type control characters.
        // The traditional work-around has always been to use Alt, which -- conveniently for Mac users -- is in the same place on a PC keyboard as Command on a Mac keyboard.
        // Things are complicated if we want to support emacs(1), which uses the alt key as meta.
        // Things are complicated in a different direction if we want to support input methods that use alt.
        // At the moment, we assume that Linux users who want characters not on their keyboard will switch keyboard mapping dynamically (which works fine).
        // We can avoid the question on Mac OS for now because disabling input methods doesn't currently work properly, and we don't get the key events anyway.
        if (GuiUtilities.isMacOs() == false) {
            final boolean useAltAsMeta = preferences.getBoolean(TerminatorPreferences.USE_ALT_AS_META);
            int modifiers = KeyEvent.ALT_MASK;
            if (useAltAsMeta) {
                modifiers = KeyEvent.SHIFT_MASK | KeyEvent.CTRL_MASK;
            }
            TerminatorMenuBar.setDefaultKeyStrokeModifiers(modifiers);
            // When useAltAsMeta is true, we want Alt-F to go to Emacs.
            // When useAltAsMeta is false, we want Alt-F to invoke the Find action.
            // In neither case do we want Alt-F to open the File menu.
            GuiUtilities.setMnemonicsEnabled(false);
        }
        
        for (int i = 0; i < frames.size(); ++i) {
            frames.get(i).optionsDidChange();
        }
    }
    
    public static void main(final String[] argumentArray) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    GuiUtilities.initLookAndFeel();
                    Terminator.getSharedInstance().optionsDidChange();
                    
                    if (Terminator.getSharedInstance().parseOriginalCommandLine(Arrays.asList(argumentArray)) == false) {
                        System.exit(1);
                    }
                } catch (Throwable th) {
                    Log.warn("Couldn't start Terminator.", th);
                    System.exit(1);
                }
            }
        });
    }
}
