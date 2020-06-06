package e.edit;

import e.ptextarea.*;
import e.util.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Reformats the entire file, using a tool like clang-format.
 */
public class ReformatFileAction extends ETextAction {
    public ReformatFileAction() {
        super("Reformat File", GuiUtilities.makeKeyStroke("R", true));
        //GnomeStockIcon.useStockIcon(this, "gtk-indent");
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow textWindow = getFocusedTextWindow();
        if (textWindow == null) {
            return;
        }
        String error = reformat(textWindow);
        if (error != null) {
            Evergreen.getInstance().showAlert("Reformat File", error);
        }
    }
    
    // TODO: call this from ETextWindow.save if the user's preferences call for reformat on save.
    public static String reformat(ETextWindow textWindow) {
        PTextArea textArea = textWindow.getTextArea();
        
        // TODO: yapf (https://github.com/google/yapf) for PYTHON
        
        final FileType fileType = textArea.getFileType();
        if (fileType == FileType.RUST) {
            return reformatPipe(textArea, "rustfmt");
        } else if (fileType == FileType.GO) {
            return reformatPipe(textArea, "gofmt");
        } else if (fileType != FileType.C_PLUS_PLUS && fileType != FileType.JAVA && fileType != FileType.JAVA_SCRIPT && fileType != FileType.PROTO) {
            return "Don't know how to reformat " + fileType.getName() + " files.";
        }
        
        // TODO: explicitly check for a clang-format binary?
        
        // Check for a .clang-format file in one of the parent directories.
        File f = textWindow.getFile();
        boolean foundClangFormat = false;
        while ((f = f.getParentFile()) != null) {
            if (new File(f, ".clang-format").exists()) {
                foundClangFormat = true;
                break;
            }
        }
        if (!foundClangFormat) {
            return "No .clang-format file found.";
        }
        
        // TODO: if there's a selection, only format those lines?
        // TODO: add the ability to only format lines that have been changed?
        
        int position = textArea.getSelectionStart();
        String input = textArea.getText();
        ArrayList<String> output = new ArrayList<>();
        ArrayList<String> errors = new ArrayList<>();
        int status = ProcessUtilities.backQuote(null, new String[] { "clang-format", "--assume-filename=" + FileUtilities.translateFilenameForShellUse(textWindow.getFilename()), "--fallback-style=Chromium", "--style=file", "--cursor=" + position}, input, output, errors);
        if (!errors.isEmpty()) {
            return "clang-format failed: " + StringUtilities.join(errors, "\n");
        } else if (status != 0) {
            return "clang-format returned " + status + " but no error message.";
        }
        
        String statusLine = output.remove(0);
        String newText = StringUtilities.join(output, "\n") + "\n";
        if (newText.equals(input)) {
            return null;
        }
        
        // The status line is JSON looking something like this:
        // { "Cursor": 2232, "IncompleteFormat": false }
        Matcher m = Pattern.compile(".*\"Cursor\": (\\d+).*").matcher(statusLine);
        if (!m.matches()) {
            return "Unintelligible clang-format JSON response: " + statusLine;
        }
        int newPosition = Integer.parseInt(m.group(1));
        textArea.setText(newText);
        textArea.select(newPosition, newPosition);
        return null;
    }
    
    // reformatPipe pipes the contents of textArea through the formatCommand, and if formatCommand exits
    // successfully replaces the contents of textArea with its stdout.
    // On failure, the textArea's contents is unchanged, and an error message is returned.
    // It is assumed that formatCommand will accept unformatted code on stdin, and produce its formatted
    // output on stdout. This is the case for 'gofmt' and 'rustfmt', which is nice of them.
    public static String reformatPipe(PTextArea textArea, String formatCommand) {
        String input = textArea.getText();
        ArrayList<String> output = new ArrayList<>();
        ArrayList<String> errors = new ArrayList<>();
        int status = ProcessUtilities.backQuote(null, new String[] {formatCommand}, input, output, errors);
        if (!errors.isEmpty()) {
            return formatCommand + " failed: " + StringUtilities.join(errors, "\n");
        } else if (status != 0) {
            return formatCommand + " returned " + status + " but no error message.";
        }
        String newText = StringUtilities.join(output, "\n") + "\n";
        if (newText.equals(input)) {
            return null;
        }
        // We make no attempt to change the caret location to keep it in the same logical location,
        // but we don't have to because PTextArea's "guessTargetCaretPos" takes care of that for us.
        textArea.setText(newText);
        return null;
    }
}
