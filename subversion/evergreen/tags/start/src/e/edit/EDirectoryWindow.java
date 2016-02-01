package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import e.gui.*;
import e.util.*;

public class EDirectoryWindow extends EWindow {
    private JList list;
    private DefaultListModel listModel;
    private File file;
    
    public EDirectoryWindow(String filename) {
        super(filename);
        file = FileUtilities.fileFromString(filename);
        listModel = new DefaultListModel();
        refreshContents();
        list = new JList(listModel);
        list.addFocusListener(this);
        list.setFont(getConfiguredFont());
        JScrollPane scrollPane = new JScrollPane(list);
        list.addMouseListener(new DoubleClickListener());
        //list.setCellRenderer(new DirectoryListCellRenderer());
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public void requestFocus() {
        list.requestFocus();
    }
    
    public Font getConfiguredFont() {
        String fontName = Parameters.getParameter("font.name", "verdana");
        int fontSize = Parameters.getParameter("font.size", 12);
        return new Font(fontName, Font.PLAIN, fontSize);
    }
    
    public class DoubleClickListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopupMenu(e);
            }
        }
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopupMenu(e);
            }
        }
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int index = list.locationToIndex(e.getPoint());
                String filename = (String) listModel.get(index);
                Edit.openFile(getContext() + File.separator + filename);
            }
        }
    }
    
    /** Directory windows contain lists of files. */
    public void refreshContents() {
        listModel.removeAllElements();
        File[] files = file.listFiles(); /* 'file' is a directory in our case. */
        if (files == null) {
            listModel.addElement("<directory does not exist>");
            return;
        }
        TreeSet directoriesTree = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        TreeSet filesTree = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        if (isSomethingAbove(file)) {
            directoriesTree.add("..");
        }
        for (int i = 0; i < files.length; i++) {
            if (FileUtilities.isIgnored(files[i]) == false) {
                if (files[i].isDirectory()) {
                    directoriesTree.add(files[i].getName() + File.separatorChar);
                } else {
                    filesTree.add(files[i].getName());
                }
            }
        }
        addArrayElementsToList(directoriesTree.toArray());
        addArrayElementsToList(filesTree.toArray());
    }
    
    public boolean isSomethingAbove(File file) {
        return file.getParentFile() != null;
    }
    
    /** Returns true if the directory is above a root, and this routine has filled the list. */
    public boolean handleRoot() {
        File[] roots = File.listRoots();
        if (roots == null || roots.length == 1) {
            return false;
        }
        
        boolean isRoot = false;
        TreeSet rootsTree = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        String filename = file.getName();
        for (int i = 0; i < roots.length; i++) {
            rootsTree.add(roots[i].getName());
            if (filename.equals(roots[i].getName())) {
                isRoot = true;
            }
        }
        
        if (isRoot) {
            addArrayElementsToList(rootsTree.toArray());
        }
        return isRoot;
    }
    
    public String getDirectorySizeDescription() {
        int itemCount = listModel.getSize();
        itemCount--; // ".." isn't interesting.
        String result = " - " + itemCount + " item";
        if (itemCount != 1) {
            result += "s";
        }
        return result;
    }
    
    public void addArrayElementsToList(Object[] array) {
        for (int i = 0; i < array.length; i++) {
            listModel.addElement(array[i].toString());
        }
    }
    
    public String nameWithoutExtension(String name) {
        int dotPosition = name.lastIndexOf('.');
        if (dotPosition == -1) { return name; }
        return name.substring(0, dotPosition);
    }
    
    public Collection getPopupMenuItems() {
        ArrayList items = new ArrayList();
        items.add(new FindFilesAction());
        items.add(new RefreshAction());
        //items.add(new UseAsGuideAction());
        return items;
    }
    
    public class RefreshAction extends AbstractAction {
        public RefreshAction() {
            super("Refresh");
        }
        public void actionPerformed(ActionEvent e) {
            refreshContents();
        }
    }
    
    public class UseAsGuideAction extends AbstractAction {
        public UseAsGuideAction() {
            super("Use as Guide");
        }
        public void actionPerformed(ActionEvent e) {
            String filename = (String) list.getSelectedValue();
            //getWorkspace().setGuideFile(getContext(), filename);
        }
    }
    
    /** Directory windows are never considered dirty because they're not worth preserving. */
    public boolean isDirty() {
        return false;
    }
    
    public String getContext() {
        return file.getPath();
    }
    
    public static class DirectoryListCellRenderer extends JLabel implements ListCellRenderer {
        private static final Icon FILE_ICON = UIManager.getIcon("FileView.fileIcon");
        private static final Icon DIRECTORY_ICON = UIManager.getIcon("FileView.directoryIcon");
        
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String text = value.toString();
            setText(text);
            setIcon((text.equals("..") || text.endsWith(File.separator)) ? DIRECTORY_ICON : FILE_ICON);
            Color background = isSelected ? list.getSelectionBackground() : list.getBackground();
            Color foreground = isSelected ? list.getSelectionForeground() : list.getForeground();
            setBackground(background);
            setForeground(foreground);
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            return this;
        }
    }
}
