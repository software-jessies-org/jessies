package e.gui;

import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public abstract class EHistoryComboBoxModel implements MutableComboBoxModel {
    private ArrayList<ListDataListener> listeners = new ArrayList<ListDataListener>();
    private Object selectedItem;
    protected Collection<String> model;
    
    public void addElement(Object element) {
        String string = (String) element;
        if (string != null && string.length() > 0) {
            model.add(string);
            fireChangeNotification();
        }
    }
    
    public void addListDataListener(ListDataListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }
    
    public void fireChangeNotification() {
        synchronized (listeners) {
            ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize());
            for (int i = 0; i < listeners.size(); i++) {
                ListDataListener l = (ListDataListener) listeners.get(i);
                l.contentsChanged(e);
            }
        }
    }
    
    public Object getSelectedItem() {
        return selectedItem;
    }
    
    public int getSize() {
        return model.size();
    }
    
    public void insertElementAt(Object element, int index) {
        model.add((String) element);
        fireChangeNotification();
    }
    
    public void removeElement(Object element) {
        //Log.warn("removeElement(" + element + ")");
        model.remove(element);
        fireChangeNotification();
    }
    
    public void removeElementAt(int index) {
        //Log.warn("removeElementAt(" + index + ")");
        removeElement(getElementAt(index));
    }
    
    public void removeListDataListener(ListDataListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }
    
    public void setSelectedItem(Object item) {
        this.selectedItem = item;
        fireChangeNotification();
    }
    
    public String toString() {
        return model != null ? model.toString() : "(null)";
    }
}
