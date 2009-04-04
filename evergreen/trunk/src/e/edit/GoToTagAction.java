package e.edit;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;

public class GoToTagAction extends ETextAction {
    public GoToTagAction() {
        // We say "tag" to be honest: we really don't know what we're going to.
        // It could be a definition, it could be a declaration, it could be an implementation (think of something like "actionPerformed"), and isn't even necessarily the right kind of thing (we might take you to a global variable with the same name as a global function, say).
        // If you don't know what a 'tag' is, you probably don't have a "tags" file lying around to use this functionality anyway.
        //
        // We use control-t even though we're roughly equivalent to Vim's control-] because:
        // (a) 't' for 'tag' is easier to remember
        // (b) using ] or } causes much pain for users with international keyboards (Danish in particular).
        super("Go to _Tag", GuiUtilities.makeKeyStroke("T", false));
    }
    
    public void actionPerformed(ActionEvent e) {
        goToTag();
    }
    
    private void goToTag() {
        // What are we looking for?
        String tagName = getSearchTerm();
        if (tagName.length() == 0) {
            Evergreen.getInstance().showAlert("Unable to go to tag", "Select an identifier.");
            return;
        }
        
        final String workspaceRoot = Evergreen.getInstance().getCurrentWorkspace().getRootDirectory();
        
        // Call our helper script/binary to find the tags for us.
        final String defaultFindTagsScript = Evergreen.getScriptFilename("find-tags.rb");
        // FIXME: document this preference after it's been successfully used at least once.
        // FIXME: now we're assuming the tool is a Ruby script, which is a step backwards.
        final String findTagsScript = Parameters.getString("tags.findTagsTool", defaultFindTagsScript);
        // FIXME: we could usefully check for a "tags" file, and maybe even offer to generate a usable one, but we'd need some kind of override in case a custom tool doesn't use a file.
        // FIXME: if Evergreen knew how to regenerate the tags, we could perhaps link it to "rescan".
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> errors = new ArrayList<String>();
        int status = ProcessUtilities.backQuote(FileUtilities.fileFromString(workspaceRoot), new String[] { "ruby", findTagsScript, tagName }, lines, errors);
        if (status == 1 || errors.size() > 0) {
            Evergreen.getInstance().showAlert("Unable to go to tag", findTagsScript + " failed. Error output:\n" + StringUtilities.join(errors, "\n"));
            return;
        }
        
        // Pull the addresses out of the matches.
        // We assume the output is in a form similar to http://code.google.com/p/google-gtags/ so people can use that or the default script we supply.
        ArrayList<String> addresses = new ArrayList<String>();
        final Pattern pattern = Pattern.compile("^([^\t]+:\\d+)");
        for (String line: lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                addresses.add(matcher.group(1) + ":");
            }
        }
        
        if (addresses.size() == 0) {
            Evergreen.getInstance().showAlert("Unable to go to tag", "No definition found for \"" + tagName + "\".");
        } else if (addresses.size() == 1) {
            Evergreen.getInstance().openFile(workspaceRoot + File.separator + addresses.get(0));
        } else {
            final JList list = new JList(addresses.toArray(new String[addresses.size()]));
            list.setCellRenderer(new EListCellRenderer(true));
            ComponentUtilities.bindDoubleClickAndEnter(list, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    final String address = (String) list.getSelectedValue();
                    Evergreen.getInstance().openFile(workspaceRoot + File.separator + address);
                }
            });
            
            FormBuilder form = new FormBuilder(Evergreen.getInstance().getFrame(), "Tags Matching \"" + tagName + "\"");
            form.getFormPanel().addRow("Tags:", new JScrollPane(list));
            form.showNonModal();
        }
    }
}
