package e.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import e.edit.*;

public class EHistoryTextField extends JComboBox {
    private EHistoryComboBoxModel history;
    
    public EHistoryTextField() {
        this(new ChronologicalComboBoxModel());
    }
    
    public EHistoryTextField(EHistoryComboBoxModel history) {
        setEditable(true);
        setEditor(new EHistoryTextFieldEditor(this));
        setModel(history);
    }
    
    public void setModel(ComboBoxModel model) {
        if (model instanceof EHistoryComboBoxModel) {
            this.history = (EHistoryComboBoxModel) model;
        }
        super.setModel(model);
    }
    
    public void rememberCurrentItem() {
        history.addElement(getCurrentText());
    }
    
    /**
     * Returns the text currently in the combo box editor. Not until the editor loses focus
     * and the edit is considered over will the result of getText be changed by the user's
     * typing. If you want to track typing, you need to invoke this method instead.
     */
    public String getCurrentText() {
        return (String) getEditor().getItem();
    }
    
    public String getText() {
        return (String) getSelectedItem();
    }
    
    public void setText(String item) {
        history.addElement(item);
        setSelectedItem(item);
    }
    
    public void selectAll() {
        getEditor().selectAll();
    }
    
    /** Invoked whenever the text in the combo box editor changes. See also getCurrentText. */
    public void textChanged() {
        /* Do nothing. */
    }
    
    /** Ensure that we select all when we get the focus, just like Windows. */
    public void requestFocus() {
        super.requestFocus();
        selectAll();
    }
}
