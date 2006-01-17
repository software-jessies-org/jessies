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
    private ETextField processes;
    private ETextField logFilename;
    private JCheckBox suspendLogging;
    private JTerminalPane terminal;
    
    private InfoDialog() {
        this.title = new TitleField();
        this.dimensions = new UneditableField();
        this.processes = new UneditableField();
        this.logFilename = new UneditableField();
        this.suspendLogging = makeSuspendLoggingCheckBox();
    }
    
    private static class UneditableField extends ETextField {
        public UneditableField() {
            // Text fields with setEditable(false) don't look very different on any platform but Win32.
            // Win32 is the only platform that clearly distinguishes between all the combinations of editable and enabled.
            // It's sadly unclear that those responsible for the other platforms even understand the distinction.
            // Although Cocoa makes a overly-subtle visual distinction, Apple's Java doesn't reproduce it.
            // As a work-around, we use a trick various Mac OS programs use: make the uneditable text fields look like labels.
            // You lose the visual clue that you can select and copy the text, but that's less important than obscuring the visual clue on editable fields that they're editable.
            // FIXME: a text area would retain the selection behavior but add wrapping.
            setBorder(null);
            setOpaque(false);
            setEditable(false);
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
        formPanel.addRow("Processes:", processes);
        formPanel.addRow("Log Filename:", logFilename);
        formPanel.addRow("", suspendLogging);
        form.getFormDialog().setRememberBounds(false);
        form.showNonModal();
    }
}
