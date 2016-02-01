package e.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class EHistoryTextFieldEditor implements ComboBoxEditor {
    private ETextField editor;
    
    public EHistoryTextFieldEditor(final EHistoryTextField listener) {
        this.editor = new ETextField() {
            public void textChanged() {
                listener.textChanged();
            }
        };
        editor.setBorder(null);
    }
    
    public void addActionListener(ActionListener l) {
        editor.addActionListener(l);
    }
    
    public Component getEditorComponent() {
        return editor;
    }
    
    public Object getItem() {
        return editor.getText();
    }
    
    public void removeActionListener(ActionListener l) {
        editor.removeActionListener(l);
    }
    
    public void selectAll() {
        editor.selectAll();
    }
    
    public void setItem(Object item) {
        editor.setText(item != null ? item.toString() : "");
    }
}
