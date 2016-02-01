package e.gui;

import java.util.*;

public class ChronologicalComboBoxModel extends EHistoryComboBoxModel {
    public ChronologicalComboBoxModel() {
        this.model = new Vector();
    }
    
    public void addElement(Object element) {
        if (element != null && (((String) element).length() > 0)) {
            model.remove(element);
            model.add(element);
            fireChangeNotification();
        }
    }
    
    public Object getElementAt(int index) {
        if (index < 0) {
            return "<invalid item>";
        }
        Object result = ((Vector) model).get(index);
        return result;
    }
}
