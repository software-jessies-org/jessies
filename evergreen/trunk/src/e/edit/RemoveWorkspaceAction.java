package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import e.forms.*;
import e.gui.*;
import e.util.*;

/**
 * FIXME: Should open a dialog where the user can specify which workspace to remove?
 */
public class RemoveWorkspaceAction extends AbstractAction {
    public static final String ACTION_NAME = "Remove Current Workspace";
    
    public RemoveWorkspaceAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        Edit.removeCurrentWorkspace();
    }
}
