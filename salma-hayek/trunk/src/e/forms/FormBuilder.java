package e.forms;

import java.awt.*;
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
    FormPanel formPanel;
    FormDialog formDialog;
    JComponent statusBar;
    
    /**
     * Gets ready to show a form with a given name as a child of the
     * given Frame.
     */
    public FormBuilder(Frame parent, String title) {
        this.formPanel = new FormPanel();
        this.formDialog = new FormDialog(this, parent, title);
    }
    
    /**
     * Returns the underlying FormDialog; only needed for advanced
     * customization.
     */
    public FormDialog getFormDialog() {
        return formDialog;
    }
    
    /**
     * Returns the underlying FormPanel; use this to add content to your
     * form. Keep hold of the components you give to the FormPanel so that
     * you can read values back from them when the dialog has been accepted.
     */
    public FormPanel getFormPanel() {
        return formPanel;
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
