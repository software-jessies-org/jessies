package e.edit;

import e.ptextarea.*;
import e.util.*;
import java.awt.event.*;
import java.util.*;

/**
 * Checks for "lint" in the selected file by running an appropriate external program.
 * There's no fundamental reason why the user couldn't just add their own external tool definitions, but:
 * 1. things that work out of the box are always preferable, and most users won't know about external tools, and fewer still will define any.
 * 2. most users are likely to use more than one language, and if they need a lint checker for each language, all those tools would be unwieldy (as I found).
 * 3. it's not currently sensible (nor is it likely to become so) to assign the same keyboard equivalent to multiple tools, and suitable keys are in short supply.
 * 4. this lets us add a bit of post-processing, so we can massage tidy(1) output into more useful grep(1)-style output, for example.
 * 5. a built-in lint checking system also lets us provide a fancier interface in future, even though it's currently no better than a user-defined external tool.
 * The major disadvantage (ignoring "more code") is that we're still likely to want some local configuration mechanism (for domain-specific languages, say, or because you prefer an alternative to the tool we suggest, or because you have access to a special tool).
 * 
 * Regarding a possible future "fancier interface", "pyflakes" is a very fast checker for Python, and "ruby -wc" and "tidy -qe" are already very quick.
 * Checking as-you-type sounds eminently possible, even without the ability to use any of these in-process.
 */
public class CheckForLintAction extends ETextAction {
    public CheckForLintAction() {
        super("Check For _Lint", GuiUtilities.makeKeyStroke("L", true));
    }
    
    public void actionPerformed(ActionEvent e) {
        checkForLint();
    }
    
    @Override public boolean isEnabled() {
        final ETextWindow textWindow = getFocusedTextWindow();
        return textWindow != null && getCheckerCommand(textWindow.getFileType()) != null;
    }
    
    private String getCheckerCommand(FileType fileType) {
        return Parameters.getString(fileType.getName() + ".lintChecker", null);
    }
    
    private void checkForLint() {
        // Check which file?
        // We need to get hold of this before we do anything that might lose the focus.
        ETextWindow textWindow = getFocusedTextWindow();
        if (textWindow == null) {
            Evergreen.getInstance().showAlert("Unable to check for lint", "Select a file to be checked.");
            return;
        }
        
        // Get the appropriate command for the selected file's type.
        FileType fileType = textWindow.getFileType();
        String command = getCheckerCommand(fileType);
        if (command == null) {
            Evergreen.getInstance().showAlert("Unable to check for lint", "Don't know how to check " + fileType.getName() + " files.");
            return;
        }
        
        // Are there unsaved files?
        // We don't just check the selected file because it might #include (or whatever) other files.
        Workspace workspace = Evergreen.getInstance().getCurrentWorkspace();
        boolean shouldContinue = workspace.prepareForAction("Save before checking for lint?", "Some files are currently modified but not saved.");
        if (shouldContinue == false) {
            return;
        }
        
        // Append the filename.
        final String filename = textWindow.getFilename();
        // FIXME: shouldn't this actually be using our shell-escaping code?
        command += " \"" + FileUtilities.fileFromString(filename).toString() + "\"";
        
        // Collect stdout and stderr into the same list, so we don't have to worry about which commands output where.
        ArrayList<String> lines = new ArrayList<String>();
        int status = ProcessUtilities.backQuote(null, ProcessUtilities.makeShellCommandArray(command), lines, lines);
        
        if (fileType == FileType.XML) {
            // Reformat the tidy(1) output into the grep(1) style we understand.
            Rewriter rewriter = new Rewriter("^line (\\d+) column (\\d+) - (.)") {
                public String replacement() {
                    return filename + ":" + group(1) + ":" + group(2) + ": " + group(3).toLowerCase();
                }
            };
            for (int i = 0; i < lines.size(); ++i) {
                lines.set(i, rewriter.rewrite(lines.get(i)));
            }
        }
        
        // Show the user what the tool found.
        final EErrorsWindow errorsWindow = workspace.createErrorsWindow("Lint Output");
        errorsWindow.appendLines(true, lines);
        errorsWindow.taskDidExit(status);
    }
}
