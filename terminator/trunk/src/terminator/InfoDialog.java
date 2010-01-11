package terminator;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import terminator.terminal.*;
import terminator.view.*;

public class InfoDialog {
    private static final InfoDialog INSTANCE = new InfoDialog();
    
    private JTextField title;
    private JTextField dimensions;
    private JTextField processes;
    private JTextField logFilename;
    private JTextField ptyFilename;
    private JCheckBox suspendLogging;
    private JTerminalPane terminal;
    
    private InfoDialog() {
        this.title = new JTextField();
        title.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override public void documentChanged() {
                terminal.setName(title.getText());
            }
        });
        this.dimensions = new UneditableTextField();
        this.processes = new UneditableTextField();
        this.logFilename = new UneditableTextField();
        this.ptyFilename = new UneditableTextField();
        this.suspendLogging = makeSuspendLoggingCheckBox();
    }
    
    private JCheckBox makeSuspendLoggingCheckBox() {
        final JCheckBox checkBox = new JCheckBox("Suspend Logging");
        checkBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final TerminalLogWriter terminalLogWriter = terminal.getControl().getTerminalLogWriter();
                terminalLogWriter.suspend(suspendLogging.isSelected());
                // The TerminalLogWriter might not be able to comply, so ensure that
                // the UI reflects the actual state.
                checkBox.setSelected(terminalLogWriter.isSuspended());
            }
        });
        return checkBox;
    }
    
    public static InfoDialog getSharedInstance() {
        return INSTANCE;
    }
    
    public void showInfoDialogFor(final JTerminalPane terminal) {
        updateFieldValuesFor(terminal);
        
        JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, terminal);
        FormBuilder form = new FormBuilder(frame, "Info");
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Title:", title);
        formPanel.addRow("Dimensions:", dimensions);
        formPanel.addRow("Pseudo-Terminal:", ptyFilename);
        formPanel.addRow("Processes:", processes);
        formPanel.addRow("Log Filename:", logFilename);
        if (GuiUtilities.isMacOs() || GuiUtilities.isWindows()) {
            JButton showInFinderButton = new JButton(GuiUtilities.isMacOs() ? "Show in Finder" : "Show in Explorer");
            showInFinderButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    terminal.getControl().getTerminalLogWriter().flush();
                    GuiUtilities.selectFileInFileViewer(logFilename.getText());
                }
            });
            formPanel.addRow("", showInFinderButton);
        }
        formPanel.addRow("", suspendLogging);
        form.getFormDialog().setRememberBounds(false);
        form.showNonModal();
    }
    
    private void updateFieldValuesFor(JTerminalPane terminal) {
        this.terminal = terminal;
        
        title.setText(terminal.getName());
        
        Dimension size = terminal.getTerminalView().getVisibleSizeInCharacters();
        dimensions.setText(size.width + " x " + size.height);
        
        PtyProcess ptyProcess = terminal.getControl().getPtyProcess();
        if (ptyProcess != null) {
            ptyFilename.setText(ptyProcess.getPtyName());
            processes.setText(ptyProcess.listProcessesUsingTty());
        } else {
            ptyFilename.setText("(no pseudo-terminal allocated)");
            processes.setText("");
        }
        
        final TerminalLogWriter terminalLogWriter = terminal.getControl().getTerminalLogWriter();
        logFilename.setText(terminalLogWriter.getInfo());
        suspendLogging.setSelected(terminalLogWriter.isSuspended());
    }
}
