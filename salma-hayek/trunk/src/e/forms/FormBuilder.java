package e.forms;

import e.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * Creates and shows form dialogs. A form dialog is a simple collection of
 * input fields, as you'd see in most general dialogs, properties dialogs,
 * and preference dialogs. A field can be a complex component such as a text
 * area, table, or whatever.
 * 
 * There's automatic support for "as-you-type" operation applied to the form's
 * text fields. See FormPanel and FormDialog for more customization details.
 */
public class FormBuilder {
    private static final String[] DEFAULT_UI = { "<default-anonymous-panel>" };
    
    private FormPanel[] formPanels;
    private FormDialog formDialog;
    
    // FormDialog wants read access to this, but there's no need for it to be publicly available.
    String[] tabTitles;
    // FormDialog wants read/write access to this, but it shouldn't be publicly available.
    JComponent statusBar;
    // FormDialog wants read access to this, but there's no need for it to be publicly available.
    ActionListener typingTimeoutActionListener = NoOpAction.INSTANCE;
    
    /**
     * Gets ready to show a form with a given name as a child of the given Frame.
     * This form does not contain multiple tabs.
     */
    public FormBuilder(Frame parent, String title) {
        this(parent, title, DEFAULT_UI);
    }
    
    /**
     * Gets ready to show a form with a given name as a child of the given Frame.
     * This form contains as many tabs as you supply tab titles.
     * You should use getFormPanels to access the tabs' form panels; indexes correspond to the supplied tab titles.
     */
    public FormBuilder(Frame parent, String title, String[] tabTitles) {
        this.tabTitles = tabTitles;
        this.formPanels = makeFormPanels();
        this.formDialog = new FormDialog(this, parent, title);
    }
    
    private FormPanel[] makeFormPanels() {
        FormPanel[] result = new FormPanel[tabTitles.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = new FormPanel();
        }
        return result;
    }
    
    /**
     * Returns the underlying FormDialog; only needed for advanced
     * customization.
     */
    public FormDialog getFormDialog() {
        return formDialog;
    }
    
    /**
     * Returns the underlying FormPanels; use this to add content to your
     * multi-tab form. Keep hold of the components you give to the FormPanel so that
     * you can read values back from them when the dialog has been accepted.
     */
    public List<FormPanel> getFormPanels() {
        return new ArrayList<FormPanel>(Arrays.asList(formPanels));
    }
    
    /**
     * Returns the underlying FormPanel; use this to add content to your
     * form. Keep hold of the components you give to the FormPanel so that
     * you can read values back from them when the dialog has been accepted.
     */
    public FormPanel getFormPanel() {
        if (formPanels.length != 1) {
            throw new IllegalArgumentException("this form has " + formPanels.length + " panels");
        }
        return formPanels[0];
    }
    
    public void setTypingTimeoutActionListener(ActionListener listener) {
        this.typingTimeoutActionListener = listener;
    }
    
    /**
     * Sets the status bar for this form.
     * Using this in preference to adding a row to the panel gives us a chance to lay out the status bar in the most appropriate manner.
     */
    public void setStatusBar(JComponent component) {
        this.statusBar = component;
    }
    
    /**
     * Shows the dialog with the given verb on the "OK" button, and returns
     * true if that button (rather than "Cancel") was chosen.
     */
    public boolean show(String actionLabel) {
        return formDialog.show(actionLabel);
    }
    
    /**
     * Shows the dialog non-modally, with just a "Close" button instead of
     * an action button and a "Cancel" button.
     */
    public void showNonModal() {
        formDialog.showNonModal();
    }
}
