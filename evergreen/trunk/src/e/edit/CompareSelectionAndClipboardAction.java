package e.edit;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import e.forms.*;
import e.gui.*;
import e.util.*;

/**
 * Shows a patch to turn the clipboard into the selection. Useful for confirming
 * that you're looking at copy & paste code before you remove it.
 * 
 * Obvious extensions:
 * 
 * 1. We don't know much about the data on the clipboard, but we know a lot
 *    about the selection; we could use that to allow double-clicking to take
 *    you to the line in question.
 * 
 * 2. Suppose you're comparing code where someone's changed an identifier or
 *    a magic number, and there are bogus differences where all you need is a
 *    parameter. Would something like replaceAll("\b(\\d+|0x[0-9a-fA-F]+)\b",
 *    "123") to replace numeric literals with one constant -- and similar for
 *    string literals and identifiers -- be helpful?
 */
public class CompareSelectionAndClipboardAction extends ETextAction {
    public static final String ACTION_NAME = "Compare Selection and Clipboard...";
    
    public CompareSelectionAndClipboardAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        if (window == null) {
            return;
        }
        
        ETextArea text = window.getText();
        String selection = text.getSelectedText();
        String clipboard = getClipboardText();
        
        // Avoid diff(1) warnings about "No newline at end of file".
        selection += "\n";
        clipboard += "\n";
        
        SimplePatchDialog.showPatchBetween("Selection/Clipboard Comparison", "selection", selection, "clipboard", clipboard);
    }
    
    private String getClipboardText() {
        String result = "";
        try {
            Clipboard selection = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable transferable = selection.getContents(null);
            result = (String) transferable.getTransferData(DataFlavor.stringFlavor);
        } catch (Exception ex) {
            Log.warn("Couldn't get clipboard contents.", ex);
        }
        return result;
    }
}
