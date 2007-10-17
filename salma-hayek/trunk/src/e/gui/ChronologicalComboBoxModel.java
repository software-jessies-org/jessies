package e.gui;

import java.util.*;

public class ChronologicalComboBoxModel extends EHistoryComboBoxModel {
    public ChronologicalComboBoxModel() {
        this.model = new ArrayList<String>();
    }
    
    public void addElement(Object element) {
        String string = (String) element;
        if (string != null && string.length() > 0) {
            model.remove(string);
            model.add(string);
            fireChangeNotification();
        }
    }
    
    public Object getElementAt(int index) {
        if (index < 0) {
            return "<invalid item>";
        }
        Object result = ((ArrayList<?>) model).get(index);
        return result;
    }
}
