package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import e.gui.*;

public class EWindow extends JComponent implements Comparable {
    private ETitleBar titleBar;
    
    private EPopupMenu popupMenu;
    private Component componentWithMenu;
    
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

    public String toString() {
        return getTitle();
    }
    
    /**
     * Implements the Comparable interface so windows can be sorted
     * into alphabetical order by title.
     */
    public int compareTo(Object other) {
        return getTitle().compareTo(((EWindow) other).getTitle());
    }
    
    public void setTitle(String title) {
        titleBar.setTitle(title);
    }
    
    public boolean isDirty() {
        return false;
    }
    
    public ETextArea getText() {
        return null;
    }
    
    /** Closes this window by removing it from its column. */
    public void closeWindow() {
        windowClosing();
        removeFromColumn();
    }
    
    public void removeFromColumn() {
        boolean mustReassignFocus = getTitleBar().isActive();
        getColumn().removeComponent(this, mustReassignFocus);
    }
    
    /** Invoked when the window is about to be closed. */
    public void windowClosing() { }
    
    public void expand() {
        getColumn().expandComponent(this);
    }
    
    public EColumn getColumn() {
        return (EColumn) SwingUtilities.getAncestorOfClass(EColumn.class, this);
    }
    
    public Collection getPopupMenuItems() {
        return null;
    }

    public void showPopupMenu(MouseEvent e) {
        Collection items = getPopupMenuItems();
        if (items != null && items.size() > 0) {
            popupMenu = new EPopupMenu();
            popupMenu.add((Action[]) items.toArray(new Action[items.size()]));
            popupMenu.show((Component) e.getSource(), e.getX(), e.getY());
        }
    }
    
    public void attachPopupMenuTo(Component c) {
        componentWithMenu = c;
        componentWithMenu.addMouseListener(new MenuMouseListener());
    }
    
    public e.edit.Workspace getWorkspace() {
        return (e.edit.Workspace) SwingUtilities.getAncestorOfClass(e.edit.Workspace.class, this);
    }
    
    public class MenuMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            handle(e);
        }
        
        public void mouseReleased(MouseEvent e) {
            handle(e);
        }
        
        public void handle(MouseEvent e) {
            if (e.isConsumed() == false && e.isPopupTrigger()) {
                requestFocusOnComponentWithMenu();
                showPopupMenuLater(e);
            }
        }
        
        public void requestFocusOnComponentWithMenu() {
            componentWithMenu.requestFocus();
        }
        
        public void showPopupMenuLater(final MouseEvent e) {
            try {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        showPopupMenu(e);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                /* Ignore: exceptions here are unfortunate but not fatal. */
            }
        }
    }
}
