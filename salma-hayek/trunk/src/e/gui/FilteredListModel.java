package e.gui;

import java.util.*;
import javax.swing.*;

/**
 * Wraps a ListModel and provides substring-based filtering.
 */
public class FilteredListModel extends AbstractListModel {
    private ListModel model;
    private ArrayList validIndexes;
    
    public FilteredListModel(final ListModel model) {
        this.model = model;
        setFilter("");
    }
    
    /**
     * Sets a new filter substring. Items in the underlying model that
     * don't contain the given substring will disappear. Set to "" if you
     * want to cancel the filtering (since every string contains the empty
     * string).
     */
    public void setFilter(String filter) {
        validIndexes = new ArrayList();
        for (int i = 0; i < model.getSize(); ++i) {
            String entry = model.getElementAt(i).toString();
            if (entry.indexOf(filter) != -1) {
                validIndexes.add(new Integer(i));
            }
        }
        fireIntervalRemoved(this, 0, model.getSize());
        fireIntervalAdded(this, 0, validIndexes.size());
    }
    
    public Object getElementAt(int index) {
        int newIndex = ((Integer) validIndexes.get(index)).intValue();
        return model.getElementAt(newIndex);
    }
    
    public int getSize() {
        return validIndexes.size();
    }
}
