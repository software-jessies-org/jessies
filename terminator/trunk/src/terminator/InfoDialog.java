package terminator;

import e.forms.*;
import e.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import terminator.view.*;

public class InfoDialog {
    private static final InfoDialog INSTANCE = new InfoDialog();
    
    private ETextField title;
    private ETextField dimensions;
    private ETextField logFilename;
    private ETextField processes;
    private JCheckBox suspendLogging;
    private JTerminalPane terminal;
    
    private InfoDialog() {
        this.title = new TitleField();
        this.dimensions = new UneditableField();
        this.logFilename = new UneditableField();
        this.processes = new UneditableField();
        this.suspendLogging = makeSuspendLoggingCheckBox();
    }
    
    private static class UneditableField extends ETextField {
        public UneditableField() {
            setEditable(false);
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    selectAll();
                }
            });
        }
    }
    
    private class TitleField extends ETextField {
        @Override
        public void textChanged() {
            terminal.setName(title.getText());
        }
    }
    
    private JCheckBox makeSuspendLoggingCheckBox() {
        final JCheckBox checkBox = new JCheckBox("Suspend Logging");
        checkBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                terminal.getLogWriter().setSuspended(suspendLogging.isSelected());
                // The LogWriter might not be able to comply, so ensure that
                // the UI reflects the actual state.
                checkBox.setSelected(terminal.getLogWriter().isSuspended());
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
        logFilename.setText(logWriter.getInfo());
        suspendLogging.setSelected(logWriter.isSuspended());
        processes.setText(terminal.getControl().getPtyProcess().listProcessesUsingTty());
        
        JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, terminal);
        
        FormBuilder form = new FormBuilder(frame, "Info");
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Title:", title);
        formPanel.addRow("Dimensions:", dimensions);
        formPanel.addRow("Log Filename:", logFilename);
        formPanel.addRow("", suspendLogging);
        formPanel.addRow("Processes:", processes);
        form.getFormDialog().setRememberBounds(false);
        form.showNonModal();
    }
}
