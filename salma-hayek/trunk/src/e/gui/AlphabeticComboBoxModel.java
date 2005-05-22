package e.gui;

import java.util.*;

public class AlphabeticComboBoxModel extends EHistoryComboBoxModel {
    public AlphabeticComboBoxModel() {
        this.model = new TreeSet<String>();
    }
    
    public Object getElementAt(int index) {
        Iterator iterator = model.iterator();
        Object result = iterator.next();
        for (int i = 0; i < index; i++) {
            result = iterator.next();
        }
        return result;
    }
}
