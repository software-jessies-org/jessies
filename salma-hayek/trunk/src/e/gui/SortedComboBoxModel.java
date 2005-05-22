package e.gui;

import javax.swing.*;

/**
 * A combo box model like DefaultComboBoxModel that keeps the items
 * sorted. The items must implement the Comparable interface.
 */
public class SortedComboBoxModel extends DefaultComboBoxModel {
    /**
     * Adds the given element in order. The element must
     * implement the Comparable interface.
     */
    public void addElement(Object element) {
        String string = (String) element;
        int i = 0;
        for (i = 0; i < getSize(); ++i) {
            String other = (String) getElementAt(i);
            if (other.compareTo(string) > 0) {
                break;
            }
        }
        super.insertElementAt(element, i);
    }
    
    /**
     * Ignores requests to insert an element at a given index, and
     * inserts the element in the correct sort order instead.
     */
    public void insertElementAt(Object element, int index) {
        addElement(element);
    }
}
