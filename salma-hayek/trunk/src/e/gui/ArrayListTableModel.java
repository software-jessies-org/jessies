package e.gui;

import java.util.*;
import javax.swing.table.*;

/**
 * Allows an ArrayList to be used as a single-column TableModel.
 * Don't modify the ArrayList once you're using it as a TableModel; replace it
 * with a new ArrayList using setArrayList.
 */
public class ArrayListTableModel extends AbstractTableModel {
    private ArrayList arrayList;
    public ArrayListTableModel() {
        setArrayList(new ArrayList());
    }
    public ArrayListTableModel(ArrayList arrayList) {
        setArrayList(arrayList);
    }
    public void setArrayList(ArrayList newArrayList) {
        this.arrayList = newArrayList;
        fireTableDataChanged();
    }
    public int getColumnCount() {
        return 1;
    }
    public int getRowCount() {
        return arrayList.size();
    }
    public String getColumnName(int col) {
        return "?";
    }
    public Object getValueAt(int row, int col) {
        return arrayList.get(row);
    }
}
