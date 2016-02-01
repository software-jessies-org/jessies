package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import e.util.*;

/**

Parses the external tool descriptions in the system properties, invoking
the appropriate abstract method when a tool is parsed. You need to subclass
and implement these methods. Examples are in EditMenuBar (which puts everything
on the Tools menu) and ETextWindow, which puts just the context-sensitive
actions on its popup menu.

Note that you have to invoke the 'parse' method because typical usage relies
on a final variable in the scope that the anonymous subclass is defined, and
access to such variables doesn't work until after construction is complete,
for reasons I've never really understood. (Both javac and jikes produce code
that behaves like this, though, so I've never doubted that it's correct,
even if it does seem odd.)

 */
public abstract class ExternalToolsParser {
    public abstract void addItem(Action action);
    public abstract void addItem(Action action, char keyEquivalent);
    public abstract void addSeparator();

    public void parse() {
        boolean firstItem = true;
        for (int i = 0; i < 256; i++) {
            String prefix = "tools." + i + ".";

            String name = Parameters.getParameter(prefix + "name");
            if (name == null) {
                /* We allow missing entries. */
                continue;
            }

            if (name.equals("<separator>")) {
                addSeparator();
                continue;
            }

            String command = Parameters.getParameter(prefix + "command");
            if (firstItem) {
                addSeparator();
                firstItem = false;
            }
            
            ExternalToolAction action = new ExternalToolAction(name, command);
            action.setChecksEverythingSaved(Parameters.getParameter(prefix + "checkEverythingSaved", false));
            action.setNeedsFile(Parameters.getParameter(prefix + "needsFile", false));
            action.setRequestsConfirmation(Parameters.getParameter(prefix + "requestConfirmation", false));
            
            String keyboardEquivalent = Parameters.getParameter(prefix + "keyboardEquivalent", null);
            if (keyboardEquivalent != null) {
                char equivalent = keyboardEquivalent.toUpperCase().charAt(0);
                addItem(action, equivalent);
            } else {
                addItem(action);
            }
        }
    }
}
