package terminator;

import e.forms.*;
import e.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import terminator.view.*;

public class InfoDialog {
    private static final InfoDialog INSTANCE = new InfoDialog();
    
    private ETextField dimensions;
    private ETextField title;
    private ETextField logFilename;
    private JCheckBox suspendLogging;
    private JTerminalPane terminal;
    
    private InfoDialog() {
        this.dimensions = makeTextField();
        this.title = makeTextField();
        this.logFilename = makeTextField();
        this.suspendLogging = makeSuspendLoggingCheckBox();
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
    
    private JCheckBox makeSuspendLoggingCheckBox() {
        JCheckBox checkBox = new JCheckBox("Suspend Logging");
        checkBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                terminal.getLogWriter().setSuspended(suspendLogging.isSelected());
            }
        });
        return checkBox;
    }
    
    public static InfoDialog getSharedInstance() {
        return INSTANCE;
    }
    
    public void showInfoDialogFor(JTerminalPane terminal) {
        this.terminal = terminal;
        
        title.setText(terminal.getName());
        Dimension size = terminal.getTextPane().getVisibleSizeInCharacters();
        dimensions.setText(size.width + " x " + size.height);
        LogWriter logWriter = terminal.getLogWriter();
        logFilename.setText(logWriter.getFilename());
        suspendLogging.setSelected(logWriter.isSuspended());
        
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Title:", title);
        formPanel.addRow("Dimensions:", dimensions);
        formPanel.addRow("Log Filename:", logFilename);
        formPanel.addRow("", suspendLogging);
        FormDialog.showNonModal(null, "Info", formPanel);
    }
}
