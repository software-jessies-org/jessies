package terminator;

import e.forms.*;
import e.gui.*;
import java.awt.*;
import java.awt.event.*;
import terminator.view.*;

public class InfoDialog {
    private static final InfoDialog INSTANCE = new InfoDialog();
    
    private ETextField dimensions;
    private ETextField title;
    private ETextField logFilename;
    
    private InfoDialog() {
        dimensions = makeTextField();
        title = makeTextField();
        logFilename = makeTextField();
    }
    
    private ETextField makeTextField() {
        ETextField result = new ETextField();
        result.setEditable(false);
        result.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                ((ETextField) e.getSource()).selectAll();
            }
        });
        return result;
    }
    
    public static InfoDialog getSharedInstance() {
        return INSTANCE;
    }
    
    public void showInfoDialogFor(JTerminalPane terminal) {
        title.setText(terminal.getName());
        Dimension size = terminal.getTextPane().getVisibleSizeInCharacters();
        dimensions.setText(size.width + " x " + size.height);
        logFilename.setText(terminal.getLogFilename());
        
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Title:", title);
        formPanel.addRow("Dimensions:", dimensions);
        formPanel.addRow("Log Filename:", logFilename);
        FormDialog.showNonModal(null, "Info", formPanel);
    }
}
