package e.edit;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import e.util.*;

/**
A column containing views.

<font color="red">Note that you have you call addComponent and not add. I don't know why I
didn't override add and invoke super.add at the critical point; this seems like a serious design
error that someone should check out and fix. As things stand, accidentally invoking add will
cause a whole can of Whoop Ass to be spilt in Edit's lap.</font>

@author Elliott Hughes <enh@acm.org>
*/
public class EColumn extends JSplitPane {
    private static final JPanel EMPTY_PANEL = new JPanel();

    private DefaultComboBoxModel windows = new DefaultComboBoxModel();
    private JPanel bottomPanel = new JPanel(new BorderLayout());
    private JComboBox comboBox = new JComboBox(windows);
    private JComponent lastWindow = null;

    public EColumn() {
        super(JSplitPane.VERTICAL_SPLIT, true);
        bottomPanel.add(comboBox, BorderLayout.NORTH);
        setWindow(EMPTY_PANEL);
        setBottomComponent(bottomPanel);
        comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JComponent c = (JComponent) comboBox.getSelectedItem();
                setWindow(c);
                if (c != null) {
                    c.requestFocus();
                }
            }
        });
        comboBox.setMaximumRowCount(20);
        updateComboBoxEnabledState();
    }

    private void setWindow(JComponent c) {
        if (this.lastWindow != null) {
            bottomPanel.remove(this.lastWindow);
        }
        if (c == null) {
            c = EMPTY_PANEL;
        }
        bottomPanel.add(c, BorderLayout.CENTER);
        bottomPanel.invalidate();
        bottomPanel.validate();
        bottomPanel.repaint();
        this.lastWindow = c;
    }
    
    public void setErrorsWindow(EErrorsWindow errorsWindow) {
        setTopComponent(errorsWindow);
    }

    public void switchToNextFile() {
        comboBox.setSelectedIndex((comboBox.getSelectedIndex() + 1) % comboBox.getItemCount());
    }
    
    public void switchToPreviousFile() {
        int index = comboBox.getSelectedIndex() - 1;
        if (index < 0) {
            index = comboBox.getItemCount() - 1;
        }
        comboBox.setSelectedIndex(index);
    }
    
    public void removeComponent(Component c, boolean mustReassignFocus) {
        windows.removeElement(c);
        updateComboBoxEnabledState();
        updateTabForWorkspace();
    }
    
    private void updateComboBoxEnabledState() {
        comboBox.setEnabled(comboBox.getItemCount() > 0);
    }
    
    /**
     * Updates the title of the tab in the JTabbedPane that corresponds to the Workspace that this
     * EColumn represents (if you can follow that). Invoked when the column has a component added
     * or removed.
     */
    public void updateTabForWorkspace() {
        Workspace workspace = getWorkspace();
        JTabbedPane tabbedPane = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, this);
        if (workspace != null && tabbedPane != null) {
            String title = workspace.getTitle();
            int windowCount = getTextWindows().length;
            if (windowCount > 0) {
                title += " (" + windowCount + ")";
            }
            tabbedPane.setTitleAt(tabbedPane.indexOfComponent(workspace), title);
            ensureNonEmptyColumnVisible(tabbedPane);
        }
    }
    
    public void ensureNonEmptyColumnVisible(JTabbedPane tabbedPane) {
        Workspace currentWorkspace = getWorkspace();
        if (currentWorkspace.isEmpty() == false) {
            return;
        }
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Workspace workspace = (Workspace) tabbedPane.getComponentAt(i);
            if (workspace.isEmpty() == false) {
                tabbedPane.setSelectedIndex(i);
            }
        }
    }
    
    private Workspace getWorkspace() {
        return (Workspace) SwingUtilities.getAncestorOfClass(Workspace.class, this);
    }
    
    public void addComponent(Component c) {
        windows.addElement(c);
        windows.setSelectedItem(c);
        updateComboBoxEnabledState();
        updateTabForWorkspace();
    }
    
    /** Moves the given component to the given absolute Y position in the column. */
    public void moveTo(Component c, int y) {
    }

    public void expandComponent(EWindow window) {
    }
 
    public EWindow findWindowByName(String name) {
        name = name.toLowerCase();
        for (int i = 0; i < windows.getSize(); ++i) {
            EWindow window = (EWindow) windows.getElementAt(i);
            if (window.getTitle().toLowerCase().endsWith(name)) {
                return window;
            }
        }
        return null;
    }

    public ETextWindow[] getTextWindows() {
        ETextWindow[] result = new ETextWindow[windows.getSize()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = (ETextWindow) windows.getElementAt(i);
        }
        return result;
    }
}
