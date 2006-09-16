package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import e.gui.*;

public class EWindow extends JComponent {
    private ETitleBar titleBar;
    
    public EWindow(String name) {
        setBackground(Color.WHITE);
        setCursor(Cursor.getDefaultCursor());
        setLayout(new BorderLayout());
        addTitleBar(name);
        setOpaque(true);
    }
    
    public void addTitleBar(String titleText) {
        titleBar = new ETitleBar(titleText, this);
        if (titleText.equals("+Errors") == false) {
            add(titleBar, BorderLayout.NORTH);
        }
    }
    
    public ETitleBar getTitleBar() {
        return titleBar;
    }
    
    public String getTitle() {
        return titleBar.getTitle();
    }

    public void setTitle(String title) {
        titleBar.setTitle(title);
    }
    
    public boolean isDirty() {
        return false;
    }
    
    public ETextArea getTextArea() {
        return null;
    }
    
    /** Closes this window by removing it from its column. */
    public void closeWindow() {
        windowWillClose();
        removeFromColumn();
    }
    
    public void removeFromColumn() {
        boolean mustReassignFocus = getTitleBar().isActive();
        getColumn().removeComponent(this, mustReassignFocus);
    }
    
    /** Invoked when the window is about to be closed. */
    public void windowWillClose() {
    }
    
    public void ensureSufficientlyVisible() {
        if (getHeight() < 2 * titleBar.getHeight()) {
            expand();
        }
    }
    
    public void expand() {
        getColumn().expandComponent(this);
    }
    
    public EColumn getColumn() {
        return (EColumn) SwingUtilities.getAncestorOfClass(EColumn.class, this);
    }
    
    public e.edit.Workspace getWorkspace() {
        return (e.edit.Workspace) SwingUtilities.getAncestorOfClass(e.edit.Workspace.class, this);
    }
}
