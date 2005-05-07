package e.edit;


import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import e.forms.*;
import e.gui.*;
import e.util.*;

public class SimplePatchDialog {
    private static final String PREFIX = "e.edit.SimplePatchDialog-";
    
    private SimplePatchDialog() {
    }
    
    public static JList makePatchView(String fromName, String fromContent, String toName, String toContent) {
        String fromFile = FileUtilities.createTemporaryFile(PREFIX, "file containing " + fromName, fromContent);
        String toFile = FileUtilities.createTemporaryFile(PREFIX, "file containing " + toName, toContent);
        
        ArrayList lines = new ArrayList();
        ArrayList errors = new ArrayList();
        String[] command = new String[] { "diff", "-u", "-b", "-B", "-L", toName, toFile, "-L", fromName, fromFile };
        int status = ProcessUtilities.backQuote(null, command, lines, errors);
        
        if (status == 0) {
            lines.add("(No differences.)");
        }
        
        DefaultListModel model = new DefaultListModel();
        for (int i = 0; i < lines.size(); ++i) {
            model.addElement(lines.get(i));
        }
        
        JList result = new JList();
        result.setCellRenderer(PatchListCellRenderer.INSTANCE);
        result.setFont(ChangeFontAction.getConfiguredFixedFont());
        result.setModel(model);
        return result;
    }
    
    public static void showPatchBetween(String title, String fromName, String fromContent, String toName, String toContent) {
        JList patchView = makePatchView(fromName, fromContent, toName, toContent);
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Differences:", new JScrollPane(patchView));
        FormDialog.showNonModal(Edit.getFrame(), title, formPanel);
    }
}
