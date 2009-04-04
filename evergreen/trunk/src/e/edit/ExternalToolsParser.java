package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.event.*;
import javax.swing.*;

/**

Parses the external tool descriptions in the system properties, invoking
the appropriate abstract method when a tool is parsed. You need to subclass
and implement these methods. Examples are in EvergreenMenuBar (which puts
everything on the Tools menu) and ETextWindow, which puts just the
context-sensitive actions on its popup menu.

 */
public abstract class ExternalToolsParser {
    public abstract void addItem(ExternalToolAction action);
    public abstract void addSeparator();

    public void parse() {
        boolean firstItem = true;
        for (int i = 0; i < 256; i++) {
            String prefix = "tools." + i + ".";

            String name = Parameters.getString(prefix + "name", null);
            if (name == null) {
                // We allow missing entries.
                continue;
            }

            if (name.equals("<separator>")) {
                addSeparator();
                continue;
            }

            String command = Parameters.getString(prefix + "command", null);
            if (command == null) {
                Log.warn("Missing property " + prefix + "command for external tool \"" + name + "\"");
                Log.warn("Perhaps you need to specify a command specific for " + System.getProperty("os.name") + "?");
                continue;
            }
            if (firstItem) {
                addSeparator();
                firstItem = false;
            }
            
            // We accept a shorthand notation for specifying input/output dispositions based on Perl's "open" syntax.
            ToolInputDisposition inputDisposition = ToolInputDisposition.NO_INPUT;
            ToolOutputDisposition outputDisposition = ToolOutputDisposition.ERRORS_WINDOW;
            boolean needsFile = false;
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
            
            ExternalToolAction action = new ExternalToolAction(name, inputDisposition, outputDisposition, command);
            action.setChecksEverythingSaved(Parameters.getBoolean(prefix + "checkEverythingSaved", false));
            action.setNeedsFile(Parameters.getBoolean(prefix + "needsFile", needsFile));
            action.setRequestsConfirmation(Parameters.getBoolean(prefix + "requestConfirmation", false));
            
            configureKeyboardEquivalent(action, prefix);
            configureIcon(action, prefix);
            
            addItem(action);
        }
    }
    
    private void configureKeyboardEquivalent(Action action, String prefix) {
        String keyboardEquivalent = Parameters.getString(prefix + "keyboardEquivalent", null);
        if (keyboardEquivalent != null) {
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
    }
    
    private void configureIcon(Action action, String prefix) {
        // Allow users to specify a GNOME stock icon, and/or the pathname to an icon.
        String stockIcon = Parameters.getString(prefix + "stockIcon", null);
        if (stockIcon != null) {
            GnomeStockIcon.useStockIcon(action, stockIcon);
        } else {
            String icon = Parameters.getString(prefix + "icon", null);
            if (icon != null) {
                // We trust the user to specify an appropriately-sized icon.
                action.putValue(Action.SMALL_ICON, new ImageIcon(icon));
            }
        }
    }
}
