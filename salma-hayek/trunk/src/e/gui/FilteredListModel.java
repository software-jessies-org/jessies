package e.gui;

import java.awt.event.*;
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
     * string). String comparison is done case-insensitively.
     */
    public void setFilter(final String filter) {
        String substring = filter.toLowerCase();
        validIndexes = new ArrayList();
        for (int i = 0; i < model.getSize(); ++i) {
            String entry = model.getElementAt(i).toString().toLowerCase();
            if (entry.indexOf(filter) != -1) {
                validIndexes.add(new Integer(i));
            }
        }
        // We can't use fireContentsChanged here because it doesn't imply
        // that the structure has changed, so the selection model won't be
        // updated. This implementation means you'll use the selection, but
        // it could be altered to report all the additions and removals.
        fireIntervalRemoved(this, 0, model.getSize());
        fireIntervalAdded(this, 0, validIndexes.size());
    }
    
    /**
     * Creates a new SearchField tied to this FilteredListModel such that
     * typing in the field will change the filter. Each invocation creates
     * a new instance.
     */
    public SearchField makeSearchField() {
        final SearchField searchField = new SearchField();
        searchField.setSendsNotificationForEachKeystroke(true);
        searchField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setFilter(searchField.getText());
            }
        });
        return searchField;
    }
    
    public Object getElementAt(int index) {
        int newIndex = ((Integer) validIndexes.get(index)).intValue();
        return model.getElementAt(newIndex);
    }
    
    public int getSize() {
        return validIndexes.size();
    }
}
