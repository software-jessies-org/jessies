package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;

public class SaveAsAction extends ETextAction {
    public static final String ACTION_NAME = "Save As...";
    
    public SaveAsAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        String filename = Edit.getCurrentWorkspace().showSaveAsDialog();
        if (filename != null) {
            window.saveAs(filename);
        }
    }
}
