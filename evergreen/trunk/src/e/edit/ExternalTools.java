package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

/**
 * Parses the external tool descriptions, found in files under ~/.e.edit.Edit/tools/.
 * See the manual for the format of the files.
 * The files and their directories are automatically monitored for changes, so you must not cache the result of getTools.
 */
public class ExternalTools {
    private static final File TOOLS_DIRECTORY = FileUtilities.fileFromString(Evergreen.getPreferenceFilename("tools"));
    private static final String MONITOR_NAME = "external tools";
    private static FileAlterationMonitor fileAlterationMonitor;
    private static List<ExternalToolAction> tools;
    private static List<Listener> listeners = new ArrayList<Listener>();
    
    public interface Listener {
        public void toolsChanged();
    }
    
    private ExternalTools() { /* Not instantiable. */ }
    
    public static synchronized void initTools() {
        rescanToolConfiguration();
    }
    
    public static void addToolsListener(Listener l) {
        listeners.add(l);
    }
    
    private static void fireToolsChanged() {
        for (Listener l : listeners) {
            l.toolsChanged();
        }
    }
    
    private static void setFileAlterationMonitor(FileAlterationMonitor newFileAlterationMonitor) {
        if (fileAlterationMonitor != null) {
            fileAlterationMonitor.dispose();
        }
        fileAlterationMonitor = newFileAlterationMonitor;
        fileAlterationMonitor.addListener(new FileAlterationMonitor.Listener() {
            public void fileTouched(String pathname) {
                rescanToolConfiguration();
            }
        });
    }
    
    public static List<ExternalToolAction> getTools() {
        return Collections.unmodifiableList(tools);
    }
    
    private static List<ExternalToolAction> rescanToolsDirectory(final FileAlterationMonitor newFileAlterationMonitor) {
        newFileAlterationMonitor.addPathname(TOOLS_DIRECTORY.toString());
        
        if (!TOOLS_DIRECTORY.exists()) {
            return Collections.emptyList();
        }
        
        List<File> toolFiles = new FileFinder().filesUnder(TOOLS_DIRECTORY, new FileFinder.Filter() {
            public boolean acceptFile(File file) {
                newFileAlterationMonitor.addPathname(file.toString());
                return true;
            }
            
            public boolean enterDirectory(File directory) {
                newFileAlterationMonitor.addPathname(directory.toString());
                return true;
            }
        });
        
        List<ExternalToolAction> newTools = new ArrayList<ExternalToolAction>();
        Collections.sort(toolFiles);
        for (File toolFile : toolFiles) {
            try {
                ExternalToolAction tool = parseFile(toolFile);
                if (tool != null) {
                    newTools.add(tool);
                }
            } catch (Exception ex) {
                Log.warn("Problem reading \"" + toolFile + "\"", ex);
            }
        }
        return newTools;
    }
    
    private static void rescanToolConfiguration() {
        final FileAlterationMonitor newFileAlterationMonitor = new FileAlterationMonitor(MONITOR_NAME);
        tools = rescanToolsDirectory(newFileAlterationMonitor);
        // Even if we found no files, a new directory may have been created.
        // The tools might even have disappeared.
        setFileAlterationMonitor(newFileAlterationMonitor);
        Evergreen.getInstance().showStatus("Tools reloaded");
        fireToolsChanged();
    }
    
    private static ExternalToolAction parseFile(File file) {
        String name = null;
        String command = null;
        
        String keyboardEquivalent = null;
        
        String icon = null;
        String stockIcon = null;
        
        boolean checkEverythingSaved = false;
        boolean needsFile = false;
        boolean requestConfirmation = false;
        
        for (String line : StringUtilities.readLinesFromFile(file)) {
            int equalsPos = line.indexOf('=');
            if (equalsPos == -1) {
                // TODO: isn't this an error worth reporting?
                Log.warn("line without '=' found in properties file");
                return null;
            }
            final String key = line.substring(0, equalsPos);
            final String value = line.substring(equalsPos + 1);
            if (key.equals("name")) {
                name = value;
            } else if (key.equals("command")) {
                command = value;
            } else if (key.equals("keyboardEquivalent")) {
                keyboardEquivalent = value;
            } else if (key.equals("icon")) {
                icon = value;
            } else if (key.equals("stockIcon")) {
                stockIcon = value;
            } else if (key.equals("checkEverythingSaved")) {
                checkEverythingSaved = Boolean.valueOf(value);
            } else if (key.equals("needsFile")) {
                needsFile = Boolean.valueOf(value);
            } else if (key.equals("requestConfirmation")) {
                requestConfirmation = Boolean.valueOf(value);
            } else {
                Log.warn("Strange line in tool file \"" + file + "\": " + line);
                return null;
            }
        }
        
        if (name == null) {
            Log.warn("No 'name' line in tool file \"" + file + "\"!");
            return null;
        } else if (command == null) {
            Log.warn("No 'command' line in tool file \"" + file + "\"!");
            return null;
        }
        
        // We accept a shorthand notation for specifying input/output dispositions based on Perl's "open" syntax.
        ToolInputDisposition inputDisposition = ToolInputDisposition.NO_INPUT;
        ToolOutputDisposition outputDisposition = ToolOutputDisposition.ERRORS_WINDOW;
        if (command.startsWith("<")) {
            inputDisposition = ToolInputDisposition.SELECTION_OR_DOCUMENT;
            outputDisposition = ToolOutputDisposition.INSERT;
            needsFile = true;
            command = command.substring(1);
        } else if (command.startsWith(">")) {
            inputDisposition = ToolInputDisposition.SELECTION_OR_DOCUMENT;
            outputDisposition = ToolOutputDisposition.ERRORS_WINDOW;
            needsFile = true;
            command = command.substring(1);
        } else if (command.startsWith("|")) {
            inputDisposition = ToolInputDisposition.SELECTION_OR_DOCUMENT;
            outputDisposition = ToolOutputDisposition.REPLACE;
            needsFile = true;
            command = command.substring(1);
        } else if (command.startsWith("!")) {
            inputDisposition = ToolInputDisposition.NO_INPUT;
            outputDisposition = ToolOutputDisposition.ERRORS_WINDOW;
            needsFile = true;
            command = command.substring(1);
        }
        
        final ExternalToolAction action = new ExternalToolAction(name, inputDisposition, outputDisposition, command);
        action.setCheckEverythingSaved(checkEverythingSaved);
        action.setNeedsFile(needsFile);
        action.setRequestConfirmation(requestConfirmation);
        
        configureKeyboardEquivalent(action, keyboardEquivalent);
        configureIcon(action, stockIcon, icon);
        
        return action;
    }
    
    private static void configureKeyboardEquivalent(Action action, String keyboardEquivalent) {
        if (keyboardEquivalent == null) {
            return;
        }
        
        keyboardEquivalent = keyboardEquivalent.trim().toUpperCase();
        // For now, the heuristic is that special keys (mainly just the function keys) have names and don't want any extra modifiers.
        // Simple letter keys, to keep from colliding with built-in keystrokes, automatically get the platform default modifier plus shift.
        // Neither part of this is really right.
        // For one thing, you might well want to have f1, shift-f1, and so on all do different (though probably related) things.
        // For another, we already use various of the "safe" combinations for built-in functionality; "Find in Files", for example.
        // I can't remember what's wrong with KeyStroke.getKeyStroke(String); I believe the problems were:
        // 1. complex case sensitivity for key names.
        // 2. no support for the idea of a platform-default modifier.
        // 3. users can get themselves into trouble with pressed/released/typed.
        // In the long run, though, we're going to want to implement our own replacement for that, or a pre-processor.
        int modifiers = 0;
        if (keyboardEquivalent.length() == 1) {
            modifiers = GuiUtilities.getDefaultKeyStrokeModifier() | InputEvent.SHIFT_MASK;
        }
        
        KeyStroke keyStroke = GuiUtilities.makeKeyStrokeWithModifiers(modifiers, keyboardEquivalent);
        if (keyStroke != null) {
            action.putValue(Action.ACCELERATOR_KEY, keyStroke);
        }
    }
    
    private static void configureIcon(Action action, String stockIcon, String icon) {
        // Allow users to specify a GNOME stock icon, and/or the pathname to an icon.
        if (stockIcon != null) {
            GnomeStockIcon.useStockIcon(action, stockIcon);
        } else if (icon != null) {
            // We trust the user to specify an appropriately-sized icon.
            action.putValue(Action.SMALL_ICON, new ImageIcon(icon));
        }
    }
}
